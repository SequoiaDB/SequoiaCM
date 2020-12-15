package com.sequoiacm.tools.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.element.ScmNodeStatus;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmLinuxExecutorImpl implements ScmExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScmLinuxExecutorImpl.class);
    private final String contentServerIdentify = ScmContentCommon.CONTENTSERVER_NAME;

    @Override
    public void startNode(String springConfigLocation, String loggingConfig, String errorLogPath,
            String options) throws ScmToolsException {
        String cmd = " nohup java " + options + " -jar '" + ScmContentCommon.getContentServerJarName()
                + "' --spring.config.location=" + springConfigLocation + " --logging.config="
                + loggingConfig + " > " + errorLogPath + " 2>&1 &";
        logger.info("starting scm by exec cmd(/bin/sh -c \" " + cmd + "\")");
        Process ps = exec(cmd);

        try {
            int rc = ps.waitFor();
            if (rc != 0) {
                String errorMsg = getErrorMsg(ps);
                logger.error("start node failed,exec cmd failed,cmd:/bin/sh -c \"" + cmd
                        + "\",errorMsg:" + errorMsg);
                throw new ScmToolsException("start node failed,error:" + errorMsg,
                        ScmExitCode.SHELL_EXEC_ERROR);
            }
        }
        catch (InterruptedException e) {
            logger.error("wait cmd return occur error,cmd:/bin/sh -c \"" + cmd + "\"", e);
            throw new ScmToolsException("wait cmd return occur interrupted exception,cmd:" + cmd
                    + ",error:" + e.getMessage(), ScmExitCode.INTERRUPT_ERROR);
        }
        catch (IOException e) {
            logger.error("get cmd ouptut failed,cmd:/bin/sh -c \"" + cmd + "\"", e);
            throw new ScmToolsException(
                    "get cmd std failed,cmd:/bin/sh -c \"" + cmd + "\",error:" + e.getMessage(),
                    ScmExitCode.IO_ERROR);
        }
        catch (Exception e) {
            logger.error("get cmd ouptut failed,cmd:/bin/sh -c \"" + cmd + "\"", e);
            throw new ScmToolsException(
                    "get cmd std failed,cmd:/bin/sh -c \"" + cmd + "\",error:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR);
        }
        finally {
            ps.destroy();
        }
    }

    @Override
    public void stopNode(int pid, boolean isForce) throws ScmToolsException {
        String killCmd;
        if (isForce) {
            logger.info("stopping scm:kill -9 " + pid);
            killCmd = "kill -9 " + pid;
        }
        else {
            logger.info("stopping scm:kill -15 " + pid);
            killCmd = "kill -15 " + pid;
        }
        Process ps = exec(killCmd);
        int rc;
        try {
            rc = ps.waitFor();
            // rc == 1 : pid not found
            if (rc == 0) {
                return;
            }
            String errorMsg = getErrorMsg(ps);
            if (rc == 1 && errorMsg.contains("No such process")) {
                return;
            }
            else {
                logger.error("stop node failed,cmd:/bin/sh -c \"" + killCmd + "\",errorMsg:"
                        + errorMsg + ",rc:" + rc);
                if (errorMsg.contains("Operation not permitted")) {
                    throw new ScmToolsException("failed to stop,pid:" + pid + ",error:" + errorMsg,
                            ScmExitCode.PERMISSION_ERROR);
                }
                else {
                    throw new ScmToolsException("failed to stop,pid:" + pid + ",error:" + errorMsg,
                            ScmExitCode.SHELL_EXEC_ERROR);
                }
            }
        }
        catch (InterruptedException e) {
            logger.error("wait cmd return occur error,cmd:/bin/sh -c \"" + killCmd + "\"", e);
            throw new ScmToolsException("wait cmd return occur error,cmd:/bin/sh -c \"" + killCmd
                    + "\",error:" + e.getMessage(), ScmExitCode.INTERRUPT_ERROR);
        }
        catch (IOException e) {
            logger.error("get cmd output failed,cmd:/bin/sh -c \"" + killCmd + "\"", e);
            throw new ScmToolsException(
                    "get cmd std failed,cmd:/bin/sh -c \"" + killCmd + "\",error:" + e.getMessage(),
                    ScmExitCode.IO_ERROR);
        }
        catch (Exception e) {
            logger.error("exec cmd occur error,cmd:/bin/sh -c \"" + killCmd + "\"", e);
            throw new ScmToolsException(
                    "get cmd std failed,cmd:/bin/sh -c \"" + killCmd + "\",error:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR);
        }
        finally {
            ps.destroy();
        }
    }

    private String getPidStr(String psTrimedStr) {
        return psTrimedStr.substring(0, psTrimedStr.indexOf(" "));
    }

    // 24613 sequoiacm-contentserver-2.2.0.jar
    // --spring.config.location=/opt/sequoiacm/conf/scm/15200/application.properties
    // --logging.config=/opt/sequoiacm/conf/scm/15200/logback.xml
    private String getConfPathStr(String psTrimedStr, String contentServerIdentify)
            throws ScmToolsException {
        int argsIdx = psTrimedStr.indexOf(" ", psTrimedStr.indexOf(contentServerIdentify));
        if (argsIdx < 0) {
            return null;
        }

        Options ops = new Options();
        ScmHelpGenerator hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, PropertiesDefine.APPLICATION_PROPERTIES_LOCATION, "", true,
                true, false));
        String[] args = psTrimedStr.substring(argsIdx).trim().split(" ");
        String confFile = null;
        for (String arg : args) {
            String[] tmpArray = new String[] { arg };
            CommandLine cl = ScmContentCommandUtil.parseArgs(tmpArray, ops, true);
            if (cl.hasOption(PropertiesDefine.APPLICATION_PROPERTIES_LOCATION)) {
                confFile = cl.getOptionValue(PropertiesDefine.APPLICATION_PROPERTIES_LOCATION);
                break;
            }
        }

        if (null == confFile) {
            return null;
        }

        File f = new File(confFile);
        if (f.isFile()) {
            f = f.getParentFile();
        }

        return f.getAbsolutePath();
    }

    @Override
    public ScmNodeStatus getNodeStatus() throws ScmToolsException {
        String[] psCmd = { "/bin/sh", "-c", "ps -eo pid,cmd | grep " + contentServerIdentify
                + " -w | grep -w -v grep | grep -w -v nohup" };
        Process ps = null;
        ScmNodeStatus psRes = new ScmNodeStatus();
        int rc;
        try {
            ps = Runtime.getRuntime().exec(psCmd);
            rc = ps.waitFor();
            if (rc == 1) {
                // no ps result
                return psRes;
            }
            if (rc != 0) {
                String errMsg = getErrorMsg(ps);
                logger.error("failed to exec cmd:" + cmd2Str(psCmd) + ",error:" + errMsg);
                throw new ScmToolsException(
                        "failed to exec cmd:" + cmd2Str(psCmd) + ",error:" + errMsg,
                        ScmExitCode.SHELL_EXEC_ERROR);
            }

        }
        catch (IOException e) {
            if (ps != null) {
                ps.destroy();
            }
            logger.error("exec cmd occur io error,cmd" + cmd2Str(psCmd), e);
            throw new ScmToolsException(
                    "Failed to exec:" + cmd2Str(psCmd) + ",error:" + e.getMessage(),
                    ScmExitCode.IO_ERROR);
        }
        catch (InterruptedException e) {
            if (ps != null) {
                ps.destroy();
            }
            logger.error("wait cmd return occur error,cmd:" + cmd2Str(psCmd), e);
            throw new ScmToolsException(
                    "Failed to exec:" + cmd2Str(psCmd) + ",error:" + e.getMessage(),
                    ScmExitCode.INTERRUPT_ERROR);
        }
        catch (Exception e) {
            if (ps != null) {
                ps.destroy();
            }
            logger.error("exec cmd occur error,cmd:" + cmd2Str(psCmd), e);
            throw new ScmToolsException(
                    "Failed to exec:" + cmd2Str(psCmd) + ",error:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR);
        }

        BufferedReader bfr = null;
        try {
            bfr = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            while (true) {
                String lineSrc = bfr.readLine();
                if (lineSrc == null) {
                    break;
                }
                parseLine(lineSrc, psRes);
            }
        }
        catch (IOException e) {
            logger.error("Failed to access ps std out", e);
            throw new ScmToolsException("Failed to access ps std out:" + e.getMessage(),
                    ScmExitCode.IO_ERROR);
        }
        catch (Exception e) {
            logger.error("Failed to access ps std out", e);
            throw new ScmToolsException("Failed to access ps std out:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR);
        }
        finally {
            closeStream(bfr);
            ps.destroy();
        }
        return psRes;

    }

    private void parseLine(String lineSrc, ScmNodeStatus psRes) {
        // exam:
        // 24613 sequoiacm-contentserver-2.2.0.jar
        // --spring.config.location=/opt/sequoiacm/conf/scm/15200/application.properties
        // --logging.config=/opt/sequoiacm/conf/scm/15200/logback.xml
        try {
            String lineTrim = lineSrc.trim();
            String pidStr = getPidStr(lineTrim);
            int pid = Integer.valueOf(pidStr);
            String confPath = getConfPathStr(lineTrim, contentServerIdentify);
            if (null != confPath) {
                if (confPath.startsWith(".")) {
                    File f = new File("");
                    confPath = f.getAbsolutePath() + confPath.substring(1);
                }

                psRes.addNode(confPath, pid);
            }
        }
        catch (Exception e) {
            logger.warn("failed to parse,ignore a line in the ps std out:line=" + lineSrc, e);
        }
    }

    private String getErrorMsg(Process ps) throws IOException {
        BufferedReader ebfr = null;
        BufferedReader stdbfr = null;
        String errMsg = new String();
        try {
            stdbfr = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            errMsg += readReader(stdbfr);
            ebfr = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
            errMsg += readReader(ebfr);
            if (errMsg.endsWith("\n")) {
                errMsg = errMsg.substring(0, errMsg.length() - 1);
            }
        }
        finally {
            closeStream(stdbfr);
            closeStream(ebfr);
        }
        return errMsg;
    }

    private String readReader(BufferedReader ebfr) throws IOException {
        String errMsg = "";
        while (true) {
            String line = ebfr.readLine();
            if (line == null) {
                break;
            }
            errMsg += line;
        }
        return errMsg;
    }

    private Process exec(String command) throws ScmToolsException {
        Process ps;
        String[] cmd = new String[3];
        cmd[0] = "/bin/sh";
        cmd[1] = "-c";
        cmd[2] = command;
        try {
            ps = Runtime.getRuntime().exec(cmd);
            return ps;
        }
        catch (IOException e) {
            logger.error("exec cmd occur io error,cmd" + cmd2Str(cmd), e);
            throw new ScmToolsException("exec cmd occur io error,cmd" + cmd2Str(cmd),
                    ScmExitCode.IO_ERROR);
        }
        catch (Exception e) {
            logger.error("exec cmd occur error,cmd" + cmd2Str(cmd), e);
            throw new ScmToolsException("exec cmd occur error,cmd" + cmd2Str(cmd),
                    ScmExitCode.IO_ERROR);
        }

    }

    private void closeStream(Reader r) {
        if (r != null) {
            try {
                r.close();
            }
            catch (Exception e) {
                logger.warn("close reader occur error", e);
            }
        }
    }

    private String cmd2Str(String[] cmdArray) {
        String tmp = "";
        for (String str : cmdArray) {
            tmp = tmp + str + " ";
        }
        return tmp;
    }

}
