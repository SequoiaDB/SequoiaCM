package com.sequoiacm.deploy.command;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmCleaner;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.core.ScmDeployer;
import com.sequoiacm.deploy.exception.DeployException;

@Command
public class ClusterSubCommand extends SubCommand {
    private static final Logger logger = LoggerFactory.getLogger(ClusterSubCommand.class);
    public static final String NAME = "cluster";

    public static final String OPT_CONF_PATH = "conf";
    public static final String OPT_DEPLOY = "deploy";
    public static final String OPT_CLEAN = "clean";
    public static final String OPT_DRYRUN = "dryrun";

    @Override
    protected boolean beforeProcess(CommandLine commandLine) {
        boolean isContinue = super.beforeProcess(commandLine);
        if (!isContinue) {
            return false;
        }
        if (!commandLine.hasOption(OPT_CONF_PATH)) {
            throw new IllegalArgumentException("missing required option: " + OPT_CONF_PATH);
        }

        if (!commandLine.hasOption(OPT_CLEAN) && !commandLine.hasOption(OPT_DEPLOY)) {
            throw new IllegalArgumentException(
                    "please specify an required option: " + OPT_DEPLOY + " " + OPT_CLEAN);
        }
        return true;
    }

    @Override
    protected Options commandOptions() {
        Options ops = new Options();
        ops.addOption(Option.builder().longOpt(OPT_CLEAN).hasArg(false).required(false)
                .desc("clean sequoiacm cluster.").build());
        ops.addOption(Option.builder().longOpt(OPT_DRYRUN).hasArg(false).required(false)
                .desc("preview what command plans to do.").build());
        ops.addOption(Option.builder().longOpt(OPT_DEPLOY).hasArg(false).required(false)
                .desc("deploy sequoiacm cluster.").build());
        ops.addOption(Option.builder().longOpt(OPT_CONF_PATH).hasArg(true).required(false)
                .desc("the conf file of deploy sequoiacm cluster, sample:"
                        + new File(CommonConfig.getInstance().getDeployConfigFilePath())
                                .getAbsolutePath())
                .build());
        return ops;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void process(CommandLine cl) throws Exception {
        String confFile = cl.getOptionValue(OPT_CONF_PATH);
        CommonConfig.getInstance().setDeployConfigFilePath(confFile);
        boolean dryrun = true;
        if (!cl.hasOption(ClusterSubCommand.OPT_DRYRUN)) {
            dryrun = false;
            ScmDeployInfoMgr.getInstance().check();
        }

        if (cl.hasOption(ClusterSubCommand.OPT_CLEAN)) {
            ScmCleaner cleaner = new ScmCleaner();
            cleaner.clean(dryrun);
        }

        if (cl.hasOption(ClusterSubCommand.OPT_DEPLOY)) {
            ScmDeployer deployer = new ScmDeployer();
            try {
                deployer.deploy(dryrun);
            }
            catch (Exception e) {
                throw new DeployException("use option '" + ClusterSubCommand.OPT_CLEAN
                        + "' to reset environment, and try again.", e.getMessage(), e);
            }
        }
    }

    @Override
    public String getDesc() {
        return "deploy sequoiacm cluster";
    }

}
