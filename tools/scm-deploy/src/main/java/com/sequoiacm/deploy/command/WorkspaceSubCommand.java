package com.sequoiacm.deploy.command;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.WorkspaceOperater;

@Command
public class WorkspaceSubCommand extends SubCommand {
    private static final Logger logger = LoggerFactory.getLogger(ClusterSubCommand.class);

    public static final String NAME = "workspace";
    public static final String OPT_CONF_PATH = "conf";
    public static final String OPT_CREATE = "create";
    public static final String OPT_DRYRUN = "dryrun";
    public static final String OPT_CLEAN = "clean";
    public static final String OPT_GRANT_USER_WITH_ALL_PRIV = "grant-all-priv";

    @Override
    protected boolean beforeProcess(CommandLine commandLine) {
        boolean isContinue = super.beforeProcess(commandLine);
        if (!isContinue) {
            return false;
        }
        if (!commandLine.hasOption(OPT_CONF_PATH)) {
            throw new IllegalArgumentException("missing required option: " + OPT_CONF_PATH);
        }
        if (!commandLine.hasOption(OPT_CLEAN) && !commandLine.hasOption(OPT_CREATE)
                && !commandLine.hasOption(OPT_GRANT_USER_WITH_ALL_PRIV)) {
            throw new IllegalArgumentException(
                    "please specify an required option: " + OPT_CREATE + " " + OPT_CLEAN);
        }
        return true;
    }

    @Override
    protected Options commandOptions() {
        Options ops = new Options();
        ops.addOption(Option.builder().longOpt(OPT_CLEAN).hasArg(false).required(false)
                .desc("clean all workspaces.").build());
        ops.addOption(Option.builder().longOpt(OPT_DRYRUN).hasArg(false).required(false)
                .desc("preview what command plans to do.").build());
        ops.addOption(
                Option.builder().longOpt(OPT_GRANT_USER_WITH_ALL_PRIV).hasArg(false).required(false)
                        .desc("grant workspace all-privilege to the workspace creator.").build());
        ops.addOption(Option.builder().longOpt(OPT_CREATE).hasArg(false).required(false)
                .desc("create workspace.").build());
        ops.addOption(Option.builder().longOpt(OPT_CONF_PATH).hasArg(true).required(false)
                .desc("the conf file of create workspace, sample:"
                        + new File(CommonConfig.getInstance().getWorkspaceConfigFilePath())
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
        CommonConfig.getInstance().setWorkspaceConfigFilePath(cl.getOptionValue(OPT_CONF_PATH));
        WorkspaceOperater wsOperater = new WorkspaceOperater();

        boolean dryrun = false;
        if (cl.hasOption(WorkspaceSubCommand.OPT_DRYRUN)) {
            dryrun = true;
        }

        if (cl.hasOption(WorkspaceSubCommand.OPT_CLEAN)) {
            wsOperater.cleanAll(dryrun);
        }

        if (cl.hasOption(WorkspaceSubCommand.OPT_CREATE)) {
            wsOperater.createWorkspace(dryrun);
        }

        if (cl.hasOption(WorkspaceSubCommand.OPT_GRANT_USER_WITH_ALL_PRIV)) {
            wsOperater.grantAllPriv(dryrun);
        }
    }

    @Override
    public String getDesc() {
        return "workspace operation";
    }

}
