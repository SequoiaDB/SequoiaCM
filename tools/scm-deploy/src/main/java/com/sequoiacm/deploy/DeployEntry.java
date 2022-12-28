package com.sequoiacm.deploy;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.sequoiacm.deploy.exception.RollbackException;
import com.sequoiacm.deploy.exception.UpgradeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.command.Command;
import com.sequoiacm.deploy.command.SubCommand;
import com.sequoiacm.deploy.common.CommandVisibleChecker;
import com.sequoiacm.deploy.common.CommandVisibleCheckerFactory;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.RefUtil;
import com.sequoiacm.deploy.exception.DeployException;

public class DeployEntry {
    private static final Logger logger = LoggerFactory.getLogger(DeployEntry.class);
    private static HashMap<String, SubCommand> commands = new HashMap<>();

    public static void main(String[] args) {
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
            logger.error("No such subcommand:" + args[0]);
            displaySubcommandsDesc();
            System.exit(1);
        }

        if (!CommandVisibleCheckerFactory.getInstance().isVisible(command.getName())) {
            logger.error("No such subcommand:" + command.getName());
            System.exit(1);
        }

        String[] subCommandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subCommandArgs, 0, subCommandArgs.length);

        try {
            command.run(subCommandArgs);
            System.exit(0);
        }
        catch (IllegalArgumentException e) {
            logger.error("Execution failed, cause by:{}", e.getMessage(), e);
            System.exit(1);
        }
        catch (DeployException e) {
            logger.error("Execution failed, error: {}", e.getMessage(), e);
            logger.error("Execution detail:{}", CommonUtils.getLogFilePath());
            logger.error("Please check the above exceptions, and then:" + e.getHelp());
            System.exit(1);
        }
        catch (UpgradeException | RollbackException e) {
            logger.error("Execution failed, error: {}", e.getMessage(), e);
            logger.error("Execution detail:{}", CommonUtils.getLogFilePath());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Execution failed, detail:{}", CommonUtils.getLogFilePath(), e);
            System.exit(1);
        }

    }

    private static void displaySubcommandsDesc() {
        System.out.println("Usage: <subcommand> [args]");
        System.out.println("Type '<subcommand> --help' for help on a specific subcommand");
        System.out.println("Available subcommands:");
        CommandVisibleChecker checker = CommandVisibleCheckerFactory.getInstance();
        for (Entry<String, SubCommand> entry : commands.entrySet()) {
            if (checker.isVisible(entry.getKey())) {
                System.out.println(entry.getKey() + ":\t\t\t" + entry.getValue().getDesc());
            }
        }
    }

    private static void initSubCommands() {
        try {
            List<SubCommand> instances = RefUtil.initInstancesAnnotatedWith(Command.class);
            for (SubCommand instance : instances) {
                commands.put(instance.getName(), instance);
                logger.debug("init subcommand:" + instance.getName());
            }
        }
        catch (Exception e) {
            logger.error("Failed to init commands", e);
            System.exit(1);
        }
    }

}
