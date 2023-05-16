package com.sequoiacm.tools.tag;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.tag.command.ContinueUpgradeTagTool;
import com.sequoiacm.tools.tag.command.UpgradeWorkspaceTagTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmTagUpgrade {
    private static final Logger logger = LoggerFactory.getLogger(ScmTagUpgrade.class);

    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("scm-tag-upgrade");
        try {
            cmd.addTool(new UpgradeWorkspaceTagTool());
            cmd.addTool(new ContinueUpgradeTagTool());
        }
        catch (ScmToolsException e) {
            logger.error("Error adding tools", e);
            System.exit(e.getExitCode());
        }
        cmd.execute(args, false);
    }
}
