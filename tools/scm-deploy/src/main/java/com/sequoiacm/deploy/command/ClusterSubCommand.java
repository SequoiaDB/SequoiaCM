package com.sequoiacm.deploy.command;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmCleaner;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.core.ScmDeployer;
import com.sequoiacm.deploy.core.ScmRollbacker;
import com.sequoiacm.deploy.core.ScmUpgradeInfoMgr;
import com.sequoiacm.deploy.core.ScmUpgradeStatusInfoMgr;
import com.sequoiacm.deploy.core.ScmUpgrader;
import com.sequoiacm.deploy.exception.DeployException;

@Command
public class ClusterSubCommand extends SubCommand {
    private static final Logger logger = LoggerFactory.getLogger(ClusterSubCommand.class);
    public static final String NAME = "cluster";
    public static final String OPT_DEPLOY = "deploy";
    public static final String OPT_CLEAN = "clean";
    public static final String OPT_CONF_PATH = "conf";
    public static final String OPT_UPGRADE = "upgrade";
    public static final String OPT_UPGRADE_CONF = "upgrade-conf";
    public static final String OPT_ROLLBACK = "rollback";
    public static final String OPT_UPGRADE_STATUS = "upgrade-status-path";
    public static final String OPT_DRYRUN = "dryrun";


    @Override
    protected boolean beforeProcess(CommandLine commandLine) {
        boolean isContinue = super.beforeProcess(commandLine);
        if (!isContinue) {
            return false;
        }
        boolean noExecuteOption = false;
        if (commandLine.hasOption(OPT_DEPLOY)) {
            noExecuteOption = true;
            if (!commandLine.hasOption(OPT_CONF_PATH)) {
                throw new IllegalArgumentException(
                        "deploy missing required option: " + OPT_CONF_PATH);
            }
            if (commandLine.hasOption(OPT_UPGRADE) || commandLine.hasOption(OPT_ROLLBACK)) {
                throw new IllegalArgumentException(
                        "deploy cannot be execute with upgrade or rollback at the same time");
            }
        }
        if (commandLine.hasOption(OPT_CLEAN)) {
            noExecuteOption = true;
            if (!commandLine.hasOption(OPT_CONF_PATH)) {
                throw new IllegalArgumentException(
                        "clean missing required option: " + OPT_CONF_PATH);
            }
            if (commandLine.hasOption(OPT_UPGRADE) || commandLine.hasOption(OPT_ROLLBACK)) {
                throw new IllegalArgumentException(
                        "clean cannot be execute with upgrade or rollback at the same time");
            }
        }
        if (commandLine.hasOption(OPT_UPGRADE)) {
            noExecuteOption = true;
            if (!commandLine.hasOption(OPT_UPGRADE_CONF)) {
                throw new IllegalArgumentException(
                        "upgrade missing required option: " + OPT_UPGRADE_CONF);
            }
            if (commandLine.hasOption(OPT_ROLLBACK)) {
                throw new IllegalArgumentException(
                        "upgrade cannot be execute with rollback at the same time");
            }
        }
        if (commandLine.hasOption(OPT_ROLLBACK)) {
            noExecuteOption = true;
            if (!commandLine.hasOption(OPT_UPGRADE_STATUS)) {
                throw new IllegalArgumentException(
                        "rollback missing required option: " + OPT_UPGRADE_STATUS);
            }
        }
        if (!noExecuteOption) {
            throw new IllegalArgumentException("please specify an required option: " + OPT_DEPLOY
                    + " " + OPT_CLEAN + " " + OPT_UPGRADE + " " + OPT_ROLLBACK);
        }
        for (SubOption subOption : SubOption.values()) {
            if (!commandLine.hasOption(subOption.getName())) {
                break;
            }
            boolean hasOption = false;
            List<String> belongOptions = subOption.getBelongOptions();
            for (String option : belongOptions) {
                if (commandLine.hasOption(option)) {
                    hasOption = true;
                    break;
                }
            }
            if (!hasOption) {
                throw new IllegalArgumentException(" --" + subOption.getName()
                        + " missing required option: " + subOption.getOneOfBelongOptionsStr());
            }
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
        ops.addOption(Option.builder().longOpt(OPT_UPGRADE).hasArg(false).required(false)
                .desc("upgrade sequoiacm cluster").build());
        ops.addOption(Option.builder().longOpt(OPT_UPGRADE_CONF).hasArg(true).required(false)
                .desc("the conf file of upgrade sequoiacm cluster, sample:"
                        + new File(CommonConfig.getInstance().getUpgradeConfigFilePath())
                        .getAbsolutePath())
                .build());
        ops.addOption(Option.builder().longOpt(OPT_ROLLBACK).hasArg(false).required(false)
                .desc("rollback sequoiacm cluster").build());
        ops.addOption(Option.builder().longOpt(OPT_UPGRADE_STATUS).hasArg(true).required(false)
                .desc("the upgrade status file of rollback sequoiacm cluster, sample:"
                        + new File(CommonConfig.getInstance().getUpgradeStatusDirPath())
                        .getAbsolutePath()
                        + "/upgrade_status_{timestamp}")
                .build());
        for (SubOption subOption : SubOption.values()) {
            ops.addOption(
                    Option.builder().longOpt(subOption.getName()).hasArg(subOption.isHasArgs())
                            .required(subOption.isRequire()).desc(subOption.getDesc()).build());
        }
        return ops;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void process(CommandLine cl) throws Exception {
        boolean dryrun = true;
        if (!cl.hasOption(ClusterSubCommand.OPT_DRYRUN)) {
            dryrun = false;
        }
        // for (SubOption subOption : SubOption.values()) {
        //
        // }
        // if (cl.hasOption(ClusterSubCommand.OPT_UPGRADE)
        // || cl.hasOption(ClusterSubCommand.OPT_ROLLBACK)) {
        // if (cl.hasOption(OPT_HOST)) {
        // String commandLineHostNames = cl.getOptionValue(OPT_HOST);
        // CommonConfig.getInstance().setCommandLineHostNames(commandLineHostNames);
        // }
        // if (cl.hasOption(OPT_SERVICE)) {
        // String commandLineServiceNames = cl.getOptionValue(OPT_SERVICE);
        // CommonConfig.getInstance().setCommandLineServiceNames(commandLineServiceNames);
        // }
        // if (cl.hasOption(OPT_UNATTENDED)) {
        // CommonConfig.getInstance().setCommandLineUnattended(true);
        // }
        // }
        for (String belongOption : SubOption.getAllBelongOptions()) {
            List<SubOption> subOptions = SubOption.getSubOption(belongOption);
            for (SubOption subOption : subOptions) {
                if (cl.hasOption(subOption.getName())) {
                    String optionValue = cl.getOptionValue(subOption.getName());
                    if (optionValue == null) {
                        optionValue = "";
                    }
                    CommonConfig.getInstance().setSubOptionValue(subOption, optionValue);
                }
            }
        }

        if (cl.hasOption(ClusterSubCommand.OPT_CLEAN)
                || cl.hasOption(ClusterSubCommand.OPT_DEPLOY)) {
            String confFile = cl.getOptionValue(OPT_CONF_PATH);
            CommonConfig.getInstance().setDeployConfigFilePath(confFile);
            ScmDeployInfoMgr.getInstance().check();
        }

        if (cl.hasOption(ClusterSubCommand.OPT_UPGRADE)) {
            String upgradeConfFile = cl.getOptionValue(OPT_UPGRADE_CONF);
            CommonConfig.getInstance().setUpgradeConfigFilePath(upgradeConfFile);
            ScmUpgradeInfoMgr.getInstance().check();
        }
        if (cl.hasOption(ClusterSubCommand.OPT_ROLLBACK)) {
            String upgradeStatusFilePath = cl.getOptionValue(OPT_UPGRADE_STATUS);
            CommonConfig.getInstance().setUpgradeStatusFilePath(upgradeStatusFilePath);
            ScmUpgradeStatusInfoMgr.getInstance().check();
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

        if (cl.hasOption(ClusterSubCommand.OPT_UPGRADE)) {
            ScmUpgrader upgrader = new ScmUpgrader();
            upgrader.upgrade(dryrun);
        }

        if (cl.hasOption(ClusterSubCommand.OPT_ROLLBACK)) {
            ScmRollbacker rollbacker = new ScmRollbacker();
            rollbacker.rollback(dryrun);
        }

    }

    @Override
    public String getDesc() {
        return "deploy sequoiacm cluster";
    }
}
