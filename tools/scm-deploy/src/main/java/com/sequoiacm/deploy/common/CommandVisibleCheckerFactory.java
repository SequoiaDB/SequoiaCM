package com.sequoiacm.deploy.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandVisibleCheckerFactory {
    private static final Logger logger = LoggerFactory
            .getLogger(CommandVisibleCheckerFactory.class);
    private static volatile CommandVisibleChecker instance;

    public static CommandVisibleChecker getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (CommandVisibleCheckerFactory.class) {
            if (instance != null) {
                return instance;
            }
            Map<String, List<String>> visibleCommmandMap = initVisibleCommandMap();
            if (visibleCommmandMap == null) {
                instance = new CommandVisibleCheckerNOP();
            }
            else {
                logger.debug("visible commands:" + visibleCommmandMap);
                instance = new CommandVisibleCheckerDefault(visibleCommmandMap);
            }
            return instance;
        }

    }

    private static Map<String, List<String>> initVisibleCommandMap() {
        String commandsPath = System.getProperty("visible.commands");
        if (commandsPath == null) {
            return null;
        }

        File commandsDir = new File(commandsPath);
        if (!commandsDir.exists()) {
            throw new IllegalArgumentException("visible.commands dir not exist:" + commandsPath);
        }
        if (!commandsDir.isDirectory()) {
            throw new IllegalArgumentException("visible.commands is not directory:" + commandsPath);
        }

        Map<String, List<String>> visibleCommmandMap = new HashMap<>();
        File[] subCommandDirs = commandsDir.listFiles();
        for (File subCommandDir : subCommandDirs) {
            if (!subCommandDir.isDirectory()) {
                continue;
            }
            File[] options = subCommandDir.listFiles();
            for (File option : options) {
                if (!option.isFile()) {
                    continue;
                }
                List<String> optionList = visibleCommmandMap.get(subCommandDir.getName());
                if (optionList == null) {
                    optionList = new ArrayList<>();
                    visibleCommmandMap.put(subCommandDir.getName(), optionList);
                }
                optionList.add(option.getName());
            }
        }
        return visibleCommmandMap;
    }
}

class CommandVisibleCheckerNOP implements CommandVisibleChecker {

    @Override
    public boolean isVisible(String subCommand) {
        return true;
    }

    @Override
    public Options filterInvisibleOptions(String subCommand, Options ops) {
        return ops;
    }

    @Override
    public void checkIsValid(String subCommand, CommandLine cl) throws IllegalArgumentException {
        return;
    }

}

class CommandVisibleCheckerDefault implements CommandVisibleChecker {
    private Map<String, List<String>> visibleCommands;

    public CommandVisibleCheckerDefault(Map<String, List<String>> visibleCommands) {
        this.visibleCommands = visibleCommands;
    }

    @Override
    public boolean isVisible(String subCommand) {
        List<String> visibleOptions = visibleCommands.get(subCommand);
        if (visibleOptions == null) {
            return false;
        }
        return true;
    }

    @Override
    public Options filterInvisibleOptions(String subCommand, Options ops) {
        if (!isVisible(subCommand)) {
            return null;
        }

        List<String> visibleOptions = visibleCommands.get(subCommand);
        Options retOptions = new Options();
        for (Option option : ops.getOptions()) {
            String longOpt = option.getLongOpt();
            String shortOpt = option.getOpt();
            if (!visibleOptions.contains(longOpt) && !visibleOptions.contains(shortOpt)) {
                continue;
            }
            retOptions.addOption(option);
        }
        return retOptions;
    }

    @Override
    public void checkIsValid(String subCommand, CommandLine cl) throws IllegalArgumentException {
        if (!isVisible(subCommand)) {
            throw new IllegalArgumentException("no such subcommand:" + subCommand);
        }
        List<String> visibleOptions = visibleCommands.get(subCommand);
        Option[] options = cl.getOptions();
        for (Option option : options) {
            String longOpt = option.getLongOpt();
            String shortOpt = option.getOpt();
            if (!visibleOptions.contains(longOpt) && !visibleOptions.contains(shortOpt)) {
                String msg = longOpt == null ? "" : ("--" + longOpt);
                msg += shortOpt == null ? "" : ("-" + shortOpt);
                throw new IllegalArgumentException("unregnized option:" + msg);
            }
        }
    }
}