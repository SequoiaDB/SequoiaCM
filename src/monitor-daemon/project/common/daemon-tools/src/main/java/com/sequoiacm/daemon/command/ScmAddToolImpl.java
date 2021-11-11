package com.sequoiacm.daemon.command;

import com.sequoiacm.daemon.common.ArgsUtils;
import com.sequoiacm.daemon.common.CommandUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.manager.ScmManagerWrapper;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ScmAddToolImpl extends ScmTool {
    private final Logger logger = LoggerFactory.getLogger(ScmAddToolImpl.class);

    private ScmManagerWrapper executor;
    private ScmHelpGenerator hp;
    private Options options;

    public ScmAddToolImpl() throws ScmToolsException {
        super("add");
        options = new Options();
        hp = new ScmHelpGenerator();
        CommandUtils.addTypeOptionForAdd(options, hp, true);
        options.addOption(hp.createOpt(DaemonDefine.OPT_SHORT_STATUS, DaemonDefine.OPT_LONG_STATUS,
                "node monitor status.", false, true, false));
        options.addOption(hp.createOpt(DaemonDefine.OPT_SHORT_CONF, DaemonDefine.OPT_LONG_CONF,
                "node conf path.", true, true, false));
        options.addOption(hp.createOpt(DaemonDefine.OPT_SHORT_OVERWRITE,
                DaemonDefine.OPT_LONG_OVERWRITE, "overwrite node info.", false, false, true));
        executor = ScmManagerWrapper.getInstance();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);

        String status = DaemonDefine.NODE_STATUS_ON;
        if (commandLine.hasOption(DaemonDefine.OPT_SHORT_STATUS)) {
            status = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_STATUS);
            ArgsUtils.checkStatusValid(status);
        }

        String confPath = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_CONF);
        ArgsUtils.checkPathExist(confPath);
        confPath = ArgsUtils.convertPathToAbsolute(confPath);

        String type = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_TYPE);
        ScmServerScriptEnum scmServerEnum = ScmServerScriptEnum.getEnumByType(type);
        ArgsUtils.checkTypeValid(scmServerEnum, confPath);

        boolean isOverWrite = commandLine.hasOption(DaemonDefine.OPT_SHORT_OVERWRITE);

        ScmNodeInfo nodeInfo = new ScmNodeInfo();
        nodeInfo.setStatus(status);
        nodeInfo.setConfPath(confPath);
        nodeInfo.setServerType(scmServerEnum);
        try {
            executor.addNodeInfo(nodeInfo, isOverWrite);
        }
        catch (ScmToolsException e) {
            File file = new File(DaemonDefine.SCMD_LOG_PATH).getAbsoluteFile();
            throw new ScmToolsException("Failed to add node,node:" + nodeInfo.toString()
                    + ",please check log:" + file.getAbsolutePath(), e.getExitCode(), e);
        }
        catch (Exception e) {
            File file = new File(DaemonDefine.SCMD_LOG_PATH).getAbsoluteFile();
            throw new ScmToolsException("Failed to add node,node:" + nodeInfo.toString()
                    + ",please check log:" + file.getAbsolutePath(), ScmExitCode.SYSTEM_ERROR, e);
        }
        logger.info("Add node success,node:{}", nodeInfo.toString());
        System.out.println("Add node success,node:" + nodeInfo.toString());
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
