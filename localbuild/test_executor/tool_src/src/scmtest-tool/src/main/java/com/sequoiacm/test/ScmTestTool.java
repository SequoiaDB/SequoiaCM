package com.sequoiacm.test;

import com.sequoiacm.test.common.RefUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.subcommand.ScmTestSubcommand;
import com.sequoiacm.test.subcommand.Subcommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScmTestTool {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestTool.class);
    private static Map<String, ScmTestSubcommand> commands = new HashMap<>();

    public static void main(String[] args) {

        initSubCommands();

        if (args.length <= 0) {
            System.out.println("Please specify subcommand");
            displaySubcommandsDesc();
            System.exit(1);
        }

        if (args[0].equals("--help") || args[0].equals("-h")) {
            displaySubcommandsDesc();
            System.exit(0);
        }

        ScmTestSubcommand subcommand = commands.get(args[0]);
        if (subcommand == null) {
            logger.error("No such subcommand:" + args[0]);
            displaySubcommandsDesc();
            System.exit(1);
        }

        String[] subcommandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subcommandArgs, 0, subcommandArgs.length);

        try {
            subcommand.run(subcommandArgs);
            System.exit(0);
        }
        catch (IllegalArgumentException e) {
            logger.error("Execution failed, cause by:{}", e.getMessage(), e);
            System.exit(1);
        }
        catch (Exception e) {
            logger.error("Execution failed, detail:{}", LocalPathConfig.TEST_LOG_PATH, e);
            System.exit(1);
        }
    }

    private static void displaySubcommandsDesc() {
        System.out.println("Usage: <subcommand> [args]");
        System.out.println("Type '<subcommand> --help' for help on a specific subcommand");
        System.out.println("Available subcommands:");
        for (Map.Entry<String, ScmTestSubcommand> entry : commands.entrySet()) {
            System.out.println(entry.getKey() + ":\t\t\t" + entry.getValue().getDesc());
        }
    }

    private static void initSubCommands() {
        try {
            List<ScmTestSubcommand> instances = RefUtil
                    .initInstancesAnnotatedWith(Subcommand.class);
            for (ScmTestSubcommand instance : instances) {
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
