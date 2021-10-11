package com.sequoiacm.infrastructure.tool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.command.ScmHelpFullToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmHelpToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class CommandManager {
    private Logger logger = LoggerFactory.getLogger(CommandManager.class.getName());;
    private Map<String, ScmTool> tools;
    private String commandManagerName;

    // logger 自己定义就行
    public CommandManager(String commandManagerName) {
        this.commandManagerName = commandManagerName;
        initTool();
    }

    public void initTool() {
        tools = new HashMap<>();
        this.tools.put("help", new ScmHelpToolImpl(this));
        this.tools.put("helpfull", new ScmHelpFullToolImpl(this));
    }

    public void addTool(ScmTool tool) {
        this.tools.put(tool.getToolName(), tool);
    }

    public void execute(String[] args) {
        if (args.length > 0) {
            ScmTool tool = null;
            try {
                tool = getInstanceByToolName(args[0]);
            }
            catch (Exception e) {
                logger.error("create  " + args[0] + " subcommand instance failed", e);
                System.err.println("create " + args[0] + " subcommand instance failed:" + e.getMessage());
                if (e instanceof ScmToolsException) {
                    System.exit(((ScmToolsException)e).getExitCode());
                }
                System.exit(ScmBaseExitCode.SYSTEM_ERROR);
            }

            if (tool != null) {
                String[] toolsArgs = Arrays.copyOfRange(args, 1, args.length);
                try {
                    this.checkHelpArgs(args);
                    tool.process(toolsArgs);
                    System.exit(ScmBaseExitCode.SUCCESS);
                }
                catch (ScmToolsException e) {
                    if (e.getExitCode() != ScmBaseExitCode.EMPTY_OUT) {
                        logAndPrintErr(args[0], e);
                    }
                    System.exit(e.getExitCode());
                }
                catch (Exception e) {
                    logAndPrintErr(args[0], e);
                    System.exit(ScmBaseExitCode.SYSTEM_ERROR);
                }
            }
            else {
                if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))) {
                    try {
                        ScmCommon.printVersion();
                        System.exit(ScmBaseExitCode.SUCCESS);
                    }
                    catch (Exception e) {
                        logger.error("print version failed", e);
                        System.err.println("print version failed:" + e.getMessage());
                        if (e instanceof ScmToolsException) {
                            System.exit(((ScmToolsException)e).getExitCode());
                        }
                        System.exit(ScmBaseExitCode.SYSTEM_ERROR);
                    }
                }
                try {
                    checkHelpArgs(args);
                }
                catch (Exception e) {
                    logger.error("analyze args failed", e);
                    System.err.println("analyze args failed:" + e.getMessage());
                    if (e instanceof ScmToolsException) {
                        System.exit(((ScmToolsException)e).getExitCode());
                    }
                    System.exit(ScmBaseExitCode.SYSTEM_ERROR);
                }
                System.out.println("No such subcommand");
            }
        }
        System.out.println(this.getHelpMsg());
        System.exit(ScmBaseExitCode.INVALID_ARG);
    }

    private void logAndPrintErr(String arg, Exception e) {
        logger.error("process failed,subcommand:" + arg, e);
        System.err.println("process failed,subcommand:" + arg);
        if (e.getMessage() != null) {
            System.err.println("error message:" + e.getMessage());
        }
    }

    public void checkHelpArgs(String[] args) throws ScmToolsException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--help")) {
                if (i < args.length - 1) {
                    printHelp(args[i + 1], false);
                }
                else {
                    System.out.println(this.getHelpMsg());
                    System.exit(ScmBaseExitCode.SUCCESS);
                }
            }
        }
    }

    public void printHelp(String arg, boolean isFullHelp) throws ScmToolsException {
        ScmTool tool = getInstanceByToolName(arg);
        if (tool != null) {
            tool.printHelp(isFullHelp);
            System.exit(ScmBaseExitCode.SUCCESS);
        }
        else {
            System.out.println("No such command");
            System.out.println(this.getHelpMsg());
            System.exit(ScmBaseExitCode.INVALID_ARG);
        }
    }

    private ScmTool getInstanceByToolName(String toolName) throws ScmToolsException {
        ScmTool instance = null;
        ScmCommon.configToolsLog(ScmCommon.LOG_FILE_ADMIN);
        instance = this.tools.get(toolName);
        return instance;
    }

    public String getHelpMsg() {
        String template = "usage: name <subcommand> [options] [args]" + "\r\n"
                + "Type 'name help [subcommand]' for help on a specific subcommand" + "\r\n"
                + "Type 'name --version' to see the program version" + "\r\n"
                + "Available subcommands:" + "\r\n";
        template = template.replaceAll("name", this.commandManagerName);
        StringBuilder sb = new StringBuilder(template);
        Set<String> cmdNames = this.tools.keySet();
        for (String cmdName : cmdNames) {
            String s = String.format("\t%s" + "\r\n", cmdName);
            sb.append(s);
        }

        return sb.toString();
    }
}
