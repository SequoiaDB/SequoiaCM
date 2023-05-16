package com.sequoiacm.s3.tools.common;

import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            typeOptDesc.append("0:all, 1:s3-server");
        }
        else {
            typeOptDesc.append("specify node type, arg:[ 1 ],\r\n");
            typeOptDesc.append("1:s3-server");
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
            String msg = e.getMessage();
            if (e instanceof MissingArgumentException) {
                // 针对参数缺失异常的提示语句做特殊处理，以便于用户更好的理解
                msg = generateMissingArgMsg(((MissingArgumentException) e).getOption());
            }
            throw new ScmToolsException(msg, ScmExitCode.INVALID_ARG);
        }
    }

    private static String generateMissingArgMsg(Option option) {
        String longOpt = option.getLongOpt();
        String shortOpt = option.getOpt();
        String msg;
        if (longOpt != null) {
            msg = "Missing argument for option: --" + longOpt;
            if (shortOpt != null) {
                msg += "(-" + shortOpt + ")";
            }
        }
        else {
            msg = "Missing argument for option: -" + shortOpt;
        }
        return msg;
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

    public static String readPasswdFromStdIn() {
        return new String(System.console().readPassword());
    }

}
