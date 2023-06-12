package com.sequoiacm.transfer;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
import java.util.Arrays;
import java.util.List;

public class Entry {
    private static final Logger logger = LoggerFactory.getLogger(Entry.class);
    
    private static final String BATCH_SIZE = "batchSize";
    private static final String THREAD = "thread";
    private static final String QUEUE_SIZE = "queueSize";
    private static final String FILE_STATUS_CHECK_BATCH_SIZE = "fileStatusCheckBatchSize";
    private static final String FILE_TRANSFER_TIMEOUT = "fileTransferTimeout";
    private static final String FILE_STATUS_CHECK_INTERVAL = "fileStatusCheckInterval";
    private static final String URL = "url";
    private static final String SCM_USER = "scmUser";
    private static final String SCM_PASSWORD = "scmPassword";
    private static final String SCM_PASSWORD_FILE = "scmPasswordFile";
    private static final String SITE_NAME = "siteName";
    private static final String WORKSPACE = "workspace";
    private static final String FILE_MATCHER = "fileMatcher";
    private static final String SDB_COORD = "sdbCoord";
    private static final String SDB_USER = "sdbUser";
    private static final String SDB_PASSWORD = "sdbPassword";
    private static final String SDB_PASSWORD_FILE = "sdbPasswordFile";
    private static final String LOGBACK_PATH = "logbackPath";

    public static void main(String[] args)
            throws ParseException, ScmException, InterruptedException, ScmMappingException,
            StrategyInvalidArgumentException, IOException {

        // print version information
        if (Arrays.asList(args).contains("--version") || Arrays.asList(args).contains("-v")) {
            try {
                ScmCommon.printVersion();
                return;
            }
            catch (Exception e) {
                logger.error("print version failed", e);
                throw new RuntimeException("print version failed:" + e.getMessage());
            }
        }

        Options ops = new Options();
        ops.addOption(null, BATCH_SIZE, true, "batch size");
        ops.addOption(null, THREAD, true, "thread size");
        ops.addOption(null, QUEUE_SIZE, true, "thread pool queue size");
        ops.addOption(null, FILE_STATUS_CHECK_BATCH_SIZE, true, "file status check batch size");
        ops.addOption(null, "help", false, "help");
        ops.addOption(null, FILE_TRANSFER_TIMEOUT, true, "file timeout for transfer");
        ops.addOption(null, FILE_STATUS_CHECK_INTERVAL, true, "file status check interval");
        ops.addRequiredOption(null, URL, true,
                "gateway url, connect to transfer target site: gatewayhost:port/target-site-Name");
        ops.addRequiredOption(null, SCM_USER, true, "scm user name");
        ops.addOption(Option.builder(null).longOpt(SCM_PASSWORD).desc("scm scmPassword")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(SCM_PASSWORD_FILE).desc("scm scmPasswordFile")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addRequiredOption(null, SITE_NAME, true, "site name, target site name");
        ops.addRequiredOption(null, WORKSPACE, true, "workspace name");
        ops.addRequiredOption(null, FILE_MATCHER, true, "file json matcher");

        ops.addRequiredOption(null, SDB_COORD, true, "metasource sdb coord: sdbhost:port");
        ops.addRequiredOption(null, SDB_USER, true, "metasource sdb user");
        ops.addOption(Option.builder(null).longOpt(SDB_PASSWORD).desc("metasource sdb password")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(
                Option.builder(null).longOpt(SDB_PASSWORD_FILE).desc("metasource sdb passwordFile")
                        .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(LOGBACK_PATH).desc("logbackPath")
                .optionalArg(true).hasArg(true).required(false).build());

        if (Arrays.asList(args).contains("--help")) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("file-transfer", ops);
            return;
        }
        CommandLineParser parser = new DefaultParser();

        CommandLine commandLine = parser.parse(ops, args, true);

        String logbackPath = commandLine.getOptionValue(LOGBACK_PATH);
        changeLogbackFile(logbackPath);

        int batchSize = Integer.parseInt(commandLine.getOptionValue(BATCH_SIZE, "10000"));
        int thread = Integer.parseInt(commandLine.getOptionValue(THREAD, "50"));
        int queueSize = Integer.parseInt(commandLine.getOptionValue(QUEUE_SIZE, "10000"));
        int fileStatusCheckBatchSize = Integer
                .parseInt(commandLine.getOptionValue(FILE_STATUS_CHECK_BATCH_SIZE, "1000"));
        int fileStatusCheckInterval = Integer
                .parseInt(commandLine.getOptionValue(FILE_STATUS_CHECK_INTERVAL, "1000"));
        String url = commandLine.getOptionValue(URL);
        String scmUser = commandLine.getOptionValue(SCM_USER);
        String scmPassword = commandLine.getOptionValue(SCM_PASSWORD);
        if (!Strings.hasText(scmPassword)) {
            String scmPasswordFile = commandLine.getOptionValue(SCM_PASSWORD_FILE);
            if (Strings.hasText(scmPasswordFile)) {
                AuthInfo authInfo = ScmFilePasswordParser.parserFile(scmPasswordFile);
                scmPassword = authInfo.getPassword();
            }
            else {
                throw new RuntimeException(
                        "invalid argument, scmPassword or scmPasswordFile can not be empty");
            }
        }

        List<String> sdbCoord = Arrays
                .asList(commandLine.getOptionValue(SDB_COORD).split(","));
        String sdbUser = commandLine.getOptionValue(SDB_USER);
        String sdbPassword = commandLine.getOptionValue(SDB_PASSWORD);
        if (!Strings.hasText(sdbPassword)) {
            String sdbPasswordFile = commandLine.getOptionValue(SDB_PASSWORD_FILE);
            if (Strings.hasText(sdbPasswordFile)) {
                AuthInfo authInfo = ScmFilePasswordParser.parserFile(sdbPasswordFile);
                sdbPassword = authInfo.getPassword();
            }
            else {
                throw new RuntimeException(
                        "invalid argument, sdbPassword or sdbPasswordFile can not be empty");
            }
        }

        String siteName = commandLine.getOptionValue(SITE_NAME);
        String workspace = commandLine.getOptionValue(WORKSPACE);
        String fileMatcherStr = commandLine.getOptionValue(FILE_MATCHER);
        BSONObject fileMatcher = (BSONObject) JSON.parse(fileMatcherStr);
        int fileTimeout = Integer
                .parseInt(commandLine.getOptionValue(FILE_TRANSFER_TIMEOUT, "180000"));
        
        logger.info(
                "transfer begin, args: workspace={}, url={}, scmUser={}, scmPassword=**,  sdbCoord={}, sdbUser={}, "
                        + "sdbPassword=**, batchSize={}, thread={}, queueSize={}, fileStatusCheckInterval={}ms, "
                        + "fileStatusCheckBatchSize={}, fileTimeout={}ms, fileMatcher={}",
                workspace, url, scmUser, sdbCoord, sdbUser, batchSize, thread, queueSize,
                fileStatusCheckInterval, fileStatusCheckBatchSize, fileTimeout, fileMatcher);

        FileTransfer transfer = new FileTransfer(batchSize, fileTimeout, thread,
                queueSize, siteName, workspace, fileMatcher, Arrays.asList(url.split(",")), scmUser,
                scmPassword, fileStatusCheckInterval, sdbCoord, sdbUser, sdbPassword,
                fileStatusCheckBatchSize);
        try {
            transfer.transfer();
        } finally {
            transfer.destroy();
        }
    }

    private static void changeLogbackFile(String logbackPath)
            throws StrategyInvalidArgumentException, IOException {
        InputStream is = null;
        try {
            if (StringUtils.isEmpty(logbackPath)) {
                is = new ClassPathResource("transferLogback.xml").getInputStream();
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
