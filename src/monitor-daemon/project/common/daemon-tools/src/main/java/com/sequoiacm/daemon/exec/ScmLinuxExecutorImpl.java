package com.sequoiacm.daemon.exec;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.element.ScmCmdResult;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScmLinuxExecutorImpl implements ScmExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScmLinuxExecutorImpl.class);

    @Override
    public void killPid(int pid, boolean isForced) throws ScmToolsException {
        String killCmd;
        if (isForced) {
            logger.info("Stopping scmd:kill -9 " + pid);
            killCmd = "kill -9 " + pid;
        }
        else {
            logger.info("Stopping scmd:kill -15 " + pid);
            killCmd = "kill -15 " + pid;
        }
        logger.info("Exec cmd \"" + killCmd + "\"");
        Process ps = exec(killCmd);
        int rc;
        try {
            rc = ps.waitFor();
            // rc == 1 : pid not found
            if (rc == 0) {
                return;
            }
            String errorMsg = getMsg(ps).getStdStr();
            if (rc == 1 && errorMsg.contains("No such process")) {
                return;
            }
            else {
                if (errorMsg.contains("Operation not permitted")) {
                    throw new ScmToolsException("Failed to stop,pid:" + pid + ",error:" + errorMsg,
                            ScmExitCode.PERMISSION_ERROR);
                }
                else {
                    throw new ScmToolsException("Failed to stop,pid:" + pid + ",error:" + errorMsg,
                            ScmExitCode.SHELL_EXEC_ERROR);
                }
            }
        }
        catch (InterruptedException e) {
            throw new ScmToolsException(
                    "Wait cmd return occur error,cmd:/bin/sh -c \"" + killCmd + "\"",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (IOException e) {
            throw new ScmToolsException("Get cmd std failed,cmd:/bin/sh -c \"" + killCmd + "\"",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Get cmd std failed,cmd:/bin/sh -c \"" + killCmd + "\"",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ps.destroy();
        }
    }

    @Override
    public List<Integer> getPid(String match) throws ScmToolsException {
        String cmd = "ps -eo pid,cmd | grep -w \"" + match + "\"| grep -w -v grep";
        Process ps = exec(cmd);
        try {
            int rc = ps.waitFor();
            if (rc == 1) {
                return new ArrayList<>();
            }
            if (rc != 0) {
                String errorMsg = getMsg(ps).getStdStr();
                throw new ScmToolsException(
                        "Failed to exec cmd:/bin/sh -c \"" + cmd + "\",errorMsg:" + errorMsg,
                        ScmExitCode.SHELL_EXEC_ERROR);
            }
        }
        catch (InterruptedException e) {
            throw new ScmToolsException("Wait cmd return occur interrupted exception,cmd:" + cmd,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (IOException e) {
            throw new ScmToolsException("Get cmd std failed,cmd:/bin/sh -c \"" + cmd + "\"",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Exec cmd occur error,cmd:/bin/sh -c \"" + cmd + "\"",
                    ScmExitCode.SYSTEM_ERROR, e);
        }

        BufferedReader bfr = null;
        try {
            bfr = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            List<Integer> pidList = new ArrayList<>();
            String line;
            while (true) {
                line = bfr.readLine();
                if (line == null) {
                    break;
                }
                pidList.add(parsePidLine(line));
            }
            return pidList;
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to access ps std out:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to access ps std out:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            CommonUtils.closeResource(bfr);
            ps.destroy();
        }
    }

    private int parsePidLine(String line) {
        line = line.trim();
        String pidStr = line.substring(0, line.indexOf(" "));
        return Integer.parseInt(pidStr);
    }

    @Override
    public ScmCmdResult execCmd(String cmd) throws ScmToolsException {
        Process ps = exec(cmd);
        int rc;
        try {
            rc = ps.waitFor();
            ScmCmdResult result = getMsg(ps);
            result.setRc(rc);

            if (rc != 0) {
                String errMsg = result.getStdStr();
                throw new ScmToolsException("Failed to exec cmd:" + cmd + ",error:" + errMsg,
                        ScmExitCode.SHELL_EXEC_ERROR, result);
            }

            return result;
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to get cmd output,cmd:/bin/sh -c \"" + cmd + "\"",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (InterruptedException e) {
            throw new ScmToolsException("Wait cmd return occur interrupted exception,cmd:" + cmd,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Exec cmd occur error,cmd:/bin/sh -c \"" + cmd + "\"",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ps.destroy();
        }
    }

    private ScmCmdResult getMsg(Process ps) throws IOException {
        ScmCmdResult result = new ScmCmdResult();
        BufferedReader ebfr = null;
        BufferedReader stdbfr = null;
        try {
            stdbfr = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            result.setStdIn(readReader(stdbfr));
            ebfr = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
            result.setStdErr(readReader(ebfr));
        }
        finally {
            CommonUtils.closeResource(stdbfr);
            CommonUtils.closeResource(ebfr);
        }
        return result;
    }

    private List<String> readReader(BufferedReader ebfr) throws IOException {
        List<String> result = new ArrayList<>();
        while (true) {
            String line = ebfr.readLine();
            if (line == null) {
                break;
            }
            result.add(line);
        }
        return result;
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
            throw new ScmToolsException("Exec cmd occur io error", ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Exec cmd occur error", ScmExitCode.SYSTEM_ERROR, e);
        }
    }
}
