package com.sequoiacm.daemon.manager;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmCmdResult;
import com.sequoiacm.daemon.element.ScmCron;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.exec.ScmExecutor;
import com.sequoiacm.infrastructure.tool.common.PropertiesUtil;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ScmCronMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmCronMgr.class);

    private ScmExecutor executor;
    private String cronPropPath;
    private static volatile ScmCronMgr instance;

    public static ScmCronMgr getInstance() throws ScmToolsException {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmCronMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmCronMgr();
            return instance;
        }
    }

    private ScmCronMgr() throws ScmToolsException {
        this.executor = CommonUtils.getExecutor();
        String daemonHomePath = System.getProperty(DaemonDefine.USER_DIR);
        this.cronPropPath = daemonHomePath + File.separator + DaemonDefine.CONF + File.separator
                + DaemonDefine.CRON_PROPERTIES;
    }

    public void createCron(int period, String startDaemonCommand) throws ScmToolsException {
        String cron = getLinuxCron(period);
        ScmCron scmCron = null;
        ScmCmdResult result = null;
        try {
            String linuxCron = cron + " " + startDaemonCommand;
            String cmd = "(crontab -l | grep -v \"" + linuxCron + "\";echo \"" + linuxCron
                    + "\") | crontab -";
            logger.info("Creating linux crontab by exec cmd (/bin/sh -c \" " + cmd + "\")");
            result = executor.execCmd(cmd);
            logger.info("Create linux crontab success,crontab:{}", linuxCron);

            String user = CommonUtils.getUser();
            scmCron = new ScmCron(user, linuxCron, period);
            writeCronProp(scmCron);
        }
        catch (ScmToolsException e) {
            if (result != null && result.getRc() == 0) {
                deleteCron(scmCron);
            }
            throw new ScmToolsException("Failed to create linux crontab", e.getExitCode(), e);
        }
    }

    public void deleteCron(ScmCron scmCron) throws ScmToolsException {
        if (scmCron == null) {
            return;
        }
        String user = CommonUtils.getUser();
        if (!scmCron.getUser().equals(user)) {
            throw new ScmToolsException("The user stop daemon is different from start, stop:" + user
                    + ", start:" + scmCron.getUser(), ScmExitCode.PERMISSION_ERROR);
        }
        String linuxCron = scmCron.getLinuxCron();
        if (linuxCron == null) {
            return;
        }
        String cmd = "(crontab -l | grep -v \"" + linuxCron + "\") | crontab -";
        try {
            logger.info("Deleting linux crontab by exec cmd (/bin/sh -c \" " + cmd + "\")");
            executor.execCmd(cmd);
            logger.info("Delete linux crontab success,crontab:{}", linuxCron);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to delete linux crontab:" + linuxCron,
                    e.getExitCode(), e);
        }
        deleteCronProp();
    }

    public String getLinuxCron(int period) throws ScmToolsException {
        int min = period / (2 * 60);
        String cron = null;
        if (min == 0) {
            cron = "* * * * *";
        }
        else if (min > 0 && min < 60) {
            cron = "*/" + min + " * * * *";
        }
        else {
            int hour = min / 60;
            if (hour > 0 && hour < 24) {
                cron = "0 */" + hour + " * * *";
            }
            else {
                throw new ScmToolsException("Period too large,period:" + period,
                        ScmExitCode.INVALID_ARG);
            }
        }
        return cron;
    }

    private void writeCronProp(ScmCron scmCron) throws ScmToolsException {
        Map<String, String> items = new HashMap<>();
        items.put(DaemonDefine.CRON_USER, scmCron.getUser());
        items.put(DaemonDefine.CRON_LINUX, scmCron.getLinuxCron());
        items.put(DaemonDefine.CRON_PERIOD, scmCron.getPeriod() + "");
        try {
            PropertiesUtil.writeProperties(items, cronPropPath);
            logger.info("Write cron properties success,path:{}", cronPropPath);
        }
        catch (ScmToolsException e) {
            File file = new File(cronPropPath);
            if (file.exists() && !file.delete()) {
                logger.error(
                        "Failed to delete cron properties,file:{},please delete the file before try the command again",
                        cronPropPath);
            }
            throw new ScmToolsException(
                    "Failed to write cron properties,properties:" + cronPropPath, e.getExitCode(),
                    e);
        }
    }

    public ScmCron readCronProp() throws ScmToolsException {
        File file = new File(cronPropPath);
        if (!file.exists()) {
            return null;
        }
        try {
            Properties prop = PropertiesUtil.loadProperties(file);
            ScmCron cron = new ScmCron(prop.getProperty(DaemonDefine.CRON_USER),
                    prop.getProperty(DaemonDefine.CRON_LINUX),
                    Integer.parseInt(prop.getProperty(DaemonDefine.CRON_PERIOD)));
            logger.info("Read cron properties success,user:{},linuxCron:{},period:{}", cron.getUser(),
                    cron.getLinuxCron(),cron.getPeriod());
            return cron;
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to read cron properties,properties:" + cronPropPath,
                    e.getExitCode(), e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to read cron properties,properties:" + cronPropPath,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    private void deleteCronProp() {
        File file = new File(cronPropPath);
        if (file.exists() && !file.delete()) {
            logger.error(
                    "Failed to delete cron properties,file:{},please delete the file before try the command again",
                    cronPropPath);
        }
    }
}
