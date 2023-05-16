package com.sequoiacm.tools.tag.command;

import com.sequoiacm.tools.tag.common.UpgradeTagStatus;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.tag.common.SequoiadbDataSourceWrapper;
import com.sequoiacm.tools.tag.common.WorkspaceTagUpgrader;

public class ContinueUpgradeTagTool extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ContinueUpgradeTagTool.class);
    private static final String OPT_LONG_STATUS_FILE = "status-file";

    private final Options options;
    private final ScmHelpGenerator hp;

    public ContinueUpgradeTagTool() throws ScmToolsException {
        super("continueUpgradeTag");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt(null, OPT_LONG_STATUS_FILE, "workspaces", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, options);

        String statusFile = cl.getOptionValue(OPT_LONG_STATUS_FILE);
        UpgradeTagStatus upgradeStatus = UpgradeTagStatus.load(statusFile);

        SequoiadbDataSourceWrapper.getInstance().init(upgradeStatus.getSdbUrl(),
                upgradeStatus.getSdbUser(), upgradeStatus.getSdbPassword());
        try {
            WorkspaceTagUpgrader upgrader = new WorkspaceTagUpgrader(upgradeStatus);
            upgrader.doUpgrade();
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to upgrade workspace tag",
                    ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().destroy();
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
