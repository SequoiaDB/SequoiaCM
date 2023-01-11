package com.sequoiacm.diagnose.command;


import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import java.util.Arrays;

public abstract class SubCommand {

    protected abstract Options addParam();

    public abstract String getName();

    public abstract String getDesc();

    public abstract void run(String[] args) throws Exception;

    public abstract void printHelp();

    public boolean hasHelp(String[] args) {
        if (Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-h")) {
            return true;
        }
        return false;
    }
}
