package com.sequoiacm.diagnose.utils;

import com.sequoiacm.diagnose.command.ScmLogCollect;
import com.sequoiacm.diagnose.config.CollectConfig;
import com.sequoiacm.diagnose.execption.CollectException;
import com.sequoiacm.diagnose.ssh.Ssh;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AnalyzeConfUtils {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeConfUtils.class);

    public static void analyzeHostInfoByConf(List<Ssh> sshList, List<String> hostList)
            throws UnknownHostException, ScmToolsException {
        for (String hostLine : hostList) {
            String[] hostInfoString = hostLine.split(",");
            if (hostInfoString.length < 4) {
                throw new IllegalArgumentException("host illegal configuration," + hostLine);
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
            Ssh ssh = getSshByHost(sshList, hostName);
            if (ssh == null) {
                try {
                    sshList.add(new Ssh(hostName, port, user, password));
                }
                catch (ScmToolsException e) {
                    System.out.println("[ERROR] " + e.getMessage());
                    logger.error(e.getMessage(), e);
                    throw e;
                }
            }
            else {
                logger.warn(
                        "host " + hostName + " cluster collect is repeat,this collect was ignored");
            }
        }
    }

    public static void analyzeHostInfoByHost(List<Ssh> sshList, String hosts)
            throws UnknownHostException, ScmToolsException {
        logger.info("parse hostInfo : --hosts " + hosts);
        for (String hostAndPort : hosts.split(",")) {
            String host = "";
            Integer port = 22;
            if (hostAndPort.contains(":")) {
                String[] hostSplit = hostAndPort.split(":");
                if (hostSplit.length != 2) {
                    throw new IllegalArgumentException("Failed to pass the hosts,hosts=" + hosts);
                }
                host = hostSplit[0];
                port = Integer.parseInt(hostSplit[1]);
            }
            else {
                host = hostAndPort;
            }
            String localIpAddress;
            String localHostName;
            try {
                localIpAddress = HostAddressUtils.getLocalHostAddress();
                localHostName = HostAddressUtils.getLocalHostName();
            }
            catch (UnknownHostException e) {
                throw new ScmToolsException("local get ip or hostName failed",
                        CollectException.SYSTEM_ERROR, e);
            }

            Ssh ssh = getSshByHost(sshList, host);

            if (ssh == null) {
                // local host
                if (localIpAddress.equals(host) || localHostName.equals(host)) {
                    ScmLogCollect.hasLocalCollect = true;
                }
                // add ssh no password
                else {
                    try {
                        sshList.add(new Ssh(host, port, null, null));
                    }
                    catch (ScmToolsException e) {
                        System.out.println("[ERROR] " + e.getMessage());
                        logger.error(e.getMessage(), e);
                        throw e;
                    }
                }
            }
            else {
                logger.warn("host " + host + " cluster collect is repeat,this collect was ignored");
            }
        }
    }

    private static Ssh getSshByHost(List<Ssh> sshList, String host)
            throws UnknownHostException {
        for (Ssh ssh : sshList) {
            if (HostAddressUtils.getIpByHostName(host)
                    .equals(HostAddressUtils.getIpByHostName(ssh.getHost()))) {
                return ssh;
            }
        }
        return null;
    }

    public static Map<String, List<String>> analyzeConfFile(String confPath)
            throws IOException, ScmToolsException {
        File file = new File(confPath);
        if (!file.isAbsolute()) {
            file = new File(ScmCommon.getUserWorkingDir() + File.separator + confPath);
        }
        if (file.isFile()) {
            logger.info("parse confFile:" + file);
            BufferedReader bfReader = null;
            Map<String, List<String>> confMap = null;
            try {
                bfReader = new BufferedReader(new FileReader(file));
                confMap = parse(bfReader);
            }
            catch (IOException e) {
                throw new ScmToolsException(e.getMessage(), CollectException.INVALID_ARG, e);
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

    private static Map<String, List<String>> parse(BufferedReader bfReader) throws IOException {
        String currentSeaction = null;
        Map<String, List<String>> seactionMap = new HashMap<>();
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

    public static void analyzeCollectConfig(List<String> otherConf) {
        for (String conf : otherConf) {
            String[] splitConf = conf.split("=");
            if (splitConf.length != 2) {
                throw new IllegalArgumentException("collectConfig illegal configuration, " + conf);
            }
            else {
                CollectConfig.assignmentCollectConfig(splitConf[0], splitConf[1]);
            }
        }
    }
}
