package com.sequoiacm.diagnose.command;

import com.sequoiacm.diagnose.collect.LocalLogCollector;
import com.sequoiacm.diagnose.collect.LogCollector;
import com.sequoiacm.diagnose.collect.RemoteLogCollector;
import com.sequoiacm.diagnose.common.LogCollectResult;
import com.sequoiacm.diagnose.config.LogCollectConfig;
import com.sequoiacm.diagnose.config.SshCommonConfig;
import com.sequoiacm.diagnose.execption.LogCollectException;
import com.sequoiacm.diagnose.ssh.Ssh;
import com.sequoiacm.diagnose.utils.HostAddressUtils;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

@Command
public class ScmLogCollect extends SubCommand {

    public static final String NAME = "log-collect";
    private static final String HOSTS = "hosts";
    private static final String CONF = "conf";
    private static final String INSTALL_PATH = "install-path";
    private static final String OUTPUT_PATH = "output-path";
    private static final String SHORT_OUTPUT_PATH = "o";
    private static final String SERVICES = "services";
    private static final String MAX_LOG_COUNT = "max-log-count";
    private static final String THREAD = "thread-size";
    private static final String NEED_ZIP = "need-zip";
    private static final String LOGBACK_PATH = "logback-path";
    private static final Logger logger = LoggerFactory.getLogger(ScmLogCollect.class);
    private static ArrayList<LogCollector> logCollectors = new ArrayList<>();
    private static boolean hasLocalCollect = false;

    public static String currentCollectPath = null;

    private static Integer collectFailCount = -1;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "execute log collect.";
    }

    public void run(String[] args)
            throws ScmToolsException, IOException, ParseException, InterruptedException {
        Options ops = addLogParam();
        CommandLine commandLine = new DefaultParser().parse(ops, args, false);

        String logbackPath = commandLine.getOptionValue(LOGBACK_PATH);
        changeLogbackFile(logbackPath);

        if (commandLine.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        System.out.println("[INFO ] start analyze parameter");

        // analyzing conf
        HashMap<String, List<String>> confMap = null;
        if (commandLine.hasOption(CONF)) {
            String confPath = commandLine.getOptionValue(CONF);
            confMap = analyzeConfFile(confPath);
            if (confMap != null && confMap.get("collectConfig") != null) {
                analyzeOthersByConf(confMap.get("collectConfig"));
            }
        }
        // analyzing hosts
        if (commandLine.hasOption(HOSTS)) {
            String hosts = commandLine.getOptionValue(HOSTS);
            analyzeHostInfoByHost(hosts);
        }
        // analyzing hosts in conf
        else {
            if (confMap != null) {
                List<String> hostList = confMap.get(HOSTS);
                if (hostList != null) {
                    analyzeHostInfoByConf(hostList);
                }
            }
        }

        if (commandLine.hasOption(INSTALL_PATH)) {
            String installPath = commandLine.getOptionValue(INSTALL_PATH);
            LogCollectConfig.setInstallPath(installPath);
        }

        analyzeOutputPath(commandLine);
        analyzeServices(commandLine);

        if (commandLine.hasOption(MAX_LOG_COUNT)) {
            int maxLogCount = Integer.parseInt(commandLine.getOptionValue(MAX_LOG_COUNT));
            if (maxLogCount < -1) {
                throw new IllegalArgumentException("Invalid arg " + MAX_LOG_COUNT + "="
                        + maxLogCount + ",it must greater than -1");
            }
            LogCollectConfig.setMaxLogCount(maxLogCount);
        }

        if (commandLine.hasOption(THREAD)) {
            int thread = Integer.parseInt(commandLine.getOptionValue(THREAD));
            if (thread <= 0) {
                throw new IllegalArgumentException(
                        "Invalid arg " + THREAD + "=" + thread + ",it must be a positive integer");
            }
            LogCollectConfig.setThreadSize(thread);
        }

        if (commandLine.hasOption(NEED_ZIP)) {
            String value = commandLine.getOptionValue(NEED_ZIP);
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                LogCollectConfig.setNeedZipCopy(Boolean.parseBoolean(value));
            }
            else {
                throw new IllegalArgumentException(
                        "Invalid arg " + NEED_ZIP + "=" + value + ",it must be boolean type");
            }
        }

        System.out.println("[INFO ] analyze finished");

        logger.info("analyze finished");

        // if analyzing hosts is empty
        if (logCollectors.size() == 0) {
            logCollectors.add(new LocalLogCollector());
        }

        String dateTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        currentCollectPath = LogCollectConfig.getResultDir() + "_" + dateTime;
        File file = new File(
                LogCollectConfig.getOutputPath() + File.separator + currentCollectPath);
        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
        }
        FileUtils.forceMkdir(file);

        ArrayList<LogCollectResult> collectResult = logCollectStart();
        printResult(collectResult);
    }

    private ArrayList<LogCollectResult> logCollectStart() throws InterruptedException {
        logger.info("log collect start");
        ExecutorService threadPool = null;
        ArrayList<LogCollectResult> collectResults = new ArrayList<>();
        ArrayList<Future<LogCollectResult>> futureResult = new ArrayList<>();
        try {
            threadPool = Executors.newFixedThreadPool(LogCollectConfig.getThreadSize());
            for (LogCollector collector : logCollectors) {
                Future<LogCollectResult> result = threadPool.submit(collector);
                futureResult.add(result);
            }
            collectFailCount = 0;
            for (Future<LogCollectResult> future : futureResult) {
                LogCollectResult collectResult = future.get();
                if (collectResult.getCode() != 0) {
                    collectFailCount++;
                    ScmToolsException e = collectResult.getException();
                    logger.error("Execution failed, cause by:{}", e.getMessage(), e);
                }
                collectResults.add(collectResult);
            }
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
        return collectResults;
    }

    private void printResult(ArrayList<LogCollectResult> collectResult) {
        if (collectFailCount == 0) {
            System.out.println("[INFO ] Scm log collect successfully："
                    + LogCollectConfig.getOutputPath() + File.separator + currentCollectPath);
        }
        else {
            System.err.println("[ERROR] Scm log collect failed :" + LogCollectConfig.getOutputPath()
                    + File.separator + currentCollectPath);
            for (LogCollectResult result : collectResult) {
                if (result.getCode() == 0) {
                    System.out.println("[INFO ] " + result.getMsg());
                }
                else {
                    System.err.println("[ERROR] " + result.getMsg());
                }
            }
            System.err.println("[ERROR] Execution detail " + System.getProperty("binPath")
                    + "/../log/scm-diagnose.log");
        }
    }

    private void analyzeServices(CommandLine commandLine) {
        if (commandLine.hasOption(SERVICES)) {
            ArrayList<String> serviceList = new ArrayList<>();
            String services = commandLine.getOptionValue(SERVICES);
            for (String service : services.split(",")) {
                if (StringUtils.isEmpty(LogCollectConfig.getServerMap().get(service))) {
                    throw new IllegalArgumentException(
                            "services is invalid arg " + service + " is not exist in scm services");
                }
                serviceList.add(service);
            }
            LogCollectConfig.setServiceList(serviceList);
        }
    }

    private void analyzeOutputPath(CommandLine commandLine) throws ScmToolsException {
        String outputPath = "";
        if (commandLine.hasOption(SHORT_OUTPUT_PATH)) {
            LogCollectConfig.setOutputPath(commandLine.getOptionValue(SHORT_OUTPUT_PATH));
        }
        else {
            if (StringUtils.isEmpty(LogCollectConfig.getOutputPath())) {
                outputPath = ScmCommon.getUserWorkingDir();
                LogCollectConfig.setOutputPath(outputPath);
            }
        }
    }

    public void analyzeOthersByConf(List<String> otherConf) {
        for (String conf : otherConf) {
            String[] splitConf = conf.split("=");
            if (splitConf.length != 2) {
                throw new IllegalArgumentException("collectConfig illegal configuration, " + conf);
            }
            else {
                assignmentOthersConf(splitConf[0], splitConf[1]);
            }
        }
    }

    private void assignmentOthersConf(String key, String value) {
        switch (key) {
            case SERVICES:
                ArrayList<String> serviceList = new ArrayList<>();
                for (String service : value.split(",")) {
                    if (LogCollectConfig.getServerMap().get(service) != null) {
                        serviceList.add(service);
                    }
                    else {
                        throw new IllegalArgumentException(
                                "collectConfig illegal configuration," + SERVICES + "=" + value
                                        + "," + service + " is not exist in scm services");
                    }
                }
                LogCollectConfig.setServiceList(serviceList);
                break;
            case MAX_LOG_COUNT:
                int logCount = LogCollectConfig.getMaxLogCount();
                try {
                    logCount = Integer.parseInt(value);
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "collectConfig illegal configuration, " + MAX_LOG_COUNT + "=" + value
                                    + ", it must be a number");
                }
                if (logCount < -1) {
                    throw new IllegalArgumentException(
                            "collectConfig illegal configuration, " + MAX_LOG_COUNT + "=" + value
                                    + ", it must >= -1");
                }
                LogCollectConfig.setMaxLogCount(logCount);
                break;
            case OUTPUT_PATH:
                LogCollectConfig.setOutputPath(value);
                break;
            case INSTALL_PATH:
                LogCollectConfig.setInstallPath(value);
                break;
            case THREAD:
                int threadSize = LogCollectConfig.getThreadSize();
                try {
                    threadSize = Integer.parseInt(value);
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("collectConfig illegal configuration, "
                            + THREAD + "=" + value + ", it must be a number");
                }
                if (threadSize < 1) {
                    throw new IllegalArgumentException(
                            "collectConfig illegal configuration, " + THREAD + "=" + value
                                    + ", it must > 0");
                }
                LogCollectConfig.setThreadSize(threadSize);
                break;
            case NEED_ZIP:
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    boolean needZip = Boolean.parseBoolean(value);
                    LogCollectConfig.setNeedZipCopy(needZip);
                    break;
                }
                else {
                    throw new IllegalArgumentException("collectConfig illegal configuration, "
                            + NEED_ZIP + "=" + value + ", it must be boolean type");
                }
            case "private-key-path":
                SshCommonConfig.setPrivateKeyPath(value);
                break;
            case "connect-timeout":
                Integer connectTimeout = SshCommonConfig.getConnectTimeout();
                try {
                    connectTimeout = Integer.parseInt(value);
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "collectConfig illegal configuration, connect-timeout=" + value
                                    + ", it must be a number and < " + Integer.MAX_VALUE);
                }
                if (connectTimeout < 1) {
                    throw new IllegalArgumentException(
                            "collectConfig illegal configuration, connect-timeout=" + value
                                    + ", it must > 0");
                }
                SshCommonConfig.setConnectTimeout(connectTimeout);
                break;
            default:
                logger.warn("collectConfig illegal configuration, " + key + "=" + value);
                break;
        }
    }

    protected void printHelp() throws ParseException {
        Options ops = addLogParam();
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", ops);
    }

    public void analyzeHostInfoByConf(List<String> hostList)
            throws ScmToolsException, UnknownHostException {
        for (String hostLine : hostList) {
            String[] hostInfoString = hostLine.split(",");
            if (hostInfoString.length < 4) {
                throw new IllegalArgumentException("host illegal configuration, " + hostLine);
            }
            String hostName = hostInfoString[0].trim();
            int port = Integer.parseInt(hostInfoString[1].trim());
            String user = hostInfoString[2].trim();
            String passwordPath = hostInfoString[3].trim();
            String password = passwordPath;
            File pwdFile = new File(passwordPath);

            if (!pwdFile.isFile()) {
                pwdFile = new File(ScmCommon.getUserWorkingDir() + File.separator + passwordPath);
            }
            if (!pwdFile.isDirectory() && pwdFile.isFile()) {
                password = ScmFilePasswordParser.parserFile(passwordPath).getPassword();
            }
            boolean isRepeat = isRepeatCollector(hostName);
            if (!isRepeat) {
                RemoteLogCollector remoteLogCollector = new RemoteLogCollector();
                remoteLogCollector.setSsh(new Ssh(hostName, port, user, password));
                logCollectors.add(remoteLogCollector);
            }
            else {
                logger.warn("remote host " + hostName
                        + " log collect is repeat,this collect was ignored");
            }
        }
    }

    private boolean isRepeatCollector(String hostName) throws UnknownHostException {
        for (LogCollector logCollector : logCollectors) {
            if (logCollector instanceof RemoteLogCollector) {
                RemoteLogCollector remoteLogCollector = (RemoteLogCollector) logCollector;
                if (HostAddressUtils.getIpByHostName(hostName).equals(
                        HostAddressUtils.getIpByHostName(remoteLogCollector.getSsh().getHost()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void analyzeHostInfoByHost(String hosts)
            throws ScmToolsException, UnknownHostException {
        logger.info("parse hostInfo : --" + HOSTS + " " + hosts);
        for (String hostAndPort : hosts.split(",")) {
            String host = "";
            Integer port = 22;
            if (hostAndPort.contains(":")) {
                String[] hostSplit = hostAndPort.split(":");
                if (hostSplit.length != 2) {
                    throw new IllegalArgumentException(
                            "Failed to pass the " + HOSTS + "," + HOSTS + "=" + hosts);
                }
                host = hostSplit[0];
                port = Integer.parseInt(hostSplit[1]);
            }
            else {
                host = hostAndPort;
            }
            String localIpAddress = "local";
            String localHostName = "local";
            try {
                localIpAddress = HostAddressUtils.getLocalHostAddress();
                localHostName = HostAddressUtils.getLocalHostName();
            }
            catch (UnknownHostException e) {
                throw new ScmToolsException("get local ip or hostName failed",
                        LogCollectException.SYSTEM_ERROR, e);
            }

            // add local host to collect logFile
            if (localIpAddress.equals(host) || localHostName.equals(host)) {
                if (hasLocalCollect) {
                    logger.warn("local host log collect is repeat,this collect was ignored");
                }
                else {
                    LogCollector logCollector = new LocalLogCollector();
                    logCollectors.add(logCollector);
                    hasLocalCollect = true;
                }
            }
            // add ssh no password to collect logFile
            else {
                boolean isRepeat = isRepeatCollector(host);
                if (!isRepeat) {
                    RemoteLogCollector remoteLogCollector = new RemoteLogCollector();
                    remoteLogCollector.setSsh(new Ssh(host, port, null, null));
                    logCollectors.add(remoteLogCollector);
                }
                else {
                    logger.warn("remote host " + host
                            + " log collect is repeat,this collect was ignored");
                }
            }
        }
    }

    public HashMap<String, List<String>> analyzeConfFile(String confPath)
            throws IOException, ScmToolsException {
        File file = new File(confPath);
        if (!file.isAbsolute()) {
            file = new File(ScmCommon.getUserWorkingDir() + File.separator + confPath);
        }
        if (file.isFile()) {
            logger.info("parse confFile:" + file);
            BufferedReader bfReader = null;
            HashMap<String, List<String>> confMap = null;
            try {
                bfReader = new BufferedReader(new FileReader(file));
                confMap = parse(bfReader);
            }
            catch (IOException e) {
                throw new ScmToolsException(e.getMessage(), LogCollectException.INVALID_ARG, e);
            }
            finally {
                if (bfReader != null) {
                    bfReader.close();
                }
            }
            return confMap;
        }
        else {
            throw new IllegalArgumentException("failed to parse conf file,it require filePath");
        }
    }

    private HashMap<String, List<String>> parse(BufferedReader bfReader) throws IOException {
        String currentSeaction = null;
        HashMap<String, List<String>> seactionMap = new HashMap<>();
        while (true) {
            String line = bfReader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            if (line.contains("HostName")) {
                continue;
            }

            if (Pattern.matches("^\\[[A-Za-z0-9]+\\]$", line)) {
                logger.debug("parse seaction:{}", line);
                currentSeaction = line.substring(1, line.length() - 1);
                if (seactionMap.containsKey(currentSeaction)) {
                    throw new IOException("duplicate search:" + line);
                }
                seactionMap.put(currentSeaction, new ArrayList<String>());
                continue;
            }

            if (currentSeaction == null) {
                throw new IOException("failed to parse conf file, missing search line:" + line);
            }
            List<String> secLines = seactionMap.get(currentSeaction);
            secLines.add(line);
        }
        return seactionMap;
    }

    protected Options addLogParam() throws ParseException {
        Options ops = new Options();
        ops.addOption(Option.builder("h").longOpt("help").hasArg(false).required(false).build());
        ops.addOption(Option.builder(null).longOpt(HOSTS).desc("scm collect log machines")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(CONF).desc("scm collect log conf path")
                .optionalArg(true)
                .hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(INSTALL_PATH).desc("scm install path")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(SHORT_OUTPUT_PATH).longOpt(OUTPUT_PATH)
                .desc("scm log collect outputPath").optionalArg(true).hasArg(true).required(false)
                .build());
        ops.addOption(Option.builder(null).longOpt(SERVICES).desc("scm services list")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(MAX_LOG_COUNT)
                .desc("scm log collect logFile max number")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(THREAD).desc("scm log collect thread size")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(NEED_ZIP).desc("scm collect log files need zip")
                .optionalArg(true).hasArg(true)
                .required(false).build());
        ops.addOption(Option.builder(null).longOpt(LOGBACK_PATH).desc("logback path")
                .optionalArg(true).hasArg(true).required(false).build());
        return ops;
    }

    private static void changeLogbackFile(String logbackPath)
            throws ScmToolsException, IOException {
        InputStream is = null;
        try {
            if (StringUtils.isEmpty(logbackPath)) {
                is = new ClassPathResource("diagnoseLogback.xml").getInputStream();
            }
            else {
                is = new FileInputStream(logbackPath);
            }
            ScmHelper.configToolsLog(is);
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException(
                    "logbackPath is " + logbackPath + "The logback xml file does not exist",
                    LogCollectException.FILE_NOT_FIND);
        }
        finally {
            ScmCommon.closeResource(is);
        }
    }
}
