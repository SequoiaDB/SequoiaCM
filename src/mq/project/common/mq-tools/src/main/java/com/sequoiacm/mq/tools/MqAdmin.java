package com.sequoiacm.mq.tools;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.mq.tools.command.ScmCreateGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmCreateNodeToolImpl;
import com.sequoiacm.mq.tools.command.ScmCreateTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmDeleteGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmDeleteTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmHelpToolImpl;
import com.sequoiacm.mq.tools.command.ScmListGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmListTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmTool;
import com.sequoiacm.mq.tools.command.ScmUpdateTopicToolImpl;
import com.sequoiacm.mq.tools.common.ScmCommon;
import com.sequoiacm.mq.tools.exception.ScmExitCode;
import com.sequoiacm.mq.tools.exception.ScmToolsException;

public class MqAdmin {
    private static Logger logger = LoggerFactory.getLogger(MqAdmin.class.getName());
    public final static String helpMsg = "usage: mqadmin <subcommand> [options] [args]" + "\r\n"
            + "Type 'mqadmin help [subcommand]' for help on a specific subcommand" + "\r\n"
            + "Type 'mqadmin --version' to see the program version" + "\r\n"
            + "Available subcommands:" + "\r\n" + "\tcreatenode" + "\r\n" + "\tcreatetopic" + "\r\n"
            + "\tcreategroup" + "\r\n" + "\tdeletetopic" + "\r\n" + "\tdeletegroup" + "\r\n"
            + "\tlisttopic" + "\r\n" + "\tlistgroup" + "\r\n" + "\tupdatetopic" + "\r\n" + "\thelp";

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
        ScmCommon.setLogAndProperties(ScmCommon.SCM_ADMIN_LOG_PATH, ScmCommon.LOG_FILE_ADMIN);
        if (toolName.equals("createnode")) {
            instance = new ScmCreateNodeToolImpl();
        }
        else if (toolName.equals("help")) {
            instance = new ScmHelpToolImpl(MqAdmin.class, false);
        }
        else if (toolName.equals("helpfull")) {
            instance = new ScmHelpToolImpl(MqAdmin.class, true);
        }
        else if (toolName.equals("createtopic")) {
            instance = new ScmCreateTopicToolImpl();
        }
        else if (toolName.equals("creategroup")) {
            instance = new ScmCreateGroupToolImpl();
        }
        else if (toolName.equals("deletetopic")) {
            instance = new ScmDeleteTopicToolImpl();
        }
        else if (toolName.equals("deletegroup")) {
            instance = new ScmDeleteGroupToolImpl();
        }
        else if (toolName.equals("listtopic")) {
            instance = new ScmListTopicToolImpl();
        }
        else if (toolName.equals("listgroup")) {
            instance = new ScmListGroupToolImpl();
        }
        else if (toolName.equals("updatetopic")) {
            instance = new ScmUpdateTopicToolImpl();
        }
        return instance;
    }
}
