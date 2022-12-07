package com.sequoiacm.diagnose;

import com.sequoiacm.diagnose.command.Command;
import com.sequoiacm.diagnose.command.SubCommand;
import com.sequoiacm.diagnose.utils.RefUtils;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiagnoseEntry {
    private static final Logger logger = LoggerFactory.getLogger(DiagnoseEntry.class);
    private static HashMap<String, SubCommand> commands = new HashMap<>();
    private static String binPath = System.getProperty("binPath");

    public static void main(String[] args) throws Exception {
        ScmHelper.configToolsLog("diagnoseLogback.xml");
        initSubCommands();

        if (args.length <= 0) {
            System.out.println("Please specify subcommand");
            displaySubcommandsDesc();
            System.exit(1);
        }

        if (args[0].equals("--help") || args[0].equals("--h")) {
            displaySubcommandsDesc();
            System.exit(0);
        }

        SubCommand command = commands.get(args[0]);
        if (command == null) {
            System.out.println("No such subcommand:" + args[0]);
            displaySubcommandsDesc();
            System.exit(1);
        }

        String[] subcommandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subcommandArgs, 0, subcommandArgs.length);
        try {
            command.run(subcommandArgs);
            System.exit(0);
        }
        catch (IllegalArgumentException e) {
            System.err.println("[ERROR] Execution failed, cause by:" + e.getMessage());
            logger.error("Execution failed, cause by:{}", e.getMessage(), e);
            System.exit(1);
        }
        catch (Exception e) {
            System.err.println(
                    "[ERROR] Execution failed,detail:" + binPath + "/../log/scm-diagnose.log");
            logger.error("Execution failed, detail:{}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void initSubCommands() {
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

    private static void displaySubcommandsDesc() {
        System.out.println("Usage: <subcommand> [args]");
        System.out.println("Type '<subcommand> --help' for help on a specific subcommand");
        System.out.println("Available subcommands:");
        for (Map.Entry<String, SubCommand> entry : commands.entrySet()) {
            System.out.println(entry.getKey() + ":\t\t\t" + entry.getValue().getDesc());
        }
    }
}
