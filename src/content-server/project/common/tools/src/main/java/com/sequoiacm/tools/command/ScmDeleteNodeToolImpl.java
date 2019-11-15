package com.sequoiacm.tools.command;

import java.io.File;
import java.net.InetAddress;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.RestDispatcher;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;
import com.sequoiacm.tools.exec.ScmExecutorWrapper;

public class ScmDeleteNodeToolImpl implements ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeleteSiteToolImpl.class);
    private final String OPT_LONG_PORT = "port";
    private final String OPT_LONG_URL = "url";
    private final String OPT_LONG_USER = "user";
    private final String OPT_LONG_PASSWD = "password";

    private final String OPT_SHORT_PORT = "p";

    private ScmHelpGenerator hp;
    private Options ops;

    public ScmDeleteNodeToolImpl() throws ScmToolsException {
        hp = new ScmHelpGenerator();
        ops = new Options();
        ops.addOption(hp.createOpt(OPT_SHORT_PORT, OPT_LONG_PORT, "content node port.", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_URL,
                "gateway url, eg:'host1:port,host2:port,host3:port'.", true, true, false));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_USER, "login admin username.", true, true, false));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_PASSWD, "login admin password.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String portStr = cl.getOptionValue(OPT_LONG_PORT);
        int port = ScmCommon.convertStrToInt(portStr);
        String gatewayUrl = cl.getOptionValue(OPT_LONG_URL);
        String user = cl.getOptionValue(OPT_LONG_USER);
        String passwd = cl.getOptionValue(OPT_LONG_PASSWD);

        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(
                    new ScmConfigOption(ScmCommandUtil.parseListUrls(gatewayUrl), user, passwd));
            ScmUser scmUser = ScmFactory.User.getUser(ss, user);

            if (!scmUser.hasRole("ROLE_AUTH_ADMIN")) {
                throw new ScmException(ScmError.OPERATION_UNAUTHORIZED,
                        "do not have priority to delete node: username=" + user + ", port=" + port);
            }
            stopNode(port);

            String hostName = InetAddress.getLocalHost().getHostName();
            RestDispatcher.getInstance().deleteNode(ss, hostName, port);

            clearNodeFile(port);
            System.out
                    .println("delete content node success:hostName=" + hostName + ", port=" + port);
            logger.info("delete content node success:hostName={}, port={}", hostName, port);
        }
        catch (Exception e) {
            logger.error("delete content node failed: port={}", port, e);
            throw new ScmToolsException("delete content node failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(ss);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

    private void stopNode(int port) throws ScmToolsException {
        ScmExecutorWrapper executor = new ScmExecutorWrapper();
        executor.stopNode(port, true);
    }

    private void clearNodeFile(int port) throws ScmToolsException {
        String confPath = ScmCommon.getScmConfAbsolutePath() + port;
        File confDir = new File(confPath);
        try {
            deleteFile(confDir);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "delete node conf directory failed: confDir=" + confDir.getPath(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }

        String logPath = ".." + File.separator + "log" + File.separator + ScmCommon.SCM_LOG_DIR_NAME
                + File.separator + port;
        File logDir = new File(logPath);
        try {
            deleteFile(logDir);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "delete node log directory failed: logDir=" + logDir.getPath(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    private void deleteFile(File dirFile) throws ScmToolsException {
        if (dirFile.exists()) {
            if (!dirFile.isFile()) {
                for (File subfile : dirFile.listFiles()) {
                    deleteFile(subfile);
                }
            }
            dirFile.delete();
        }
    }
}
