package com.sequoiacm.deploy.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.sequoiacm.deploy.common.CommandVisibleCheckerFactory;

public abstract class SubCommand {
    public abstract String getName();

    public abstract String getDesc();

    protected abstract Options commandOptions();

    protected abstract void process(CommandLine cl) throws Exception;

    public void run(String[] args) throws Exception {
        Options options = commandOptions();
        options.addOption(
                Option.builder("h").longOpt("help").hasArg(false).required(false).build());

        CommandLine cl = parseOptions(args, options);

        boolean isContinue = beforeProcess(cl);
        if (!isContinue) {
            return;
        }

        process(cl);

    }

    private CommandLine parseOptions(String[] args, Options options) throws ParseException {
        try {
            return new DefaultParser().parse(options, args);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected void printHelp() {
        Options ops = commandOptions();

        Options visibleOps = CommandVisibleCheckerFactory.getInstance()
                .filterInvisibleOptions(getName(), ops);

        if (visibleOps == null) {
            throw new IllegalArgumentException("no such subcommand:" + getName());
        }

        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", visibleOps);
    }

    // return true if subcommand should be run
    protected boolean beforeProcess(CommandLine commandLine) {
        if (commandLine.hasOption("help")) {
            printHelp();
            return false;
        }

        CommandVisibleCheckerFactory.getInstance().checkIsValid(getName(), commandLine);

        return true;
    }

}
