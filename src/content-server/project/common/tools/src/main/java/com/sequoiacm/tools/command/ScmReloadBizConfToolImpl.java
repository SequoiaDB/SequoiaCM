package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.tools.ScmCtl;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.common.ScmMetaMgr;
import com.sequoiacm.tools.element.ReloadResInfo;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmReloadBizConfToolImpl extends ScmTool {
    private final String OPT_LONG_RUNNING_NODE = "running-node";
    private final String OPT_SHORT_RUNNING_NODE = "r";

    private final String OPT_LONG_ALL = "all";
    private final String OPT_SHORT_ALL = "a";

    private final String OPT_LONG_SITE = "site";
    private final String OPT_SHORT_SITE = "s";

    private final String OPT_LONG_NODE = "node";
    private final String OPT_SHORT_NODE = "n";

    private Options options;
    private ScmHelpGenerator hp;
    private String contentserverHost = ScmCommon.DEFAULT_CONTENSERVER_HOST;;
    private int contentserverPort = ScmCommon.DEDAULT_CONTENSERVER_PORT;
    private String mainSiteUrl;
    private String mainSiteUser;
    private String mainSitePasswd;
    private ScmMetaMgr mg;
    private ScmSession ss;

    public ScmReloadBizConfToolImpl() throws ScmToolsException {
        super("reloadbizconf");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt(OPT_SHORT_RUNNING_NODE, OPT_LONG_RUNNING_NODE,
                "specified a running node url, this node will\nlaunch a reloadbizconf request, default:\n'localhost:15000'.",
                false, true, false));
        options.addOption(
                hp.createOpt(OPT_SHORT_ALL, OPT_LONG_ALL, "reload all site.", false, false, false));
        options.addOption(hp.createOpt(OPT_SHORT_SITE, OPT_LONG_SITE,
                "site name, reload specified site.", false, true, false));
        options.addOption(hp.createOpt(OPT_SHORT_NODE, OPT_LONG_NODE,
                "node name, reload specified node.", false, true, false));

    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_SHORT_ALL) && (commandLine.hasOption(OPT_SHORT_SITE)
                || commandLine.hasOption(OPT_SHORT_NODE))) {
            throw new ScmToolsException("--" + OPT_LONG_ALL + " --" + OPT_LONG_SITE + " --"
                    + OPT_LONG_NODE + " can be specify only one", ScmExitCode.INVALID_ARG);
        }
        if (commandLine.hasOption(OPT_LONG_SITE) && commandLine.hasOption(OPT_SHORT_NODE)) {
            throw new ScmToolsException("--" + OPT_LONG_ALL + " --" + OPT_LONG_SITE + " --"
                    + OPT_LONG_NODE + " can be specify only one", ScmExitCode.INVALID_ARG);
        }

        if (commandLine.hasOption(OPT_SHORT_RUNNING_NODE)) {
            String c = commandLine.getOptionValue(OPT_SHORT_RUNNING_NODE);
            String[] carrays = c.split(":");
            if (carrays.length != 2) {
                throw new ScmToolsException(
                        "wrong agument for --" + OPT_LONG_RUNNING_NODE + ":" + c,
                        ScmExitCode.INVALID_ARG);
            }
            contentserverPort = ScmCommon.convertStrToInt(carrays[1]);
            contentserverHost = carrays[0];
        }

        try {
            ss = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION,
                    new ScmConfigOption(contentserverHost + ":" + contentserverPort));
        }
        catch (ScmException e1) {
            throw new ScmToolsException(
                    "Can't connetct to contentserver:\"" + contentserverHost + ":"
                            + contentserverPort + "\",errorMsg:" + e1.getMessage(),
                    ScmExitCode.SCM_LOGIN_ERROR);
        }

        try {
            loadRootSiteUrl();
        }
        catch (Exception e) {
            ss.close();
            throw new ScmToolsException("loadRootSiteUrl", ScmExitCode.SCM_GETPROPERTY_FAILED, e);
        }

        mg = new ScmMetaMgr(mainSiteUrl, mainSiteUser, mainSitePasswd);
        ReloadResInfo resInfo = new ReloadResInfo(mg);
        try {
            if (commandLine.hasOption(OPT_SHORT_SITE)) {
                String siteStr = commandLine.getOptionValue(OPT_SHORT_SITE);
                reload(ServerScope.SITE, siteStr, resInfo);
            }
            else if (commandLine.hasOption(OPT_SHORT_NODE)) {
                String serverStr = commandLine.getOptionValue(OPT_SHORT_NODE);
                reload(ServerScope.NODE, serverStr, resInfo);

            }
            else {
                reload(ServerScope.ALL_SITE, null, resInfo);
            }
        }
        finally {
            mg.close();
            ss.close();
        }

        resInfo.printRes();
        if (resInfo.haveFailedHost()) {
            throw new ScmToolsException(ScmExitCode.SCM_RELOADCONF_HAVE_FAILED_HOST);
        }
    }

    private void loadRootSiteUrl() throws Exception {
        List<String> keys = new ArrayList<>();
        keys.add(PropertiesDefine.PROPERTY_ROOTSITE_URL);
        keys.add(PropertiesDefine.PROPERTY_ROOTSITE_USER);
        keys.add(PropertiesDefine.PROPERTY_ROOTSITE_PASSWD);
        BSONObject keyValue;

        try {
            keyValue = ScmSystem.Configuration.getConfProperties(ss, keys);
        }
        catch (ScmException e) {
            throw new ScmToolsException(
                    "Failed to get root site url info from server:" + contentserverHost + ":"
                            + contentserverPort + ",error:" + e.getMessage(),
                    ScmExitCode.SCM_GETPROPERTY_FAILED);
        }

        mainSiteUrl = (String) keyValue.get(PropertiesDefine.PROPERTY_ROOTSITE_URL);
        if (mainSiteUrl == null) {
            throw new ScmToolsException(
                    "Get conf properties missing value of:"
                            + PropertiesDefine.PROPERTY_ROOTSITE_URL,
                    ScmExitCode.SCM_GETPROPERTY_MISSING_MAIN_SITE);
        }
        mainSiteUser = (String) keyValue.get(PropertiesDefine.PROPERTY_ROOTSITE_USER);
        if (mainSiteUser == null) {
            mainSiteUser = "";
        }

        String passwdFile = (String) keyValue.get(PropertiesDefine.PROPERTY_ROOTSITE_PASSWD);
        if (passwdFile == null) {
            passwdFile = "";
        }

        mainSitePasswd = ScmFilePasswordParser.parserFile(passwdFile).getPassword();
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

    private void reload(ServerScope scope, String name, ReloadResInfo resInfo)
            throws ScmToolsException {
        int id;
        if (scope == ServerScope.ALL_SITE) {
            id = -1;
        }
        else if (scope == ServerScope.SITE) {
            id = mg.getSiteIdByName(name);
        }
        else {
            id = mg.getContenserverIdByName(name);
        }

        try {
            List<BSONObject> resList = ScmSystem.Configuration.reloadBizConf(scope, id, ss);
            resInfo.addReloadResList(resList);
            return;
        }
        catch (ScmException e) {
            if (scope == ServerScope.ALL_SITE) {
                throw new ScmToolsException("Failed to reload all site:" + e.getMessage(),
                        ScmExitCode.SCM_RELOADBIZCONF_ERROR);
            }
            else if (scope == ServerScope.SITE) {
                throw new ScmToolsException("Failed to reload " + name + " site:" + e.getMessage(),
                        ScmExitCode.SCM_RELOADBIZCONF_ERROR);
            }
            else {
                throw new ScmToolsException(
                        "Failed to reload " + name + " server:" + e.getMessage(),
                        ScmExitCode.SCM_RELOADBIZCONF_ERROR);
            }
        }
    }

}
