package com.sequoiacm.tools;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.tools.command.ScmAlterWorkspaceToolImpl;
import com.sequoiacm.tools.command.ScmAttachRoleToolImpl;
import com.sequoiacm.tools.command.ScmCreateNodeToolImpl;
import com.sequoiacm.tools.command.ScmCreateRoleToolImpl;
import com.sequoiacm.tools.command.ScmCreateSiteToolImpl;
import com.sequoiacm.tools.command.ScmCreateUserToolImpl;
import com.sequoiacm.tools.command.ScmCreateWsToolImpl;
import com.sequoiacm.tools.command.ScmDeleteNodeToolImpl;
import com.sequoiacm.tools.command.ScmDeleteRoleToolImpl;
import com.sequoiacm.tools.command.ScmDeleteSiteToolImpl;
import com.sequoiacm.tools.command.ScmDeleteUserToolImpl;
import com.sequoiacm.tools.command.ScmDeleteWorkspaceToolImpl;
import com.sequoiacm.tools.command.ScmGrantRoleToolImpl;
import com.sequoiacm.tools.command.ScmHelpToolImpl;
import com.sequoiacm.tools.command.ScmListPrivilege;
import com.sequoiacm.tools.command.ScmListRoleImpl;
import com.sequoiacm.tools.command.ScmListSiteToolImpl;
import com.sequoiacm.tools.command.ScmListUserImpl;
import com.sequoiacm.tools.command.ScmListWorkspaceToolImpl;
import com.sequoiacm.tools.command.ScmPasswordEncryptor;
import com.sequoiacm.tools.command.ScmResetPassword;
import com.sequoiacm.tools.command.ScmRevokeRoleToolImpl;
import com.sequoiacm.tools.command.ScmTool;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;


public class ScmAdmin {
    private static Logger logger = LoggerFactory.getLogger(ScmAdmin.class.getName());
    public final static String helpMsg = "usage: scmadmin <subcommand> [options] [args]" + "\r\n"
            + "Type 'scmadmin help [subcommand]' for help on a specific subcommand" + "\r\n"
            + "Type 'scmadmin --version' to see the program version" + "\r\n"
            + "Available subcommands:" + "\r\n" + "\tcreatesite" + "\r\n" +"\tdeletesite" + "\r\n" + "\tlistsite" + "\r\n"
            + "\tcreatews" + "\r\n" + "\tdeletews" + "\r\n" + "\talterws" + "\r\n" + "\tlistws"
            + "\r\n" + "\tcreatenode" + "\r\n" + "\tdeletenode" + "\r\n" +"\tcreateuser" + "\r\n" + "\tdeleteuser" + "\r\n"
            + "\tlistuser" + "\r\n" + "\tattachrole" + "\r\n" + "\tcreaterole" + "\r\n"
            + "\tdeleterole" + "\r\n" + "\tlistrole" + "\r\n" + "\tgrantrole" + "\r\n"
            + "\trevokerole" + "\r\n" + "\tlistprivilege" + "\r\n" + "\tencrypt" + "\r\n"
            + "\tresetpassword" + "\r\n" + "\thelp";

    // deleteuser deleterole revokerole

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

        if (toolName.equals("createsite")) {
            // ScmCommon.setLogAndProperties(ScmCommon.CREATE_SITE_LOG_PATH,
            // ScmCommon.LOG_FILE_CREATESITE);
            instance = new ScmCreateSiteToolImpl();
        }
        else if (toolName.equals("deletesite")) {
            instance = new ScmDeleteSiteToolImpl();
        }
        else if (toolName.equals("listsite")) {
            instance = new ScmListSiteToolImpl();
        }
        else if (toolName.equals("help")) {
            instance = new ScmHelpToolImpl(ScmAdmin.class, false);
        }
        else if (toolName.equals("helpfull")) {
            instance = new ScmHelpToolImpl(ScmAdmin.class, true);
        }
        else if (toolName.equals("createws")) {
            // ScmCommon
            // .setLogAndProperties(ScmCommon.CREATE_WS_LOG_PATH,
            // ScmCommon.LOG_FILE_CREATEWS);
            instance = new ScmCreateWsToolImpl();
        }
        else if (toolName.equals("deletews")) {
            instance = new ScmDeleteWorkspaceToolImpl();
        }
        else if (toolName.equals("listws")) {
            instance = new ScmListWorkspaceToolImpl();
        }
        else if (toolName.equals("alterws")) {
            instance = new ScmAlterWorkspaceToolImpl();
        }
        else if (toolName.equals("createnode")) {
            instance = new ScmCreateNodeToolImpl();
        }
        else if (toolName.equals("deletenode")) {
            instance = new ScmDeleteNodeToolImpl();
        }
        else if (toolName.equals("createuser")) {
            instance = new ScmCreateUserToolImpl();
        }
        else if (toolName.equals("deleteuser")) {
            instance = new ScmDeleteUserToolImpl();
        }
        else if (toolName.equals("listuser")) {
            instance = new ScmListUserImpl();
        }
        else if (toolName.equals("createrole")) {
            instance = new ScmCreateRoleToolImpl();
        }
        else if (toolName.equals("deleterole")) {
            instance = new ScmDeleteRoleToolImpl();
        }
        else if (toolName.equals("listrole")) {
            instance = new ScmListRoleImpl();
        }
        else if (toolName.equals("attachrole")) {
            instance = new ScmAttachRoleToolImpl();
        }
        else if (toolName.equals("grantrole")) {
            instance = new ScmGrantRoleToolImpl();
        }
        else if (toolName.equals("revokerole")) {
            instance = new ScmRevokeRoleToolImpl();
        }
        else if (toolName.equals("listprivilege")) {
            instance = new ScmListPrivilege();
        }
        else if (toolName.equals("encrypt")) {
            instance = new ScmPasswordEncryptor();
        }
        else if (toolName.equals("resetpassword")) {
            instance = new ScmResetPassword();
        }
        return instance;
    }
}
