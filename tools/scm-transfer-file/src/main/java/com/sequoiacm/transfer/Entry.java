package com.sequoiacm.transfer;

import com.sequoiacm.client.exception.ScmException;
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

import java.util.Arrays;
import java.util.List;

public class Entry {
    private static final Logger logger = LoggerFactory.getLogger(Entry.class);
    public static void main(String[] args) throws ParseException, ScmException, InterruptedException {
        Options ops = new Options();
        ops.addOption(null, "batchSize", true, "batch size");
        ops.addOption(null, "thread", true, "thread size");
        ops.addOption(null, "queueSize", true, "thread pool queue size");
        ops.addOption(null, "fileStatusCheckBatchSize", true, "file status check batch size");
        ops.addOption(null, "help", false, "help");
        ops.addOption(null, "fileTransferTimeout", true, "file timeout for transfer");
        ops.addOption(null, "fileStatusCheckInterval", true, "file status check interval");
        ops.addRequiredOption(null, "url", true, "gateway url, connect to transfer target site: gatewayhost:port/target-site-Name");
        ops.addRequiredOption(null, "scmUser", true, "scm user name");
        ops.addOption(Option.builder(null).longOpt("scmPassword").desc("scm scmPassword").optionalArg(true).hasArg(true).required(true).build());
        ops.addRequiredOption(null, "siteId", true, "site id, target site id");
        ops.addRequiredOption(null, "workspace", true, "workspace name");
        ops.addRequiredOption(null, "fileMatcher", true, "file json matcher");

        ops.addRequiredOption(null, "sdbCoord", true, "metasource sdb coord: sdbhost:port");
        ops.addRequiredOption(null, "sdbUser", true, "metasource sdb user");
        ops.addOption(Option.builder(null).longOpt("sdbPassword").desc("metasource sdb password").optionalArg(true).hasArg(true).required(true).build());


        if (Arrays.asList(args).contains("--help")) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("file-transfer", ops);
            return;
        }
        CommandLineParser parser = new DefaultParser();

        CommandLine commandLine = parser.parse(ops, args, true);

        int batchSize = Integer.parseInt(commandLine.getOptionValue("batchSize", "10000"));
        int thread = Integer.parseInt(commandLine.getOptionValue("thread", "50"));
        int queueSize = Integer.parseInt(commandLine.getOptionValue("queueSize", "10000"));
        int fileStatusCheckBatchSize = Integer.parseInt(commandLine.getOptionValue("fileStatusCheckBatchSize", "1000"));
        int fileStatusCheckInterval = Integer.parseInt(commandLine.getOptionValue("fileStatusCheckInterval", "1000"));
        String url = commandLine.getOptionValue("url");
        String scmUser = commandLine.getOptionValue("scmUser");
        String scmPassword = commandLine.getOptionValue("scmPassword");
        if (scmPassword == null) {
            System.out.print("scmPassword for " + scmUser + ": ");
            scmPassword = new String(System.console().readPassword());
        }

        List<String> sdbCoord = Arrays
                .asList(commandLine.getOptionValue("sdbCoord").split(","));
        String sdbUser = commandLine.getOptionValue("sdbUser");
        String sdbPassword = commandLine.getOptionValue("sdbPassword");
        if (sdbPassword == null) {
            System.out.print("sdbPassword for " + sdbUser + ": ");
            sdbPassword = new String(System.console().readPassword());
        }

        int siteId = Integer.parseInt(commandLine.getOptionValue("siteId"));
        String workspace = commandLine.getOptionValue("workspace");
        String fileMatcherStr = commandLine.getOptionValue("fileMatcher");
        BSONObject fileMatcher = (BSONObject) JSON.parse(fileMatcherStr);
        int fileTimeout = Integer.parseInt(commandLine.getOptionValue("fileTransferTimeout", "180000"));


        logger.info("transfer begin, args: workspace={}, url={}, scmUser={}, scmPassword=**,  sdbCoord={}, sdbUser={}, sdbPassword=**, " +
                        "batchSize={}, thread={}, queueSize={}, fileStatusCheckInterval={}ms, fileStatusCheckBatchSize={}, fileTimeout={}ms, fileMatcher={}", workspace, url, scmUser, sdbCoord, sdbUser
                , batchSize, thread, queueSize, fileStatusCheckInterval, fileStatusCheckBatchSize, fileTimeout, fileMatcher);

        FileReadFromSrcSite transfer = new FileReadFromSrcSite(batchSize, fileTimeout, thread, queueSize, siteId, workspace, fileMatcher,
                Arrays.asList(url.split(",")), scmUser, scmPassword, fileStatusCheckInterval, sdbCoord, sdbUser, sdbPassword, fileStatusCheckBatchSize);

        try {
            transfer.transfer();
        } finally {
            transfer.destroy();
        }
    }
}
