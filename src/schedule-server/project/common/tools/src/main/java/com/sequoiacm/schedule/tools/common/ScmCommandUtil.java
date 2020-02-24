package com.sequoiacm.schedule.tools.common;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.tools.exception.ScmExitCode;
import com.sequoiacm.schedule.tools.exception.ScmToolsException;

public class ScmCommandUtil {
    private static final Logger logger = LoggerFactory.getLogger(ScmCommandUtil.class);

    public final static String OPT_SHORT_NODE_TYPE = "t";
    public final static String OPT_LONG_NODE_TYPE = "type";

    public static final String OPT_SHORT_HELP = "h";
    public static final String OPT_LONG_HELP = "help";
    public static final String OPT_LONG_VER = "version";
    public static final String OPT_SHORT_VER = "v";

    public static final String LOCALTION_SITE_NAME = "site";

    public static void addTypeOption(Options ops, ScmHelpGenerator hp, boolean isRequire,
            boolean haveAllOpDesc) throws ScmToolsException {
        StringBuilder typeOptDesc = new StringBuilder();
        if (haveAllOpDesc) {
            typeOptDesc.append("specify node type, arg:[ 0 | 1 ],\r\n");
            typeOptDesc.append("0:all, 1:schedule-server");
        }
        else {
            typeOptDesc.append("specify node type, arg:[ 1 ],\r\n");
            typeOptDesc.append("1:schedule-server");
        }
        Option op = hp.createOpt(OPT_SHORT_NODE_TYPE, OPT_LONG_NODE_TYPE, typeOptDesc.toString(),
                isRequire, true, false);
        ops.addOption(op);
        return;
    }

    public static CommandLine parseArgs(String[] args, Options options, boolean stopAtNonOption)
            throws ScmToolsException {
        CommandLine commandLine;
        CommandLineParser parser = new DefaultParser();
        try {
            commandLine = parser.parse(options, args, stopAtNonOption);
            return commandLine;
        }
        catch (ParseException e) {
            logger.error("Invalid arg", e);
            throw new ScmToolsException(e.getMessage(), ScmExitCode.INVALID_ARG);
        }
    }

    public static CommandLine parseArgs(String[] args, Options options) throws ScmToolsException {
        return parseArgs(args, options, false);
    }

    public static boolean isContainHelpArg(String[] args) {
        for (String arg : args) {
            if (arg.equals("-" + OPT_SHORT_HELP) || arg.equals("--" + OPT_LONG_HELP)
                    || arg.equals(OPT_LONG_HELP)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNeedPrintVersion(String[] args) {
        if (args.length == 1) {
            if (args[0].equals("-" + OPT_SHORT_VER) || args[0].equals("--" + OPT_LONG_VER)) {
                return true;
            }
        }
        return false;
    }

    public static int getTimeout(CommandLine commandLine, String timeoutOptName)
            throws ScmToolsException {
        int shortestTimeout = 5; // 5s
        String timeOutStr = commandLine.getOptionValue(timeoutOptName);
        int timeout = ScmCommon.convertStrToInt(timeOutStr);
        if (timeout < shortestTimeout) {
            timeout = shortestTimeout;
        }
        return timeout * 1000;
    }

}
