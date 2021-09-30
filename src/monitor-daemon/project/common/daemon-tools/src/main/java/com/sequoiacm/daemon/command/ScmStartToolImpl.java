package com.sequoiacm.daemon.command;

import com.sequoiacm.daemon.common.ArgsUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.manager.ScmManagerWrapper;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmStartToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmStartToolImpl.class);

    private ScmHelpGenerator hp;
    private Options options;
    private int periodTime;
    private ScmManagerWrapper executor;

    public ScmStartToolImpl() throws ScmToolsException {
        super("start");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt(DaemonDefine.OPT_SHORT_PERIOD, DaemonDefine.OPT_LONG_PERIOD,
                "daemon period", false, true, false));
        executor = ScmManagerWrapper.getInstance();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);

        if (commandLine.hasOption(DaemonDefine.OPT_SHORT_PERIOD)) {
            String periodStr = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_PERIOD);
            this.periodTime = ArgsUtils.convertAndCheckPeriod(periodStr);
        }
        else {
            this.periodTime = DaemonDefine.PERIOD;
        }
        logger.info("Starting...");
        System.out.println("Starting...");
        executor.startDaemon(this.periodTime);
        logger.info("Start daemon success");
        System.out.println("Start daemon success");
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
