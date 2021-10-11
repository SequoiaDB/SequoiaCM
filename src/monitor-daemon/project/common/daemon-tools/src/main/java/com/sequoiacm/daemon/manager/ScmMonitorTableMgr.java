package com.sequoiacm.daemon.manager;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.common.TableUtils;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.lock.ScmFileLock;
import com.sequoiacm.daemon.lock.ScmFileLockFactory;
import com.sequoiacm.infrastructure.tool.common.PropertiesUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScmMonitorTableMgr {
    private String tablePath;
    private String backUpPath;
    private String daemonHomePath;
    private final ScmNodeMgr nodeMgr = ScmNodeMgr.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(ScmMonitorTableMgr.class);
    private static volatile ScmMonitorTableMgr instance;

    public static ScmMonitorTableMgr getInstance() throws ScmToolsException {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmMonitorTableMgr.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmMonitorTableMgr();
            return instance;
        }
    }

    private ScmMonitorTableMgr() throws ScmToolsException {
        this.daemonHomePath = System.getProperty(DaemonDefine.USER_DIR);
        this.tablePath = daemonHomePath + File.separator + DaemonDefine.CONF + File.separator
                + DaemonDefine.MONITOR_TABLE;
        this.backUpPath = daemonHomePath + File.separator + DaemonDefine.CONF + File.separator
                + DaemonDefine.MONITOR_BACKUP;

        initTable();
    }

    public List<ScmNodeInfo> listTable() throws ScmToolsException {
        return readTable();
    }

    private void initTable() throws ScmToolsException {
        if (!isTableExist()) {
            try {
                ScmCommon.createFile(tablePath);
            }
            catch (ScmToolsException e) {
                if (e.getExitCode() != ScmExitCode.FILE_ALREADY_EXIST) {
                    throw e;
                }
                logger.warn("Monitor table is already created");
            }
        }
        boolean isInitSuccess = false;
        File file = new File(tablePath);
        ScmFileLock fileLock = ScmFileLockFactory.getInstance().createFileLock(file);
        fileLock.lock();
        try {
            List<ScmNodeInfo> nodeList = null;
            try {
                nodeList = TableUtils.jsonFileToNodeList(file);
            }
            catch (ScmToolsException e) {
                nodeList = recoverTable();
            }
            // 如果列表长度不为 0，说明监控表已经被其他进程初始化成功，那么就直接退出
            if (nodeList != null && nodeList.size() > 0) {
                isInitSuccess = true;
                return;
            }

            nodeList = new ArrayList<>();
            File scmDir = new File(daemonHomePath).getParentFile();
            File[] serviceDirs = scmDir.listFiles();
            if (serviceDirs == null) {
                return;
            }
            for (File serviceDir : serviceDirs) {
                List<ScmNodeInfo> nodeInfoList = nodeMgr.generateNodeInfo(serviceDir);
                nodeList.addAll(nodeInfoList);
            }
            TableUtils.nodeListToJsonFile(nodeList, file);
            isInitSuccess = true;
            logger.info("Init monitor table success,table:{}", tablePath);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to init file,file:" + tablePath, e.getExitCode(),
                    e);
        }
        finally {
            if (!isInitSuccess) {
                if (file.exists()) {
                    FileWriter fileWriter = null;
                    try {
                        fileWriter = new FileWriter(file);
                        fileWriter.write("");
                        fileWriter.flush();
                    }
                    catch (IOException e) {
                        logger.error(
                                "Failed to empty monitor table,file:{},please delete or empty the file before try the command again",
                                tablePath, e);
                    }
                    finally {
                        CommonUtils.closeResource(fileWriter);
                    }
                }
            }
            fileLock.unlock();
        }
    }

    public void addNodeInfo(ScmNodeInfo needAddNode, boolean isOverWrite) throws ScmToolsException {
        boolean isAddSuccess = false;
        String scmdPropertiesPath = null;
        File file = new File(tablePath);
        ScmFileLock fileLock = ScmFileLockFactory.getInstance().createFileLock(file);
        fileLock.lock();
        try {
            scmdPropertiesPath = recordScmdLoc(needAddNode);
            List<ScmNodeInfo> nodeList = readTableWithBackUp(tablePath, backUpPath);
            ScmNodeInfo nodeInTable = null;
            for (ScmNodeInfo node : nodeList) {
                if (node.getPort() == needAddNode.getPort()) {
                    nodeInTable = node;
                    break;
                }
            }
            if (!isOverWrite) {
                if (nodeInTable != null) {
                    throw new ScmToolsException(
                            "Failed to add nodeInfo, caused by: nodeInfo exist, node:"
                                    + needAddNode.toString(),
                            ScmExitCode.SCM_ALREADY_EXIST_ERROR);
                }
            }
            else {
                if (nodeInTable != null) {
                    nodeList.remove(nodeInTable);
                }
            }
            nodeList.add(needAddNode);
            TableUtils.nodeListToJsonFile(nodeList, file);
            isAddSuccess = true;
        }
        finally {
            if (!isAddSuccess && scmdPropertiesPath != null) {
                File propFile = new File(scmdPropertiesPath);
                if (propFile.exists() && !propFile.delete()) {
                    logger.error(
                            "Failed to delete daemon properties,file:{},please delete the file before try the command again",
                            scmdPropertiesPath);
                }
            }
            fileLock.unlock();
        }
    }

    private String recordScmdLoc(ScmNodeInfo nodeInfo) throws ScmToolsException {
        String confPath = nodeInfo.getConfPath();
        String dirName = nodeInfo.getServerType().getDirName();
        String serviceParentPath = confPath.substring(0, confPath.indexOf(dirName));
        File serviceParentDir = new File(serviceParentPath);

        String daemonHomePath = System.getProperty(DaemonDefine.USER_DIR);
        File scmDir = new File(daemonHomePath).getParentFile();

        // 判断要监控的服务目录是否与守护进程工具目录在同一个父目录下，如果不在需要记录工具的绝对路径
        if (!serviceParentDir.getAbsolutePath().equals(scmDir.getAbsolutePath())) {
            String conf = File.separator + DaemonDefine.CONF;
            String propPath = confPath.substring(0, confPath.indexOf(conf) + conf.length())
                    + File.separator + DaemonDefine.SCMD_PROPERTIES;
            Map<String, String> items = new HashMap<>();
            items.put(DaemonDefine.DAEMON_LOCATION, daemonHomePath);
            try {
                PropertiesUtil.writeProperties(items, propPath);
            }
            catch (ScmToolsException e) {
                File file = new File(propPath);
                if (file.exists() && !file.delete()) {
                    logger.error(
                            "Failed to delete daemon properties,file:{},please delete the file before try the command again",
                            propPath);
                }
                throw new ScmToolsException(
                        "Failed to write daemon properties,properties:" + propPath,
                        e.getExitCode(), e);
            }
            logger.info("Write daemon properties success,properties:{}", propPath);
            return propPath;
        }
        return null;
    }

    public void removeNodeInfo(ScmNodeInfo nodeInfo) throws ScmToolsException {
        File file = new File(tablePath);
        ScmFileLock fileLock = ScmFileLockFactory.getInstance().createFileLock(file);
        fileLock.lock();
        try {
            List<ScmNodeInfo> nodeList = readTableWithBackUp(tablePath, backUpPath);

            for (ScmNodeInfo node : nodeList) {
                if (node.getPort() == nodeInfo.getPort()) {
                    nodeList.remove(node);
                    break;
                }
            }
            TableUtils.nodeListToJsonFile(nodeList, file);
            logger.info("Remove node success,nodeInfo:{}", nodeInfo.toString());
        }
        finally {
            fileLock.unlock();
        }
    }

    public void changeStatus(ScmNodeInfo needChangeNode) throws ScmToolsException {
        File file = new File(tablePath);
        ScmFileLock fileLock = ScmFileLockFactory.getInstance().createFileLock(file);
        fileLock.lock();
        try {
            List<ScmNodeInfo> nodeList = readTableWithBackUp(tablePath, backUpPath);

            if (needChangeNode.getPort() > 0) {
                int port = needChangeNode.getPort();
                for (ScmNodeInfo node : nodeList) {
                    if (node.getPort() == port) {
                        logger.info("Change node:[{}] status,original:{},changed:{}",
                                node.toString(), node.getStatus(), needChangeNode.getStatus());
                        node.setStatus(needChangeNode.getStatus());
                        break;
                    }
                }
            }
            else if (needChangeNode.getServerType() != null) {
                for (ScmNodeInfo node : nodeList) {
                    if (node.getServerType().getType()
                            .equals(needChangeNode.getServerType().getType())) {
                        logger.info("Change node:[{}] status,original:{},changed:{}",
                                node.toString(), node.getStatus(), needChangeNode.getStatus());
                        node.setStatus(needChangeNode.getStatus());
                    }
                }
            }
            // 如果不是前两者，说明用户设置 type=all
            else {
                for (ScmNodeInfo node : nodeList) {
                    logger.info("Change node:[{}] status,original:{},changed:{}", node.toString(),
                            node.getStatus(), needChangeNode.getStatus());
                    node.setStatus(needChangeNode.getStatus());
                }
            }
            TableUtils.nodeListToJsonFile(nodeList, file);
        }
        finally {
            fileLock.unlock();
        }
    }

    public boolean isTableExist() {
        File file = new File(tablePath);
        return file.exists();
    }

    public List<ScmNodeInfo> readTable() throws ScmToolsException {
        List<ScmNodeInfo> nodeList = null;
        File tableFile = new File(tablePath);
        ScmFileLock fileLock = ScmFileLockFactory.getInstance().createFileLock(tableFile);
        fileLock.readLock();
        try {
            nodeList = TableUtils.jsonFileToNodeList(tableFile);
        }
        catch (ScmToolsException e) {
            nodeList = recoverTable();
        }
        finally {
            fileLock.unlock();
        }
        return nodeList == null ? new ArrayList<ScmNodeInfo>() : nodeList;
    }

    private List<ScmNodeInfo> recoverTable() throws ScmToolsException {
        File tableFile = new File(tablePath);
        try {
            TableUtils.copyFile(backUpPath, tablePath);
            try {
                List<ScmNodeInfo> nodeList = TableUtils.jsonFileToNodeList(tableFile);
                return nodeList;
            }
            catch (ScmToolsException e) {
                File backUpFile = new File(backUpPath);
                if (backUpFile.exists() && !backUpFile.delete()) {
                    logger.error(
                            "Failed to delete backUp file,file:{},please delete it before try exec command again",
                            backUpPath);
                }
                throw new ScmToolsException(
                        "Backup file is damaged and it is deleted, please try exec command again",
                        ScmExitCode.INVALID_ARG, e);
            }
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to recover file,file:" + tablePath, e.getExitCode(),
                    e);
        }
    }

    private List<ScmNodeInfo> readTableWithBackUp(String tablePath, String backUpPath)
            throws ScmToolsException {
        File tableFile = new File(tablePath);
        List<ScmNodeInfo> nodeList = null;

        try {
            nodeList = TableUtils.jsonFileToNodeList(tableFile);
            try {
                TableUtils.copyFile(tablePath, backUpPath);
            }
            catch (ScmToolsException e) {
                logger.warn("Failed to back up file,file:{},backup:{}", tableFile, backUpPath);
            }
        }
        catch (ScmToolsException e) {
            nodeList = recoverTable();
        }
        logger.info("Back up monitor table success");
        return nodeList == null ? new ArrayList<ScmNodeInfo>() : nodeList;
    }

    public void setTablePath(String tablePath) {
        this.tablePath = tablePath;
    }

    public void setBackUpPath(String backUpPath) {
        this.backUpPath = backUpPath;
    }

    public void setMonitorPath(String daemonHomePath) {
        this.daemonHomePath = daemonHomePath;
    }
}
