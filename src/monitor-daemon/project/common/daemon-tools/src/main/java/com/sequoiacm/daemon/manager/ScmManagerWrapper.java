package com.sequoiacm.daemon.manager;

import com.sequoiacm.daemon.common.*;
import com.sequoiacm.daemon.element.*;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ScmManagerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ScmManagerWrapper.class);
    private ScmNodeMgr nodeMgr;
    private ScmCronMgr cronMgr;
    private ScmDaemonMgr daemonMgr;
    private ScmMonitorTableMgr tableMgr;

    private final CountDownLatch stopMainThread = new CountDownLatch(1);
    private static final long SLEEP_TIME = 200;
    private static final long STOP_TIME = 15 * 1000;
    private static ScmManagerWrapper instance;

    public static ScmManagerWrapper getInstance() throws ScmToolsException {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmManagerWrapper.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmManagerWrapper();
            return instance;
        }
    }

    private ScmManagerWrapper() throws ScmToolsException {
        nodeMgr = ScmNodeMgr.getInstance();
        cronMgr = ScmCronMgr.getInstance();
        daemonMgr = ScmDaemonMgr.getInstance();
        tableMgr = ScmMonitorTableMgr.getInstance();
    }

    public void startDaemon(int period) throws ScmToolsException {
        checkEnvironment();

        boolean isStartSuccess = false;
        String command = null;
        try {
            ScmCron scmCron = cronMgr.readCronProp();
            if (scmCron != null) {
                if (scmCron.getPeriod() != period) {
                    throw new ScmToolsException(
                            "The period input is different from current, input:" + period
                                    + ",current:" + scmCron.getPeriod()
                                    + ", please stop first if you want to change period",
                            ScmExitCode.INVALID_ARG);
                }
                List<Integer> pidList = getDaemonPid(scmCron);
                if (pidList.size() == 0) {
                    // 守护进程没有跑，清理环境
                    cronMgr.deleteCron(scmCron);
                    cronMgr.deleteCronProp();
                }
                else {
                    // 守护进程已开启，不做处理
                    isStartSuccess = true;
                    logger.info("Daemon is already started");
                    System.out.println("Daemon is already started");
                    return;
                }
            }
            tableMgr.initTable();
            command = daemonMgr.startDaemon(period);
            cronMgr.createCron(period, command);
            isStartSuccess = true;
            logger.info("Start daemon success");
            System.out.println("Start daemon success");
        }
        finally {
            if (!isStartSuccess && command != null) {
                List<Integer> pidList = daemonMgr.getDaemonPid(command);
                for (int pid : pidList) {
                    daemonMgr.stopDaemon(pid, true);
                }
            }
        }
    }

    public void stopDaemon() throws ScmToolsException {
        ScmCron scmCron = cronMgr.readCronProp();
        if (scmCron != null) {
            cronMgr.deleteCron(scmCron);
            List<Integer> pidList = getDaemonPid(scmCron);
            if (pidList.size() == 0) {
                cronMgr.deleteCronProp();
                logger.info("Daemon is already stopped");
                System.out.println("Daemon is already stopped");
                return;
            }
            for (int pid : pidList) {
                long begin = System.currentTimeMillis();
                daemonMgr.stopDaemon(pid, false);
                while (getDaemonPid(scmCron).contains(pid)) {
                    ScmCommon.sleep(SLEEP_TIME);
                    if (System.currentTimeMillis() - begin > STOP_TIME) {
                        daemonMgr.stopDaemon(pid, true);
                        break;
                    }
                }
            }
            cronMgr.deleteCronProp();
            logger.info("Stop daemon success");
            System.out.println("Stop daemon success");
        }
        else {
            logger.info("Daemon is already stopped");
            System.out.println("Daemon is already stopped");
        }
    }

    private List<Integer> getDaemonPid(ScmCron scmCron) throws ScmToolsException {
        if (scmCron == null) {
            return new ArrayList<>();
        }
        // linuxCron: * * * * * export JAVA_HOME=xxx;cd /opt/sequoiacm/daemon;nohup java
        // -cp xxx.jar com.sequoiacm.daemon.Scmd cron --period 5 /opt/sequoiacm/daemon/log/error.out 2>&1 &
        String linuxCron = scmCron.getLinuxCron();
        if (linuxCron == null || linuxCron.length() == 0) {
            return new ArrayList<>();
        }
        String export = "export";
        // cron: export JAVA_HOME=xxx;cd /opt/sequoiacm/daemon;nohup java -cp xxx.jar
        // com.sequoiacm.daemon.Scmd cron --period 5 /opt/sequoiacm/daemon/log/error.out 2>&1 &
        try {
            String cron = linuxCron.substring(linuxCron.indexOf(export)).trim();
            return daemonMgr.getDaemonPid(cron);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to get daemon pid, linuxCron: " + linuxCron,
                    ScmExitCode.INVALID_ARG, e);
        }
    }

    // 跟表相关
    public void changeNodeStatus(ScmNodeMatcher matcher, ScmNodeModifier modifier)
            throws ScmToolsException {
        tableMgr.changeStatus(matcher, modifier);
    }

    public void addNodeInfo(ScmNodeInfo nodeInfo, boolean isOverwrite) throws ScmToolsException {
        int port = nodeMgr.getNodePort(nodeInfo);
        nodeInfo.setPort(port);
        tableMgr.addNodeInfo(nodeInfo, isOverwrite);
    }

    public List<ScmNodeInfo> listNodeInfo() throws ScmToolsException {
        tableMgr = ScmMonitorTableMgr.getInstance();
        return tableMgr.listTable();
    }

    public void checkEnvironment() throws ScmToolsException {
        cronMgr.checkCrontabService();
    }

    public void startTimer(int period, String daemonHomePath) throws ScmToolsException {
        String tablePath = daemonHomePath + File.separator + DaemonDefine.CONF + File.separator
                + DaemonDefine.MONITOR_TABLE;
        String backUpPath = daemonHomePath + File.separator + DaemonDefine.CONF + File.separator
                + DaemonDefine.MONITOR_BACKUP;
        tableMgr.setMonitorPath(daemonHomePath);
        tableMgr.setTablePath(tablePath);
        tableMgr.setBackUpPath(backUpPath);

        logger.info("Starting timer...");
        ScmTimer scmTimer = ScmTimerFactory.createScmTimer();
        ScmTimerTask task = new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    ScmTask.doTask(tableMgr, nodeMgr);
                }
                catch (Throwable e) {
                    logger.error("Timer task is interrupted", e);
                    stopMainThread.countDown();
                }
            }
        };
        scmTimer.schedule(task, 0, period * 1000);

        // 如果主线程不停止，那么守护进程就不会退出
        try {
            stopMainThread.await();
        }
        catch (InterruptedException e) {
            throw new ScmToolsException("Timer main thread is interrupted",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

}
