package com.sequoiacm.clean;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class Entry {
    private static final Logger logger = LoggerFactory.getLogger(Entry.class);

    public static void main(String[] args) throws Exception {
        Options ops = new Options();
        if (args[0].equals("zkClean")) {
            args = Arrays.copyOfRange(args, 1, args.length);
            ops.addRequiredOption(null, "zkUrls", true, "zkUrls");
            ops.addRequiredOption(null, "maxBuffer", true, "mb");
            ops.addRequiredOption(null, "maxResidualTime", true, "ms");
            ops.addOption(null, "maxChildNum", true, "maxChildNum");

            if (Arrays.asList(args).contains("--help")) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("file-clean", ops);
                return;
            }
            CommandLineParser parser = new DefaultParser();

            CommandLine commandLine = parser.parse(ops, args, false);

            String zkUrls = commandLine.getOptionValue("zkUrls");

            int maxBuffer = Integer.parseInt(commandLine.getOptionValue("maxBuffer"));
            int maxResidualTime = Integer.parseInt(commandLine.getOptionValue("maxResidualTime"));
            int maxChildNum = Integer.parseInt(commandLine.getOptionValue("maxChildNum", "1000"));
            new ZkCleaner(zkUrls, maxBuffer, maxResidualTime, maxChildNum).cleanZk();
            return;
        }
        if (!args[0].equals("scmFileClean")) {
            throw new IllegalArgumentException(
                    "invalid subcommand:" + args[0] + ", eg: scmFileClean zkClean");
        }
        args = Arrays.copyOfRange(args, 1, args.length);

        ops.addRequiredOption(null, "workspace", true, "workspace name");
        ops.addRequiredOption(null, "fileMatcher", true, "file json matcher");
        ops.addRequiredOption(null, "cleanSiteId", true, "site id, clean site id");
        ops.addRequiredOption(null, "holdingDataSiteId", true, "site id holding data");
        ops.addRequiredOption(null, "holdingDataSiteInstances", true,
                "site holding data instance list");
        ops.addOption(null, "queueSize", true, "thread pool queue size");
        ops.addOption(null, "thread", true, "thread size");
        ops.addRequiredOption(null, "metaSdbCoord", true, "metaSdbCoord");
        ops.addRequiredOption(null, "metaSdbUser", true, "metaSdbUser");
        ops.addRequiredOption(null, "metaSdbPassword", true, "metaSdbPassword");

        ops.addRequiredOption(null, "cleanSiteLobSdbCoord", true, "cleanSiteLobSdbCoord");
        ops.addRequiredOption(null, "cleanSiteLobSdbUser", true, "cleanSiteLobSdbUser");
        ops.addRequiredOption(null, "cleanSiteLobSdbPassword", true, "cleanSiteLobSdbPassword");
        ops.addRequiredOption(null, "zkUrls", true, "zkUrls");

        if (Arrays.asList(args).contains("--help")) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("file-clean", ops);
            return;
        }
        CommandLineParser parser = new DefaultParser();

        CommandLine commandLine = parser.parse(ops, args, false);

        String wsName = commandLine.getOptionValue("workspace");

        BSONObject fileMatcher = (BSONObject) JSON.parse(commandLine.getOptionValue("fileMatcher"));
        int cleanSiteId = Integer.parseInt(commandLine.getOptionValue("cleanSiteId"));
        int holdingDataSiteId = Integer.parseInt(commandLine.getOptionValue("holdingDataSiteId"));
        List<String> holdingDataSiteInstances = Arrays
                .asList(commandLine.getOptionValue("holdingDataSiteInstances").split(","));
        int queueSize = Integer.parseInt(commandLine.getOptionValue("queueSize", "10000"));
        int thread = Integer.parseInt(commandLine.getOptionValue("thread", "20"));

        String metaSdbPassword = commandLine.getOptionValue("metaSdbPassword");
        String cleanSiteLobSdbPassword = commandLine.getOptionValue("cleanSiteLobSdbPassword");
        String cleanSiteLobSdbUser = commandLine.getOptionValue("cleanSiteLobSdbUser");
        List<String> metaSdbCoord = Arrays
                .asList(commandLine.getOptionValue("metaSdbCoord").split(","));
        String metaSdbUser = commandLine.getOptionValue("metaSdbUser");
        List<String> cleanSiteLobSdbCoordList = Arrays
                .asList(commandLine.getOptionValue("cleanSiteLobSdbCoord").split(","));
        String zkUrls = commandLine.getOptionValue("zkUrls");
        FileCleaner cleanner = new FileCleaner(wsName, fileMatcher, cleanSiteId, queueSize, thread,
                metaSdbPassword, metaSdbUser, metaSdbCoord, cleanSiteLobSdbUser,
                cleanSiteLobSdbPassword, cleanSiteLobSdbCoordList, zkUrls, holdingDataSiteId,
                holdingDataSiteInstances);
        try {
            cleanner.clean();
        }
        finally {
            cleanner.destroy();
        }
        logger.info("main thread exit..");
    }

}
