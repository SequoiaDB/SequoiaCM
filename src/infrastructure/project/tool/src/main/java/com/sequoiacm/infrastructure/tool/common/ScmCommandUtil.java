package com.sequoiacm.infrastructure.tool.common;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmCommandUtil {
    private static final Logger logger = LoggerFactory.getLogger(ScmCommandUtil.class);

    public final static String OPT_SHORT_NODE_TYPE = "t";
    public final static String OPT_LONG_NODE_TYPE = "type";
    public final static String OPT_SHORT_PORT = "p";
    public final static String OPT_LONG_PORT = "port";
    public final static String OPT_SHORT_MODE = "m";
    public final static String OPT_LONG_MODE = "mode";
    public final static String OPT_SHORT_LONG = "l";
    public final static String OPT_LONG_LONG = "long";

    public static final String OPT_SHORT_HELP = "h";
    public static final String OPT_LONG_HELP = "help";
    public static final String OPT_LONG_VER = "version";
    public static final String OPT_SHORT_VER = "v";

    public static void addTypeOptionForStartOrStop(List<ScmNodeType> nodeTypes, Options ops,
            ScmHelpGenerator hp, boolean isRequire, boolean haveAllOpDesc)
            throws ScmToolsException {
        addTypeOptionForCreate(null, nodeTypes, ops, hp, isRequire, haveAllOpDesc, true);
    }

    public static void addTypeOptionForCreate(Map<String, ScmNodeRequiredParamGroup> nodeProperties,
            List<ScmNodeType> nodeTypes, Options ops, ScmHelpGenerator hp, boolean isRequire,
            boolean haveAllOpDesc, boolean withTypeNum) throws ScmToolsException {
        StringBuilder typeOptDesc = new StringBuilder();
        typeOptDesc.append("specify node type, arg:");
        // first 拼接 [ 1 | 2 | 3 | 21 | 20 ]
        StringBuilder first = new StringBuilder();
        // second 拼接 0:all, 1:service-center, 2:gateway, ...
        StringBuilder second = new StringBuilder();

        first.append("[");
        // all
        if (haveAllOpDesc) {
            if (withTypeNum) {
                first.append(" 0 |");
                second.append("0:all,").append(System.lineSeparator());
            }
            else {
                second.append("all, ").append(System.lineSeparator());
            }
        }
        // 拼接 nodeType
        for (ScmNodeType nodeType : nodeTypes) {
            first.append(String.format(" %s |", nodeType.getType()));
            if (withTypeNum) {
                second.append(String.format("%s:%s, ", nodeType.getType(), nodeType.getName()));
            }
            else {
                second.append(String.format("%s, ", nodeType.getName()));
            }
            if (nodeProperties != null && nodeProperties.size() > 0) {
                second.append("required properties:").append(System.lineSeparator());
                ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = nodeProperties.get(nodeType.getType());
                if (scmNodeRequiredParamGroup != null) {
                    for (String str : scmNodeRequiredParamGroup.getExample()) {
                        second.append("\t");
                        second.append(str);
                        second.append(System.lineSeparator());
                    }
                }
            }
            else {
                second.append(System.lineSeparator());
            }
        }

        // 去掉末尾的 "|"
        first.deleteCharAt(first.length() - 1);
        first.append("],").append(System.lineSeparator());

        // 合并
        if (withTypeNum) {
            typeOptDesc.append(first);
        }
        else {
            typeOptDesc.append(System.lineSeparator());
        }
        typeOptDesc.append(second);
        Option op = hp.createOpt(OPT_SHORT_NODE_TYPE, OPT_LONG_NODE_TYPE, typeOptDesc.toString(),
                isRequire, true, false);
        ops.addOption(op);
    }

    // type without typeNum
    public static void addTypeOptionForStartOrStopWithOutTypeNum(List<ScmNodeType> nodeTypes,
            Options ops, ScmHelpGenerator hp) throws ScmToolsException {
        addTypeOptionForCreate(null, nodeTypes, ops, hp, false, true, false);
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
            throw new ScmToolsException(e.getMessage(), ScmBaseExitCode.INVALID_ARG);
        }
    }

    public static String getPidCommandByjarName(String jarNamePrefix) {
        return "ps -eo pid,cmd | grep " + jarNamePrefix + " | grep -w -v grep | grep -w -v nohup";
    }

    public static int getPidFromPsResult(String psResult) {
        String trim = psResult.trim();
        String pidStr = trim.substring(0, trim.indexOf(" "));
        return Integer.valueOf(pidStr);
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

    public static ScmUserInfo checkAndGetUser(CommandLine cl, String userOption, String pwdOption,
            String pwdFileOption, boolean needRetypeInput, boolean pwdRequied)
            throws ScmToolsException {
        String username = cl.getOptionValue(userOption);
        if (cl.hasOption(pwdOption) && cl.hasOption(pwdFileOption)) {
            throw new ScmToolsException("do not specify --" + pwdOption + " and " + "--"
                    + pwdFileOption + " at the same time", ScmBaseExitCode.INVALID_ARG);
        }

        if (cl.hasOption(pwdOption)) {
            String pwd = cl.getOptionValue(pwdOption);
            if (pwd == null) {
                if (needRetypeInput) {
                    return new ScmUserInfo(username, readRetypeOptionValue(pwdOption));
                }
                return new ScmUserInfo(username, readOptionValue(pwdOption));
            }
            return new ScmUserInfo(username, pwd);
        }
        else if (cl.hasOption(pwdFileOption)) {
            String pwdFile = cl.getOptionValue(pwdFileOption);
            return new ScmUserInfo(username, parsePwdFile(username, pwdFile));
        }
        else {
            if (pwdRequied) {
                throw new ScmToolsException(
                        "please specify --" + pwdOption + " or " + "--" + pwdFileOption,
                        ScmBaseExitCode.INVALID_ARG);
            }
            return new ScmUserInfo(username, null);
        }
    }

    public static ScmUserInfo checkAndGetUser(CommandLine cl, String userOption, String pwdOption,
            String pwdFileOption) throws ScmToolsException {
        return checkAndGetUser(cl, userOption, pwdOption, pwdFileOption, false, true);
    }

    public static ScmUserInfo checkAndGetUser(CommandLine cl, String userOption, String pwdOption,
            boolean needRetypeInput) throws ScmToolsException {
        String username = cl.getOptionValue(userOption);
        String pwd = cl.getOptionValue(pwdOption);
        if (pwd == null) {
            if (needRetypeInput) {
                return new ScmUserInfo(username, readRetypeOptionValue(pwdOption));
            }
            return new ScmUserInfo(username, readOptionValue(pwdOption));

        }
        return new ScmUserInfo(username, pwd);
    }

    private static String parsePwdFile(String username, String pwdfile) throws ScmToolsException {
        AuthInfo auth = null;
        File file = new File(pwdfile);
        if (file.isAbsolute()) {
            auth = ScmFilePasswordParser.parserFile(pwdfile);
        }
        else {
            auth = ScmFilePasswordParser.parserFile(ScmCommon.getUserWorkingDir() + "/" + pwdfile);
        }
        if (!username.equals(auth.getUserName())) {
            throw new ScmToolsException("the specified username doesn't match with password file",
                    ScmBaseExitCode.INVALID_ARG);
        }
        return auth.getPassword();
    }

    private static String readRetypeOptionValue(String optionName) throws ScmToolsException {
        String password1 = readOptionValue(optionName);
        System.out.print("retype " + optionName + " value: ");
        String password2 = readPasswdFromStdIn();
        if (!password1.equals(password2)) {
            throw new ScmToolsException("passwords do not match", ScmBaseExitCode.INVALID_ARG);
        }
        return password1;
    }

    private static String readOptionValue(String optionName) throws ScmToolsException {
        System.out.print(optionName + " value: ");
        return readPasswdFromStdIn();
    }

    public static String readPasswdFromStdIn() {
        return new String(System.console().readPassword());
    }

}
