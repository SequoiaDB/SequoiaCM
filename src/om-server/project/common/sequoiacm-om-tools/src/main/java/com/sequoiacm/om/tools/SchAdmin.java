package com.sequoiacm.om.tools;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.om.tools.command.ScmCreateNodeToolImpl;
import com.sequoiacm.om.tools.command.ScmTool;
import com.sequoiacm.om.tools.common.ScmCommon;
import com.sequoiacm.om.tools.exception.ScmExitCode;
import com.sequoiacm.om.tools.exception.ScmToolsException;

public class SchAdmin {
    private static Logger logger = LoggerFactory.getLogger(SchAdmin.class.getName());
    public final static String helpMsg = "usage: scmadmin <subcommand> [options] [args]" + "\r\n"
            + "Type 'schadmin help [subcommand]' for help on a specific subcommand" + "\r\n"
            + "Type 'schadmin --version' to see the program version" + "\r\n"
            + "Available subcommands:" + "\r\n" + "\tcreatenode" + "\r\n" + "\tsubscribe" + "\r\n"
            + "\tunsubscribe" + "\r\n" + "\tlistsubscribers" + "\r\n" + "\thelp";

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
                System.err
                        .println("create  " + args[0] + " subcommand instance failed,stack trace:");
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
                System.out.println("No such subcommand");
            }
        }
        System.out.println(helpMsg);
        System.exit(ScmExitCode.INVALID_ARG);
    }

    public static void checkHelpArgs(String[] args) throws ScmToolsException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--help")) {
                if (i < args.length - 1) {
                    printHelp(args[i + 1], false);
                }
                else {
                    System.out.println(helpMsg);
                    System.exit(ScmExitCode.SUCCESS);
                }
            }
        }
    }

    public static void printHelp(String arg, boolean isFullHelp) throws ScmToolsException {
        ScmTool tool = getInstanceByToolName(arg);
        if (tool != null) {
            tool.printHelp(isFullHelp);
            System.exit(ScmExitCode.SUCCESS);
        }
        else {
            System.out.println("No such command");
            System.out.println(helpMsg);
            System.exit(ScmExitCode.INVALID_ARG);
        }
    }

    private static ScmTool getInstanceByToolName(String toolName) throws ScmToolsException {
        ScmTool instance = null;
        if (toolName.equals("createnode")) {
            instance = new ScmCreateNodeToolImpl();
        }
        else {

        }
        return instance;
    }
}
