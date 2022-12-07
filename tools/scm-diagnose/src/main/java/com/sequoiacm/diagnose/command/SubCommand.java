package com.sequoiacm.diagnose.command;


import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public abstract class SubCommand {

    protected abstract Options addParam() throws ParseException;

    public abstract String getName();

    public abstract String getDesc();

    public abstract void run(String[] args) throws Exception;
}
