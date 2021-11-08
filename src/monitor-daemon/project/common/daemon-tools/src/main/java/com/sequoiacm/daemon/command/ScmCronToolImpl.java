package com.sequoiacm.daemon.command;

import com.sequoiacm.daemon.common.ArgsUtils;
import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.lock.ScmFileResource;
import com.sequoiacm.daemon.lock.ScmFileResourceFactory;
import com.sequoiacm.daemon.manager.ScmManagerWrapper;
import com.sequoiacm.daemon.lock.ScmFileLock;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.*;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ScmCronToolImpl extends ScmTool {

    private final static Logger logger = LoggerFactory.getLogger(ScmCronToolImpl.class);

    private Options options;
    private ScmHelpGenerator hp;
    private ScmManagerWrapper executor;

    public ScmCronToolImpl() throws ScmToolsException {
        super("cron");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt(DaemonDefine.OPT_SHORT_PERIOD, DaemonDefine.OPT_LONG_PERIOD,
                "monitor period.", true, true, false));
        executor = ScmManagerWrapper.getInstance();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmHelper.configToolsLog(DaemonDefine.CRON_LOG_CONF);

        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);

        String periodStr = commandLine.getOptionValue(DaemonDefine.OPT_SHORT_PERIOD);
        int period = ArgsUtils.convertAndCheckPeriod(periodStr);

        String jarPath = CommonUtils.getJarPath(ScmCronToolImpl.class);
        File file = new File(jarPath);
        ScmFileResource resource = ScmFileResourceFactory.getInstance().createFileResource(file);
        ScmFileLock lock = resource.createLock();
        if (lock.tryLock()) {
            try {
                String daemonHomePath = jarPath.substring(0,
                        jarPath.indexOf(File.separator + DaemonDefine.JARS));
                executor.startTimer(period, daemonHomePath);
            }
            finally {
                lock.unlock();
                resource.releaseFileResource();
            }
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
