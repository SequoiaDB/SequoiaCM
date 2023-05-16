package com.sequoiacm.config.tools.command;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.config.tools.exception.ScmExitCode;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmGlobalConfigDefine;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ScmUpdateGlobalConfigImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmUpdateGlobalConfigImpl.class);
    private final Options ops;
    private final ScmHelpGenerator hp;
    private final String OPT_LONG_URL = "url";
    private final String OPT_LONG_ADMIN_USER = "user";
    private final String OPT_LONG_ADMIN_PASSWD = "password";
    private final String OPT_LONG_CONFIG_NAME = "config-name";
    private final String OPT_LONG_CONFIG_VALUE = "config-value";

    public ScmUpdateGlobalConfigImpl() throws ScmToolsException {
        super("update-global-config");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, OPT_LONG_CONFIG_NAME,
                "config name. exam: " + ScmGlobalConfigDefine.TAG_LIB_DEFAULT_DOMAIN, true, true,
                false));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_CONFIG_VALUE, "config value.", true, true, false));

        ops.addOption(hp.createOpt(null, OPT_LONG_URL, "gateway url. exam: host1:8080", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ADMIN_USER, "login admin username.", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ADMIN_PASSWD, "login admin password.", false,
                true, true, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(OPT_LONG_ADMIN_USER);
        String urls = cl.getOptionValue(OPT_LONG_URL);
        String[] urlArray = urls.split(",");
        String passwd = cl.getOptionValue(OPT_LONG_ADMIN_PASSWD);
        if (passwd == null) {
            System.out.print("password: ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }

        String confName = cl.getOptionValue(OPT_LONG_CONFIG_NAME);
        String confValue = cl.getOptionValue(OPT_LONG_CONFIG_VALUE);
        ScmSession session = null;
        try {
            session = ScmFactory.Session
                    .createSession(new ScmConfigOption(Arrays.asList(urlArray), user, passwd));
            ScmSystem.Configuration.setGlobalConfig(session, confName, confValue);
            System.out.println("Update global config success: " + confName + "=" + confValue);
            logger.info("Update global config success: " + confName + "=" + confValue);
        }
        catch (Exception e) {
            throw new ScmToolsException("update global config failed, " + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            IOUtils.close(session);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
