package com.sequoiacm.test.subcommand;

import org.apache.commons.cli.*;

public abstract class ScmTestSubcommand<T> {

    public abstract String getName();

    public abstract String getDesc();

    protected abstract Options commandOptions();

    protected abstract T parseCommandLineArgs(CommandLine commandLine);

    protected abstract void process(T args) throws Exception;

    public void run(String[] args) throws Exception {
        Options options = commandOptions();
        options.addOption(
                Option.builder("h").longOpt("help").hasArg(false).required(false).build());

        CommandLine cl = parseOptions(args, options);
        if (cl.hasOption("help")) {
            printHelp();
            return;
        }

        T arg = parseCommandLineArgs(cl);
        process(arg);
    }

    private CommandLine parseOptions(String[] args, Options options) {
        try {
            return new DefaultParser().parse(options, args);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected void printHelp() {
        Options ops = commandOptions();
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", ops);
    }
}

