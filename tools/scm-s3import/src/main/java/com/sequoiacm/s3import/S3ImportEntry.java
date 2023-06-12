package com.sequoiacm.s3import;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.s3import.common.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.s3import.command.Command;
import com.sequoiacm.s3import.command.SubCommand;
import com.sequoiacm.s3import.common.RefUtils;

public class S3ImportEntry {

    private static final Logger logger = LoggerFactory.getLogger(S3ImportEntry.class);
    private static Map<String, SubCommand> commands = new HashMap<>();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        initSubCommand();

        if (args.length <= 0) {
            System.out.println("Please specify subcommand");
            displaySubcommandsDesc();
            System.exit(1);
        }

        if (args[0].equals("--help") || args[0].equals("-h")) {
            displaySubcommandsDesc();
            System.exit(0);
        }

        if (args[0].equals("--version") || args[0].equals("-v")) {
            try {
                ScmCommon.printVersion();
                System.exit(0);
            }
            catch (Exception e) {
                logger.error("print version failed", e);
                System.err.println("print version failed:" + e.getMessage());
                System.exit(1);
            }
        }

        SubCommand subcommand = commands.get(args[0]);
        if (subcommand == null) {
            logger.error("no such subcommand:" + args[0]);
            displaySubcommandsDesc();
            System.exit(1);
        }

        String[] subcommandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subcommandArgs, 0, subcommandArgs.length);

        try {
            subcommand.run(subcommandArgs);
            logger.debug("Exec success, total time: {}", System.currentTimeMillis() - startTime);
            System.exit(0);
        }
        catch (Exception e) {
            logger.debug("Exec interrupt, total time: {}", System.currentTimeMillis() - startTime);
            logger.error("Execution failed, cause by:" + e.getMessage(), e);
            System.err.println("\nExecution failed, cause by:" + e.getMessage());
            System.exit(1);
        }
    }

    private static void displaySubcommandsDesc() {
        System.out.println("Usage: <subcommand> [args]");
        System.out.println("Type '<subcommand> --help' for help on a specific subcommand");
        System.out.println("Type '--version / -v' to see the program version");
        System.out.println("Available subcommands:");
        for (Map.Entry<String, SubCommand> entry : commands.entrySet()) {
            System.out.println(
                    CommonUtils.getPaddingKey(entry.getKey()) + "\t" + entry.getValue().getDesc());
        }
    }

    private static void initSubCommand() {
        try {
            List<SubCommand> instances = RefUtils.initInstancesAnnotatedWith(Command.class);
            for (SubCommand instance : instances) {
                commands.put(instance.getName(), instance);
                logger.debug("Init subcommand:" + instance.getName());
            }
        }
        catch (Exception e) {
            logger.error("Failed to init commands", e);
            System.exit(1);
        }
    }
}
