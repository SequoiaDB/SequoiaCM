package com.sequoiacm.clean;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import org.apache.commons.cli.*;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Entry {
    private static final Logger logger = LoggerFactory.getLogger(Entry.class);

    private static final String ZK_URLS = "zkUrls";
    private static final String MAX_BUFFERS = "maxBuffer";
    private static final String MAX_RESIDUAL_TIME = "maxResidualTime";
    private static final String MAX_CHILD_NUM = "maxChildNum";

    private static final String HOLDING_DATA_SITE_NAME = "holdingDataSiteName";
    private static final String TARGET_SITE_INSTANCES = "targetSiteInstances";
    private static final String WORKSPACE = "workspace";
    private static final String FILE_MATCHER = "fileMatcher";
    private static final String CLEAN_SITE_NAME = "cleanSiteName";
    private static final String DATASOURCE_CONF = "datasourceConf";
    private static final String QUEUE_SIZE = "queueSize";
    private static final String THREAD = "thread";
    private static final String SRC_SITE_PASSWORD_FILE = "srcSitePasswordFile";
    private static final String SRC_SITE_PASSWORD = "srcSitePassword";
    private static final String META_SDB_COORD = "metaSdbCoord";
    private static final String META_SDB_USER = "metaSdbUser";
    private static final String META_SDB_PASSWORD = "metaSdbPassword";
    private static final String META_SDB_PASSWORD_FILE = "metaSdbPasswordFile";
    private static final String CONNECT_TIMEOUT = "connectTimeout";
    private static final String SOCKET_TIMEOUT = "socketTimeout";
    private static final String MAX_CONNECTION_NUM = "maxConnectionNum";
    private static final String KEEP_ALIVE_TIMEOUT = "keepAliveTimeout";
    private static final String LOGBACK_PATH = "logbackPath";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing required arg: zkClean or scmFileClean");
        }
        Options ops = new Options();
        ops.addRequiredOption(null, ZK_URLS, true, "zkUrls");
        ops.addOption(Option.builder(null).longOpt(LOGBACK_PATH).desc("logbackPath")
                .optionalArg(true).hasArg(true).required(false).build());
        if (args[0].equals("zkClean")) {
            args = Arrays.copyOfRange(args, 1, args.length);
            ops.addRequiredOption(null, MAX_BUFFERS, true, "mb");
            ops.addRequiredOption(null, MAX_RESIDUAL_TIME, true, "ms");
            ops.addOption(null, MAX_CHILD_NUM, true, "maxChildNum");

            if (Arrays.asList(args).contains("--help")) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("file-clean", ops);
                return;
            }
            CommandLineParser parser = new DefaultParser();

            CommandLine commandLine = parser.parse(ops, args, false);
            String logbackPath = commandLine.getOptionValue(LOGBACK_PATH);
            changeLogbackFile(logbackPath);

            String zkUrls = commandLine.getOptionValue(ZK_URLS);

            int maxBuffer = Integer.parseInt(commandLine.getOptionValue(MAX_BUFFERS));
            int maxResidualTime = Integer.parseInt(commandLine.getOptionValue(MAX_RESIDUAL_TIME));
            int maxChildNum = Integer.parseInt(commandLine.getOptionValue(MAX_CHILD_NUM, "1000"));

            logger.info(
                    "zk clean begin, args: zkUrls={}, maxBuffer={}, maxResidualTime={}, maxChildNum={}",
                    zkUrls, maxBuffer, maxResidualTime, maxChildNum);

            new ZkCleaner(zkUrls, maxBuffer, maxResidualTime, maxChildNum).cleanZk();
            return;
        }
        if (!args[0].equals("scmFileClean")) {
            throw new IllegalArgumentException(
                    "invalid subcommand:" + args[0] + ", eg: scmFileClean zkClean");
        }
        args = Arrays.copyOfRange(args, 1, args.length);

        ops.addRequiredOption(null, WORKSPACE, true, "workspace name");
        ops.addRequiredOption(null, FILE_MATCHER, true, "file json matcher");
        ops.addRequiredOption(null, CLEAN_SITE_NAME, true, "site name, clean site name");
        ops.addRequiredOption(null, HOLDING_DATA_SITE_NAME, true, "holdingDataSiteName");
        ops.addOption(Option.builder(null).longOpt(DATASOURCE_CONF).desc("datasource config")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(TARGET_SITE_INSTANCES)
                .desc("site holding data instance list").optionalArg(true).hasArg(true)
                .required(false).build());
        ops.addOption(null, QUEUE_SIZE, true, "thread pool queue size");
        ops.addOption(null, THREAD, true, "thread size");
        ops.addOption(
                Option.builder(null).longOpt(SRC_SITE_PASSWORD_FILE).desc("srcSitePasswordFile")
                        .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(SRC_SITE_PASSWORD).desc("srcSitePassword")
                .optionalArg(true).hasArg(true).required(false).build());

        ops.addRequiredOption(null, META_SDB_COORD, true, "metaSdbCoord");
        ops.addRequiredOption(null, META_SDB_USER, true, "metaSdbUser");
        ops.addOption(Option.builder(null).longOpt(META_SDB_PASSWORD).desc("metaSdbPassword")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(
                Option.builder(null).longOpt(META_SDB_PASSWORD_FILE).desc("metaSdbPasswordFile")
                        .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(null, CONNECT_TIMEOUT, true, "connectTimeout");
        ops.addOption(null, SOCKET_TIMEOUT, true, "socketTimeout");
        ops.addOption(null, MAX_CONNECTION_NUM, true, "maxConnectionNum");
        ops.addOption(null, KEEP_ALIVE_TIMEOUT, true, "keepAliveTimeout");


        if (Arrays.asList(args).contains("--help")) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("file-clean", ops);
            return;
        }
        CommandLineParser parser = new DefaultParser();

        CommandLine commandLine = parser.parse(ops, args, false);
        String logbackPath = commandLine.getOptionValue(LOGBACK_PATH);
        changeLogbackFile(logbackPath);
        String wsName = commandLine.getOptionValue(WORKSPACE);

        BSONObject fileMatcher = (BSONObject) JSON.parse(commandLine.getOptionValue(FILE_MATCHER));
        List<String> targetDataSiteInstances = new ArrayList<>();
        if (!StringUtils.isEmpty(commandLine.getOptionValue(TARGET_SITE_INSTANCES))) {
            targetDataSiteInstances = Arrays
                    .asList(commandLine.getOptionValue(TARGET_SITE_INSTANCES).split(","));
        }
        String cleanSiteName = commandLine.getOptionValue(CLEAN_SITE_NAME);
        String holdingDataSiteName = commandLine.getOptionValue(HOLDING_DATA_SITE_NAME);
        Map<String, String> datasourceConf = new HashMap<>();
        String datasourceConfStr = commandLine.getOptionValue(DATASOURCE_CONF);
        if (!StringUtils.isEmpty(datasourceConfStr)) {
            for (String s : datasourceConfStr.split(",")) {
                String[] arr = s.split("=");
                if (arr.length != 2) {
                    throw new RuntimeException("invalid argument, datasourceConf format error, eg: k1=v2,k2=v2");
                } else {
                    datasourceConf.put(arr[0], arr[1]);
                }
            }
        }
        int queueSize = Integer.parseInt(commandLine.getOptionValue(QUEUE_SIZE, "10000"));
        int thread = Integer.parseInt(commandLine.getOptionValue(THREAD, "20"));
        String srcSitePasswordFile = commandLine.getOptionValue(SRC_SITE_PASSWORD_FILE);
        String srcSitePassword = commandLine.getOptionValue(SRC_SITE_PASSWORD);
        if (StringUtils.isEmpty(srcSitePasswordFile) && StringUtils.isEmpty(srcSitePassword)) {
            throw new RuntimeException(
                    "invalid argument, srcSitePassword or srcSitePasswordFile can not be empty");
        }

        String metaSdbUser = commandLine.getOptionValue(META_SDB_USER);
        String metaSdbPassword = commandLine.getOptionValue(META_SDB_PASSWORD);
        if (StringUtils.isEmpty(metaSdbPassword)) {
            String metaSdbPasswordFile = commandLine.getOptionValue(META_SDB_PASSWORD_FILE);
            if (!StringUtils.isEmpty(metaSdbPasswordFile)) {
                AuthInfo authInfo = ScmFilePasswordParser.parserFile(metaSdbPasswordFile);
                metaSdbPassword = authInfo.getPassword();
            }
            else {
                throw new RuntimeException(
                        "invalid argument, metaSdbUser or metaSdbUserFile can not be empty");
            }
        }

        List<String> metaSdbCoord = Arrays
                .asList(commandLine.getOptionValue(META_SDB_COORD).split(","));
        int connectTimeout = Integer
                .parseInt(commandLine.getOptionValue(CONNECT_TIMEOUT, "10000"));
        int socketTimeout = Integer.parseInt(commandLine.getOptionValue(SOCKET_TIMEOUT, "0"));
        int maxConnectionNum = Integer
                .parseInt(commandLine.getOptionValue(MAX_CONNECTION_NUM, "500"));
        int keepAliveTimeout = Integer
                .parseInt(commandLine.getOptionValue(KEEP_ALIVE_TIMEOUT, "0"));
        String zkUrls = commandLine.getOptionValue(ZK_URLS);

        logger.info(
                "clean begin, args: workspace={}, fileMatcher={}, targetDataSiteInstances={}, cleanSiteName={}, "
                        + "holdingDataSiteName={}, queueSize={}, thread={}, srcSitePasswordFile={}, srcSitePassword=**, "
                        + "metaSdbPassword=**, metaSdbUser={}, metaSdbCoord={}, connectTimeout={}ms, socketTimeout={}ms, "
                        + "maxConnectionNum={}, keepAliveTimeout={}, zkUrls={}",
                wsName, fileMatcher, targetDataSiteInstances, cleanSiteName, holdingDataSiteName,
                queueSize, thread, srcSitePasswordFile, metaSdbUser, metaSdbCoord, connectTimeout,
                socketTimeout, maxConnectionNum, keepAliveTimeout, zkUrls);

        FileCleaner cleanner = new FileCleaner(wsName, fileMatcher, targetDataSiteInstances,
                cleanSiteName, holdingDataSiteName, queueSize, thread, srcSitePasswordFile,
                srcSitePassword, metaSdbPassword, metaSdbUser, metaSdbCoord, connectTimeout,
                socketTimeout, maxConnectionNum, keepAliveTimeout, zkUrls, datasourceConf);
        try {
            cleanner.clean();
        }
        finally {
            cleanner.destroy();
        }
        logger.info("main thread exit..");
    }

    private static void changeLogbackFile(String logbackPath)
            throws StrategyInvalidArgumentException, IOException {
        InputStream is = null;
        try {
            if (StringUtils.isEmpty(logbackPath)) {
                is = new ClassPathResource("cleanLogback.xml").getInputStream();
            }
            else {
                is = new FileInputStream(logbackPath);
            }
            ScmHelper.configToolsLog(is);
        }
        catch (FileNotFoundException e) {
            throw new StrategyInvalidArgumentException(
                    "logbackPath is " + logbackPath + "The logback xml file does not exist");
        }
        finally {
            ScmCommon.closeResource(is);
        }
    }
}
