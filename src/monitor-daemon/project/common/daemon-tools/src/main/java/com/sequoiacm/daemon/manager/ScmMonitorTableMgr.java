package com.sequoiacm.daemon.manager;

import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.element.ScmNodeMatcher;
import com.sequoiacm.daemon.element.ScmNodeModifier;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.lock.ScmFileLock;
import com.sequoiacm.daemon.lock.ScmFileResource;
import com.sequoiacm.daemon.lock.ScmFileResourceFactory;
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

        createTable();
    }

    public List<ScmNodeInfo> listTable() throws ScmToolsException {
        return readTable();
    }

    public void createTable() throws ScmToolsException {
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
    }

    public void initTable(ScmFileResource resource) throws ScmToolsException {
        boolean isInitSuccess = false;
        try {
            List<ScmNodeInfo> nodeList = resource.readFile();
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
            resource.writeFile(nodeList);
            isInitSuccess = true;
            logger.info("Init monitor table success,table:{}", tablePath);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to init monitor table,file:" + tablePath, e.getExitCode(),
                    e);
        }
        finally {
            if (!isInitSuccess) {
                resource.clearFileResource();
            }
        }
    }

    public void addNodeInfo(ScmNodeInfo needAddNode, boolean isOverWrite) throws ScmToolsException {
        boolean isAddSuccess = false;
        String scmdPropertiesPath = null;
        File file = new File(tablePath);
        ScmFileResource resource = ScmFileResourceFactory.getInstance().createFileResource(file, backUpPath);
        ScmFileLock lock = resource.createLock();
        lock.lock();
        try {
            List<ScmNodeInfo> nodeList = resource.readFile();
            scmdPropertiesPath = recordScmdLoc(needAddNode);
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
            resource.writeFile(nodeList);
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
            lock.unlock();
            resource.releaseFileResource();
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
                        "Failed to write daemon properties,properties:" + propPath, e.getExitCode(),
                        e);
            }
            logger.info("Write daemon properties success,properties:{}", propPath);
            return propPath;
        }
        return null;
    }

    public void removeNodeInfo(ScmNodeInfo nodeInfo) throws ScmToolsException {
        File file = new File(tablePath);
        ScmFileResource resource = ScmFileResourceFactory.getInstance().createFileResource(file, backUpPath);
        ScmFileLock lock = resource.createLock();
        lock.lock();
        try {
            List<ScmNodeInfo> nodeList = resource.readFile();

            for (ScmNodeInfo node : nodeList) {
                if (node.getPort() == nodeInfo.getPort()) {
                    nodeList.remove(node);
                    break;
                }
            }
            resource.writeFile(nodeList);
            logger.info("Remove node success,nodeInfo:{}", nodeInfo.toString());
        }
        finally {
            lock.unlock();
            resource.releaseFileResource();
        }
    }

    public void changeStatus(ScmNodeMatcher matcher, ScmNodeModifier modifier)
            throws ScmToolsException {
        File file = new File(tablePath);
        ScmFileResource resource = ScmFileResourceFactory.getInstance().createFileResource(file, backUpPath);
        ScmFileLock lock = resource.createLock();
        lock.lock();
        try {
            List<ScmNodeInfo> nodeList = resource.readFile();

            boolean isNodeExist = false;
            for (ScmNodeInfo nodeInfo : nodeList) {
                if (matcher.isMatch(nodeInfo)) {
                    isNodeExist = true;
                    modifier.modifyNodeInfo(nodeInfo);
                    logger.info("Change nodeInfo success:[{}]", nodeInfo.toString());
                }
            }

            // 如果要修改的节点不存在，那么就打屏告诉用户修改失败，然后直接退出程序
            if (!isNodeExist) {
                logger.warn(
                        "Failed to change node status caused by no matching node in monitor table, condition={}",
                        matcher.toString());
                System.out.println(
                        "Failed to change node status caused by no matching node in monitor table, condition="
                                + matcher.toString());
                System.exit(ScmExitCode.SUCCESS);
            }
            resource.writeFile(nodeList);
        }
        finally {
            lock.unlock();
            resource.releaseFileResource();
        }
    }

    public boolean isTableExist() {
        File file = new File(tablePath);
        return file.exists();
    }

    public List<ScmNodeInfo> readTable() throws ScmToolsException {
        File tableFile = new File(tablePath);
        ScmFileResource resource = ScmFileResourceFactory.getInstance().createFileResource(tableFile, backUpPath);
        ScmFileLock lock = resource.createLock();
        lock.lock();
        try {
            return resource.readFile();
        }
        finally {
            lock.unlock();
            resource.releaseFileResource();
        }
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

    public String getTablePath() {
        return this.tablePath;
    }

    public String getBackUpPath() {
        return this.backUpPath;
    }
}
