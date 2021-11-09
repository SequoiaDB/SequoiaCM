package com.sequoiacm.daemon.manager;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.exec.ScmExecutor;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class ScmDaemonMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmDaemonMgr.class);

    private ScmExecutor executor;
    private static ScmDaemonMgr instance;

    public static ScmDaemonMgr getInstance() throws ScmToolsException {
        if (instance != null) {
            return instance;
        }
        instance = new ScmDaemonMgr();
        return instance;
    }

    private ScmDaemonMgr() throws ScmToolsException {
        this.executor = CommonUtils.getExecutor();
    }

    public String startDaemon(int period) throws ScmToolsException {
        try {
            // 如果没有JAVA_HOME，由Linux的crontab开启的守护进程使用节点的脚本时会失败
            String javaHome = System.getenv("JAVA_HOME");
            if (javaHome == null || javaHome.length() == 0) {
                throw new ScmToolsException(
                        "Failed to start daemon process, caused by: missing JAVA_HOME",
                        ScmExitCode.SYSTEM_ERROR);
            }
            // Linux的crontab启动守护进程之后，默认的路径在当前用户的home目录，这会导致日志输出的位置不正确，所以需要跳转到daemon目录下
            String daemonHomePath = ScmHelper.getPwd();
            String errorLogPath = daemonHomePath + File.separator + "log" + File.separator + DaemonDefine.ERROR_OUT;
            String jarPath = CommonUtils.getJarPath(ScmDaemonMgr.class);
            String mainMethod = DaemonDefine.MAIN_METHOD;
            String cmd = "export JAVA_HOME=" + javaHome + ";export PATH=$JAVA_HOME/bin:$PATH;cd " + daemonHomePath
                    + ";nohup java -cp " + jarPath + " " + mainMethod + " cron " + "--period "
                    + period + " > " + errorLogPath + " 2>&1 &";
            logger.debug("Starting daemon process by exec cmd, cmd:{}", cmd);
            executor.execCmd(cmd);
            logger.info("Start daemon process success,exec cmd:{}", cmd);
            return cmd;
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to start daemon process", e.getExitCode(), e);
        }
    }

    public void stopDaemon(int pid, boolean isForced) throws ScmToolsException {
        if (pid < 0) {
            return;
        }
        try {
            executor.killPid(pid, isForced);
            logger.info("Kill daemon process success,pid:{}", pid);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to kill daemon process, pid = " + pid,
                    e.getExitCode(), e);
        }
    }

    public List<Integer> getDaemonPid(String cron) throws ScmToolsException {
        // cron: export JAVA_HOME=xxx;cd /opt/sequoiacm/daemon;nohup java -cp xxx.jar
        //       com.sequoiacm.daemon.Scmd cron --period 5 > /opt/sequoiacm/daemon/log/error.out 2>&1 &
        cron = cron.trim();
        try {
            String nohup = "nohup";
            // processMatcher: java -cp xxx.jar com.sequoiacm.daemon.Scmd cron --period 5
            String processMatcher = cron
                    .substring(cron.indexOf(nohup) + nohup.length(), cron.indexOf(">")).trim();
            logger.info("Get daemon pid by matching condition:{}", processMatcher);
            List<Integer> pidList = executor.getPid(processMatcher);
            logger.info("Get daemon pid success, pidList:{}", pidList);
            return pidList;
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to get daemon pid", e.getExitCode(), e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to cut cron, cron: " + cron,
                    ScmExitCode.INVALID_ARG, e);
        }
    }
}
