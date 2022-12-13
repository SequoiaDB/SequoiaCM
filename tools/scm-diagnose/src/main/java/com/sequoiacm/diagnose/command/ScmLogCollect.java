package com.sequoiacm.diagnose.command;

import com.sequoiacm.diagnose.collect.LocalLogCollector;
import com.sequoiacm.diagnose.collect.LogCollector;
import com.sequoiacm.diagnose.collect.RemoteLogCollector;
import com.sequoiacm.diagnose.common.CollectResult;
import com.sequoiacm.diagnose.config.CollectConfig;
import com.sequoiacm.diagnose.ssh.Ssh;
import com.sequoiacm.diagnose.utils.HostAddressUtils;
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

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Command
public class ScmLogCollect extends SubCommand {

    private static final String NAME = "log-collect";
    private static final String HOSTS = "hosts";
    private static final String CONF = "conf";
    private static final String INSTALL_PATH = "install-path";
    private static final String OUTPUT_PATH = "output-path";
    private static final String SHORT_OUTPUT_PATH = "o";
    private static final String SERVICES = "services";
    private static final String MAX_LOG_COUNT = "max-log-count";
    private static final String THREAD = "thread-size";
    private static final String NEED_ZIP = "need-zip";
    private static final String RESULT_DIR = "scm-collect-logs";

    private static final Logger logger = LoggerFactory.getLogger(ScmLogCollect.class);
    private static ArrayList<LogCollector> logCollectors = new ArrayList<>();

    public static String currentCollectPath = null;

    public static boolean hasLocalCollect = false;

    private static Integer collectFailCount = -1;

    private static List<Ssh> sshList = new ArrayList<>();

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
        Options ops = addParam();
        CommandLine commandLine = new DefaultParser().parse(ops, args, false);

        if (commandLine.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        System.out.println("[INFO ] start analyze parameter");

        CollectConfig.setResultDir(RESULT_DIR);
        CollectConfig.init(commandLine, sshList);

        System.out.println("[INFO ] analyze parameter finished");
        logger.info("analyze parameter finished");

        String dateTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        currentCollectPath = CollectConfig.getResultDir() + "_" + dateTime;
        File file = new File(
                CollectConfig.getOutputPath() + File.separator + currentCollectPath);
        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
        }
        FileUtils.forceMkdir(file);

        List<CollectResult> collectResult = logCollectStart();
        printResult(collectResult);
    }

    private List<CollectResult> logCollectStart()
            throws InterruptedException, UnknownHostException, ScmToolsException {

        for (Ssh ssh : sshList) {
            if (HostAddressUtils.getLocalHostAddress().equals(ssh.getHost())
                    || HostAddressUtils.getLocalHostName().equals(ssh.getHost())) {
                logCollectors.add(new LocalLogCollector());
            }
            else {
                RemoteLogCollector remoteLogCollector = new RemoteLogCollector();
                remoteLogCollector.setSsh(ssh);
                logCollectors.add(remoteLogCollector);
            }
        }

        // if analyzing hosts is empty
        if (logCollectors.size() == 0 || hasLocalCollect) {
            logCollectors.add(new LocalLogCollector());
        }

        ExecutorService threadPool = null;
        List<CollectResult> collectResults = new ArrayList<>();
        List<Future<CollectResult>> futureResult = new ArrayList<>();
        logger.info("log collect start");
        try {
            threadPool = Executors.newFixedThreadPool(CollectConfig.getThreadSize());
            for (LogCollector collector : logCollectors) {
                Future<CollectResult> result = threadPool.submit(collector);
                futureResult.add(result);
            }
            collectFailCount = 0;
            for (Future<CollectResult> future : futureResult) {
                CollectResult collectResult = future.get();
                if (collectResult.getCode() != 0) {
                    collectFailCount++;
                    Exception e = collectResult.getException();
                    logger.error("Execution failed,cause by:{}", e.getMessage(), e);
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

    private void printResult(List<CollectResult> collectResult) {
        if (collectFailCount == 0) {
            System.out.println("[INFO ] scm log collect successfully："
                    + CollectConfig.getOutputPath() + File.separator + currentCollectPath);
        }
        else {
            for (CollectResult result : collectResult) {
                if (result.getCode() == 0) {
                    System.out.println("[INFO ] " + result.getMsg());
                }
                else {
                    System.err.println("[ERROR] " + result.getMsg());
                }
            }
            System.err.println("[ERROR] scm log collect failed: collect result in "
                    + CollectConfig.getOutputPath() + File.separator + currentCollectPath);
            System.err.println(
                    "[ERROR] Execution detail " + ScmHelper.getPwd() + "/log/scm-diagnose.log");
        }
    }

    protected void printHelp() throws ParseException {
        Options ops = addParam();
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", ops);
    }

    protected Options addParam() throws ParseException {
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
        return ops;
    }
}
