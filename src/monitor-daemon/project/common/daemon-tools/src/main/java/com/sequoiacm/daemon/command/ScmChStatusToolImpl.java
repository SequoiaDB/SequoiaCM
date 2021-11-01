package com.sequoiacm.daemon.command;

import com.sequoiacm.daemon.common.ArgsUtils;
import com.sequoiacm.daemon.common.CommandUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.*;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.manager.ScmManagerWrapper;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmChStatusToolImpl extends ScmTool {

    private static final Logger logger = LoggerFactory.getLogger(ScmChStatusToolImpl.class);

    private ScmManagerWrapper executor;
    private ScmHelpGenerator hp;
    private Options options;

    public ScmChStatusToolImpl() throws ScmToolsException {
        super("chstatus");
        hp = new ScmHelpGenerator();
        options = new Options();
        options.addOption(hp.createOpt(DaemonDefine.OPT_SHORT_PORT, DaemonDefine.OPT_LONG_PORT,
                "node port.", false, true, false));
        CommandUtils.addTypeOptionForChStatus(options, hp, false);
        options.addOption(hp.createOpt(DaemonDefine.OPT_SHORT_STATUS, DaemonDefine.OPT_LONG_STATUS,
                "node status.", true, true, false));

        executor = ScmManagerWrapper.getInstance();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(DaemonDefine.OPT_SHORT_TYPE)
                && commandLine.hasOption(DaemonDefine.OPT_SHORT_PORT)
                || !commandLine.hasOption(DaemonDefine.OPT_SHORT_TYPE)
                        && !commandLine.hasOption(DaemonDefine.OPT_SHORT_PORT)) {
            throw new ScmToolsException("please set -" + DaemonDefine.OPT_SHORT_TYPE + " or -"
                    + DaemonDefine.OPT_SHORT_PORT, ScmExitCode.INVALID_ARG);
        }

        String status = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_STATUS);
        ArgsUtils.checkStatusValid(status);
        ScmNodeModifier nodeModifier = new ScmNodeModifier(status);

        ScmNodeMatcher nodeMatcher = null;
        if (commandLine.hasOption(DaemonDefine.OPT_SHORT_PORT)) {
            String portStr = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_PORT);
            int port = ScmCommon.convertStrToInt(portStr);
            ArgsUtils.checkPortValid(port);
            nodeMatcher = new ScmNodeMatcher(port);
        }
        else {
            String type = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_TYPE);
            if (type.equalsIgnoreCase("all")) {
                nodeMatcher = new ScmNodeMatcher();
            }
            else {
                ScmServerScriptEnum serverScript = ScmServerScriptEnum.getEnumByType(type);
                if (serverScript == null) {
                    throw new ScmToolsException("Type isn't exist,type: " + type,
                            ScmExitCode.INVALID_ARG);
                }
                nodeMatcher = new ScmNodeMatcher(serverScript.getType());
            }
        }

        try {
            executor.changeNodeStatus(nodeMatcher, nodeModifier);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to change node status,nodeMatcher:" + nodeMatcher.toString(),
                    e.getExitCode(), e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to change node status,nodeMatcher:" + nodeMatcher.toString(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        logger.info("Change node status success,nodeMatcher:{}", nodeMatcher.toString());
        System.out.println("Change node status success,nodeMatcher:" + nodeMatcher.toString());
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
