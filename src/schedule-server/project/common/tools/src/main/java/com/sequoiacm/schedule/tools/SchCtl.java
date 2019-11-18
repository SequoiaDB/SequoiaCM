package com.sequoiacm.schedule.tools;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.tools.command.ScmHelpToolImpl;
import com.sequoiacm.schedule.tools.command.ScmListToolImpl;
import com.sequoiacm.schedule.tools.command.ScmStartToolImpl;
import com.sequoiacm.schedule.tools.command.ScmStopToolImpl;
import com.sequoiacm.schedule.tools.command.ScmTool;
import com.sequoiacm.schedule.tools.common.ScmCommon;
import com.sequoiacm.schedule.tools.common.ScmHelper;
import com.sequoiacm.schedule.tools.common.ScmToolsDefine;
import com.sequoiacm.schedule.tools.exception.ScmExitCode;
import com.sequoiacm.schedule.tools.exception.ScmToolsException;

public class SchCtl {
    private static Logger logger = LoggerFactory.getLogger(SchCtl.class.getName());
    public static String helpMsg = "usage: scmctl <subcommand> [options] [args]" + "\r\n"
            + "Type 'schctl help [subcommand]' for help on a specific subcommand" + "\r\n"
            + "Type 'schctl --version' to see the program version" + "\r\n" + "\r\n"
            + "Available subcommands:" + "\r\n" + "\tstart" + "\r\n" + "\tstop" + "\r\n" + "\tlist"
            + "\r\n" + "\thelp";

    public static void main(String[] args) {
        if (args.length > 0) {
            ScmTool tool = null;
            try {
                tool = getInstanceByToolName(args[0]);
            }
            catch (ScmToolsException e) {
                e.printErrorMsg();
                System.exit(e.getExitCode());
            }
            catch (Exception e) {
                logger.error("create  " + args[0] + " subcommand instance failed", e);
                System.err.println("create  " + args[0]
                        + " subcommand instance failed,stack trace:");
                e.printStackTrace();
                System.exit(ScmExitCode.SYSTEM_ERROR);
            }

            if (tool != null) {
                String[] toolsArgs = Arrays.copyOfRange(args, 1, args.length);
                try {
                    tool.process(toolsArgs);
                    System.exit(ScmExitCode.SUCCESS);
                }
                catch (ScmToolsException e) {
                    e.printErrorMsg();
                    System.exit(e.getExitCode());
                }
                catch (Exception e) {
                    logger.error("process failed,subcommand:" + args[0], e);
                    System.err.println("process failed,subcommand:" + args[0] + ",stack trace:");
                    e.printStackTrace();
                    System.exit(ScmExitCode.SYSTEM_ERROR);
                }
            }
            else {
                if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))) {
                    try {
                        ScmCommon.printVersion();
                        System.exit(ScmExitCode.SUCCESS);
                    }
                    catch (ScmToolsException e) {
                        e.printErrorMsg();
                        System.exit(e.getExitCode());
                    }
                    catch (Exception e) {
                        logger.error("print version failed", e);
                        System.err.println("print version failed,stack trace:");
                        e.printStackTrace();
                        System.exit(ScmExitCode.SYSTEM_ERROR);
                    }

                }
                try {
                    checkHelpArgs(args);
                }
                catch (ScmToolsException e) {
                    e.printErrorMsg();
                    System.exit(e.getExitCode());
                }
                catch (Exception e) {
                    logger.error("analyze args failed", e);
                    System.err.println("analyze args failed,stack trace:");
                    e.printStackTrace();
                    System.exit(ScmExitCode.SYSTEM_ERROR);
                }
                System.out.println("No such command");
            }
        }
        System.out.println(helpMsg);
        System.exit(ScmExitCode.INVALID_ARG);
    }

    public static void printHelp(String helpArgs, boolean isFullHelp) throws ScmToolsException {
        ScmTool tool = getInstanceByToolName(helpArgs);
        if (tool != null) {
            tool.printHelp(isFullHelp);
            System.exit(ScmExitCode.SUCCESS);
        }
        else {
            System.out.println("No such subcommand");
            System.out.println(helpMsg);
            System.exit(ScmExitCode.INVALID_ARG);
        }
    }

    public static void checkHelpArgs(String[] args) throws ScmToolsException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--help")) {
                if (i < args.length - 1) {
                    printHelp(args[i + 1], false);
                    System.exit(ScmExitCode.SUCCESS);
                }
                else {
                    System.out.println(helpMsg);
                    System.exit(ScmExitCode.SUCCESS);
                }
            }
        }
    }

    private static ScmTool getInstanceByToolName(String toolName) throws ScmToolsException {
        ScmTool instance = null;
        if (toolName.equals("start")) {
            ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.START_LOG_CONF);
            instance = new ScmStartToolImpl();
        }
        else if (toolName.equals("stop")) {
            ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.STOP_LOG_CONF);
            instance = new ScmStopToolImpl();
        }
        else if (toolName.equals("list")) {
            instance = new ScmListToolImpl();
        }
        else if (toolName.equals("help")) {
            instance = new ScmHelpToolImpl(SchCtl.class, false);
        }
        else if (toolName.equals("helpfull")) {
            instance = new ScmHelpToolImpl(SchCtl.class, true);
        }
        else {
            // TODO:
        }
        return instance;
    }

}
