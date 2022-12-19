package com.sequoiacm.config.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sequoiacm.config.tools.common.ConfigType;
import com.sequoiacm.config.tools.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public abstract class AbstractScmRefreshConfig extends ScmTool {
    protected final String OPT_LONG_SERVICE = "service";
    protected final String OPT_LONG_NODE = "node";
    protected final String OPT_LONG_CONFIG = "config";

    protected final String OPT_LONG_URL = "url";
    protected final String OPT_LONG_ADMIN_USER = "user";
    protected final String OPT_LONG_ADMIN_PASSWD = "password";
    protected final String OPT_LONG_ADMIN_PASSWD_FILE = "password-file";
    protected final String OPT_LONG_ACCEPT_UNKNOWN_CONF = "accept-unknown-conf";

    protected String name;
    protected Integer type;
    protected String config;
    protected String gatewayUrl;
    protected String username;
    protected String password;

    protected Options ops;
    protected ScmHelpGenerator hp;
    protected boolean acceptUnknownConfig;

    public AbstractScmRefreshConfig(String tooName) throws ScmToolsException {
        super(tooName);
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, OPT_LONG_SERVICE, "service name.", false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_NODE, "node name.", false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_CONFIG,
                "config item, exam: " + configOptionExample(), true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ACCEPT_UNKNOWN_CONF,
                "accept unknown configuration.", false, false, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_URL,
                "gateway url. exam: host1:8080,host2:8080,host3:8080", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ADMIN_USER, "login admin username.", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ADMIN_PASSWD, "login admin password.", false,
                true, true, false, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ADMIN_PASSWD_FILE, "login admin password file.",
                false, true, false));
    }

    public void parseCommandParam(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        if (cl.hasOption(OPT_LONG_SERVICE)) {
            if (cl.hasOption(OPT_LONG_NODE)) {
                throw new ScmToolsException(
                        "invalid argument, only one service name and node name can be filled in",
                        ScmExitCode.INVALID_ARG);
            }
            else {
                this.type = ConfigType.BY_SERVICE.getType();
                this.name = cl.getOptionValue(OPT_LONG_SERVICE);
            }
        }
        else {
            if (!cl.hasOption(OPT_LONG_NODE)) {
                throw new ScmToolsException(
                        "invalid argument, service name or node name can not be empty",
                        ScmExitCode.INVALID_ARG);
            }
            else {
                this.type = ConfigType.BY_NODE.getType();
                this.name = cl.getOptionValue(OPT_LONG_NODE);
            }
        }
        this.config = cl.getOptionValue(OPT_LONG_CONFIG);
        this.gatewayUrl = cl.getOptionValue(OPT_LONG_URL);
        ScmUserInfo adminUser = ScmCommandUtil.checkAndGetUser(cl, OPT_LONG_ADMIN_USER,
                OPT_LONG_ADMIN_PASSWD, OPT_LONG_ADMIN_PASSWD_FILE);
        this.username = adminUser.getUsername();
        this.password = adminUser.getPassword();
        this.acceptUnknownConfig = cl.hasOption(OPT_LONG_ACCEPT_UNKNOWN_CONF);
    }

    public abstract void operation() throws ScmToolsException;

    protected abstract String configOptionExample();

    @Override
    public void process(String[] args) throws ScmToolsException {
        parseCommandParam(args);
        operation();
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
