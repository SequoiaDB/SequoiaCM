package com.sequoiacm.tools.tag.command;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.tag.common.SequoiadbDataSourceWrapper;
import com.sequoiacm.tools.tag.common.TagLibMgr;
import com.sequoiacm.tools.tag.common.WorkspaceTagUpgrader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class UpgradeWorkspaceTagTool extends ScmTool {

    public static final int DEFAULT_THREAD = 5;
    private static final Logger logger = LoggerFactory.getLogger(UpgradeWorkspaceTagTool.class);
    private static final String OPT_LONG_TAG_LIB_DOMAIN = "tag-lib-domain";
    private static final String OPT_THREAD = "thread";
    static final String OPT_LONG_WORKSPACES = "workspaces";
    static final String OPT_LONG_MDS_URL = "mdsurl";
    static final String OPT_LONG_MDS_USER = "mdsuser";
    static final String OPT_LONG_MDS_PASSWD = "mdspasswd";
    private final Options options;
    private final ScmHelpGenerator hp;

    public UpgradeWorkspaceTagTool() throws ScmToolsException {
        super("upgradeWorkspaceTag");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt(null, OPT_LONG_WORKSPACES, "workspaces", true, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_TAG_LIB_DOMAIN, "tag lib domain name.", true,
                true, false, false, false));
        options.addOption(hp.createOpt(null, OPT_THREAD, "thread, default 5", false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_MDS_URL,
                "meta source sdb url: host1:11810,host2:11810", true, true, false));
        options.addOption(
                hp.createOpt(null, OPT_LONG_MDS_USER, "meta source sdb user", true, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_MDS_PASSWD, "password for login sdb.", true,
                true, false, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, options);
        String passwd = cl.getOptionValue(OPT_LONG_MDS_PASSWD);
        if (passwd == null) {
            System.out.print("password: ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }

        int thread = Integer.parseInt(cl.getOptionValue(OPT_THREAD, DEFAULT_THREAD + ""));
        String user = cl.getOptionValue(OPT_LONG_MDS_USER);
        String urls = cl.getOptionValue(OPT_LONG_MDS_URL);
        String tagLibDomain = cl.getOptionValue(OPT_LONG_TAG_LIB_DOMAIN);
        String[] urlArray = urls.split(",");
        List<String> urlList = new ArrayList<>(Arrays.asList(urlArray));
        String workspaces = cl.getOptionValue(OPT_LONG_WORKSPACES);
        List<String> wsNameList = Arrays.asList(workspaces.split(","));

        SequoiadbDataSourceWrapper.getInstance().init(urlList, user, passwd);
        try {
            WorkspaceTagUpgrader upgrader = new WorkspaceTagUpgrader(wsNameList, tagLibDomain, thread);
            upgrader.doUpgrade();
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to upgrade workspace tag",
                    ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().destroy();
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
