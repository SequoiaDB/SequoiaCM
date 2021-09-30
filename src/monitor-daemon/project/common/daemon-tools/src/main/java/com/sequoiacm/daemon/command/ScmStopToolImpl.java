package com.sequoiacm.daemon.command;

import com.sequoiacm.daemon.manager.ScmManagerWrapper;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmStopToolImpl extends ScmTool {

    private static final Logger logger = LoggerFactory.getLogger(ScmStopToolImpl.class);

    private ScmManagerWrapper executor;
    private ScmHelpGenerator hp;
    private Options options;

    public ScmStopToolImpl() throws ScmToolsException {
        super("stop");
        hp = new ScmHelpGenerator();
        options = new Options();
        executor = ScmManagerWrapper.getInstance();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmCommandUtil.parseArgs(args, options);

        logger.info("Stopping...");
        System.out.println("Stopping...");
        executor.stopDaemon();
        logger.info("Stop daemon success");
        System.out.println("Stop daemon success");
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
