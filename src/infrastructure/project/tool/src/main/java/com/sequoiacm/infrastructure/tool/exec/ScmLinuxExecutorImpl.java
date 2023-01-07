package com.sequoiacm.infrastructure.tool.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeProcessInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeStatus;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmLinuxExecutorImpl implements ScmExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScmLinuxExecutorImpl.class);

    @Override
    public void startNode(String jarPath, String springConfigLocation, String loggingConfig,
            String errorLogPath, String options, String workingDir) throws ScmToolsException {
        if(ScmCommon.isNeedBackup(errorLogPath)){
            ScmCommon.backupErrorOut(errorLogPath);
        }
        ScmCommon.printStartInfo(errorLogPath);
        String cmd = " nohup java " + options + " -jar '" + jarPath + "' --spring.config.location="
                + springConfigLocation + " --logging.config=" + loggingConfig + " >> " + errorLogPath
                + " 2>&1 &";
        logger.info("starting scm by exec cmd(/bin/sh -c \" " + cmd + "\")");
        try {
            execShell(cmd, workingDir);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("start node failed, error:" + e.getMessage(),
                    e.getExitCode(), e);
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
                logger.error(
                        "stop node failed,cmd:/bin/sh -c \"" + killCmd + "\",errorMsg:" + errorMsg);
                if (errorMsg.contains("Operation not permitted")) {
                    throw new ScmToolsException("failed to stop,pid:" + pid + ",error:" + errorMsg,
                            ScmBaseExitCode.PERMISSION_ERROR);
                }
                else {
                    throw new ScmToolsException("failed to stop,pid:" + pid + ",error:" + errorMsg,
                            ScmBaseExitCode.SHELL_EXEC_ERROR);
                }
            }
        }
        catch (InterruptedException e) {
            logger.error("wait cmd return occur error,cmd:/bin/sh -c \"" + killCmd + "\"", e);
            throw new ScmToolsException("wait cmd return occur error,cmd:/bin/sh -c \"" + killCmd
                    + "\",error:" + e.getMessage(), ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (IOException e) {
            logger.error("get cmd output failed,cmd:/bin/sh -c \"" + killCmd + "\"", e);
            throw new ScmToolsException(
                    "get cmd std failed,cmd:/bin/sh -c \"" + killCmd + "\",error:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (Exception e) {
            logger.error("exec cmd occur error,cmd:/bin/sh -c \"" + killCmd + "\"", e);
            throw new ScmToolsException(
                    "get cmd std failed,cmd:/bin/sh -c \"" + killCmd + "\",error:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        finally {
            ps.destroy();
        }
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
        ops.addOption(hp.createOpt(null, ScmToolsDefine.PROPERTIES.APPLICATION_PROPERTIES_LOCATION,
                "", true, true, false));
        String[] args = psTrimedStr.substring(argsIdx).trim().split(" ");
        String confFile = null;
        for (String arg : args) {
            String[] tmpArray = new String[] { arg };
            CommandLine cl = ScmCommandUtil.parseArgs(tmpArray, ops, true);
            if (cl.hasOption(ScmToolsDefine.PROPERTIES.APPLICATION_PROPERTIES_LOCATION)) {
                confFile = cl
                        .getOptionValue(ScmToolsDefine.PROPERTIES.APPLICATION_PROPERTIES_LOCATION);
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
    public ScmNodeStatus getNodeStatus(ScmNodeType nodeType) throws ScmToolsException {
        ScmNodeStatus psRes = new ScmNodeStatus();

        _getNodeStatus(nodeType, psRes);

        return psRes;
    }

    @Override
    public void execShell(String cmd) throws ScmToolsException {
        execShell(cmd, null);
    }

    private void execShell(String cmd, String workingDir) throws ScmToolsException {
        Process ps = exec(cmd, workingDir);

        try {
            int rc = ps.waitFor();
            if (rc != 0) {
                String errorMsg = getErrorMsg(ps);
                logger.error("exec cmd failed,cmd:/bin/sh -c \"" + cmd + "\",errorMsg:" + errorMsg);
                throw new ScmToolsException("exec cmd failed,error:" + errorMsg,
                        ScmBaseExitCode.SHELL_EXEC_ERROR);
            }
        }
        catch (InterruptedException e) {
            logger.error("wait cmd return occur error,cmd:/bin/sh -c \"" + cmd + "\"", e);
            throw new ScmToolsException("wait cmd return occur interrupted exception,cmd:" + cmd
                    + ",error:" + e.getMessage(), ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (IOException e) {
            logger.error("get cmd output failed,cmd:/bin/sh -c \"" + cmd + "\"", e);
            throw new ScmToolsException(
                    "get cmd std failed,cmd:/bin/sh -c \"" + cmd + "\",error:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (Exception e) {
            logger.error("get cmd output failed,cmd:/bin/sh -c \"" + cmd + "\"", e);
            ScmCommon.throwToolException("get cmd std failed,cmd:/bin/sh -c \"" + cmd + "\"", e);
        }
        finally {
            ps.destroy();
        }
    }

    public void _getNodeStatus(ScmNodeType nodeType, ScmNodeStatus res) throws ScmToolsException {
        String[] psCmd = { "/bin/sh", "-c",
                ScmCommandUtil.getPidCommandByjarName(nodeType.getJarNamePrefix()) };
        Process ps = null;
        int rc;
        try {
            ps = Runtime.getRuntime().exec(psCmd);
            rc = ps.waitFor();
            if (rc == 1) {
                // no ps result
                return;
            }
            if (rc != 0) {
                String errMsg = getErrorMsg(ps);
                logger.error("failed to exec cmd:" + cmd2Str(psCmd) + ",error:" + errMsg);
                throw new ScmToolsException(
                        "failed to exec cmd:" + cmd2Str(psCmd) + ",error:" + errMsg,
                        ScmBaseExitCode.SHELL_EXEC_ERROR);
            }

        }
        catch (IOException e) {
            if (ps != null) {
                ps.destroy();
            }
            logger.error("exec cmd occur io error,cmd" + cmd2Str(psCmd), e);
            throw new ScmToolsException(
                    "Failed to exec:" + cmd2Str(psCmd) + ",error:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (InterruptedException e) {
            if (ps != null) {
                ps.destroy();
            }
            logger.error("wait cmd return occur error,cmd:" + cmd2Str(psCmd), e);
            throw new ScmToolsException(
                    "Failed to exec:" + cmd2Str(psCmd) + ",error:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (Exception e) {
            if (ps != null) {
                ps.destroy();
            }
            logger.error("exec cmd occur error,cmd:" + cmd2Str(psCmd), e);
            throw new ScmToolsException(
                    "Failed to exec:" + cmd2Str(psCmd) + ",error:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }

        BufferedReader bfr = null;
        try {
            bfr = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            while (true) {
                String lineSrc = bfr.readLine();
                if (lineSrc == null) {
                    break;
                }
                parseLine(lineSrc, res, nodeType);
            }
        }
        catch (IOException e) {
            logger.error("Failed to access ps std out", e);
            throw new ScmToolsException("Failed to access ps std out:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (Exception e) {
            logger.error("Failed to access ps std out", e);
            throw new ScmToolsException("Failed to access ps std out:" + e.getMessage(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        finally {
            closeStream(bfr);
            ps.destroy();
        }
    }

    private void parseLine(String lineSrc, ScmNodeStatus psRes, ScmNodeType nodeType) {
        // exam:
        // 24613 sequoiacm-contentserver-2.2.0.jar
        // --spring.config.location=/opt/sequoiacm/conf/scm/15200/application.properties
        // --logging.config=/opt/sequoiacm/conf/scm/15200/logback.xml
        try {
            int pid = ScmCommandUtil.getPidFromPsResult(lineSrc);
            String confPath = getConfPathStr(lineSrc.trim(), nodeType.getJarNamePrefix());
            if (null != confPath) {
                if (confPath.startsWith(".")) {
                    File f = new File("");
                    confPath = f.getAbsolutePath() + confPath.substring(1);
                }

                psRes.addNode(new ScmNodeProcessInfo(pid, confPath, nodeType));
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
        return exec(command, null);
    }

    private Process exec(String command, String workingDir) throws ScmToolsException {
        Process ps;
        String[] cmd = new String[3];
        cmd[0] = "/bin/sh";
        cmd[1] = "-c";
        cmd[2] = command;
        try {
            File dir = null;
            if (workingDir != null) {
                dir = new File(workingDir);
            }
            ps = Runtime.getRuntime().exec(cmd, null, dir);
            return ps;
        }
        catch (IOException e) {
            logger.error("exec cmd occur io error,cmd" + cmd2Str(cmd), e);
            throw new ScmToolsException("exec cmd occur io error,cmd" + cmd2Str(cmd),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        catch (Exception e) {
            logger.error("exec cmd occur error,cmd" + cmd2Str(cmd), e);
            throw new ScmToolsException("exec cmd occur error,cmd" + cmd2Str(cmd),
                    ScmBaseExitCode.SYSTEM_ERROR);
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
