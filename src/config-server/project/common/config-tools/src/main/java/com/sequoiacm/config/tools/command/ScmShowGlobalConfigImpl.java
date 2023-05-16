package com.sequoiacm.config.tools.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ScmShowGlobalConfigImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmShowGlobalConfigImpl.class);
    private final Options ops;
    private final ScmHelpGenerator hp;
    private final String OPT_LONG_URL = "url";
    private final String OPT_LONG_ADMIN_USER = "user";
    private final String OPT_LONG_ADMIN_PASSWD = "password";
    private final String OPT_LONG_CONFIG_NAME = "config-name";

    public ScmShowGlobalConfigImpl() throws ScmToolsException {
        super("show-global-config");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, OPT_LONG_CONFIG_NAME,
                "config name. exam: " + ScmGlobalConfigDefine.TAG_LIB_DEFAULT_DOMAIN, false, true,
                false));
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
        ScmSession session = null;
        try {
            session = ScmFactory.Session
                    .createSession(new ScmConfigOption(Arrays.asList(urlArray), user, passwd));
            Map<String, String> confMap = new HashMap<>();
            if (confName == null) {
                confMap = ScmSystem.Configuration.getGlobalConfig(session);
            }
            else {
                String value = ScmSystem.Configuration.getGlobalConfig(session, confName);
                confMap.put(confName, value);
            }
            printConfMap(confMap);
        }
        catch (Exception e) {
            throw new ScmToolsException("show global config failed, " + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            IOUtils.close(session);
        }

    }

    private void printConfMap(Map<String, String> confMap) {
        if (confMap.isEmpty()) {
            System.out.println("No global config to show.");
            return;
        }
        for (Map.Entry<String, String> entry : confMap.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
