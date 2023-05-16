package com.sequoiacm.tools.tag.common;

import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.tag.command.UpgradeWorkspaceTagTool;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class UpgradeTagStatus {
    public static final String STATUS_FILE_KEY_WORKSPACES = "workspaces";
    public static final String STATUS_FILE_KEY_MDSURL = "mdsurl";
    public static final String STATUS_FILE_KEY_MDSUSER = "mdsuser";
    public static final String STATUS_FILE_KEY_MDSPASSWD = "mdspasswd";
    public static final String STATUS_FILE_KEY_THREAD = "thread";
    public static final String STATUS_FILE_KEY_CURRENT_WORKSPACE = "currentWorkspace";
    public static final String STATUS_FILE_KEY_CURRENT_WORKSPACE_FILE_COUNT = "currentWorkspaceFileCount";
    public static final String STATUS_FILE_KEY_CURRENT_WORKSPACE_PROCESSED_FILE_COUNT = "currentWorkspaceProcessedFileCount";
    public static final String STATUS_FILE_KEY_FILE_ID_MARKER = "fileIdMarker";
    public static final String STATUS_FILE_KEY_TAG_LIB_DOMAIN = "tagLibDomain";
    public static final String STATUS_FILE_KEY_CURRENT_WORKSPACE_PROCESSING_SCOPE = "currentWorkspaceProcessingScope";
    public static final String STATUS_FILE_KEY_FAILED_FILE_PREFIX = "failedFile.";
    public static final String STATUS_FILE_KEY_CURRENT_WORKSPACE_EMPTY_CUSTOM_TAG_PROCESSED = "isCurrentWorkspaceEmptyCustomTagProcessed";


    private List<String> wsList;
    private List<String> sdbUrl;
    private String sdbUser;
    // 持久化时需要加密
    private String sdbPassword;
    private int thread = UpgradeWorkspaceTagTool.DEFAULT_THREAD;

    private String currentWorkspace;
    private long currentWorkspaceFileCount;
    private long currentWorkspaceProcessedFileCount;

    private String fileIdMarker = CommonDefine.FILE_ID_MARKER_BEGIN;
    private FileScope currentWorkspaceProcessingScope = FileScope.CURRENT;
    private Map<String, List<FileBasicInfo>> failedFile = new HashMap<>();
    private String tagLibDomain;

    private boolean isCurrentWorkspaceEmptyCustomTagProcessed = false;

    // 无需持久化，只是方便外部访问失败文件数
    private int allFailedFileCount = 0;
    // 路径不需要持久化
    private String statusFilePath;

    private UpgradeTagStatus() {
    }

    public static UpgradeTagStatus newStatus(String statusFilePath, List<String> wsList,
            List<String> sdbUrl, String sdbUser, String sdbPassword, int thread,
            String tagLibDomain)
            throws ScmToolsException {
        UpgradeTagStatus status = new UpgradeTagStatus();
        status.statusFilePath = new File(statusFilePath).getAbsolutePath();
        status.setWsList(wsList);
        status.setSdbUrl(sdbUrl);
        status.setSdbUser(sdbUser);
        status.setSdbPassword(sdbPassword);
        status.setThread(thread);
        status.setTaglibDomain(tagLibDomain);

        File parent = new File(statusFilePath).getParentFile();
        if (!parent.exists()) {
            boolean mkdirs = parent.mkdirs();
            if (!mkdirs) {
                throw new ScmToolsException("mkdirs status dir failed, path: " + statusFilePath,
                        ScmBaseExitCode.SYSTEM_ERROR);
            }
        }
        return status;
    }

    public void setCurrentWorkspaceEmptyCustomTagProcessed(boolean currentWorkspaceEmptyCustomTagProcessed) {
        isCurrentWorkspaceEmptyCustomTagProcessed = currentWorkspaceEmptyCustomTagProcessed;
    }

    public boolean isCurrentWorkspaceEmptyCustomTagProcessed() {
        return isCurrentWorkspaceEmptyCustomTagProcessed;
    }

    private void setTaglibDomain(String tagLibDomain) {
        this.tagLibDomain = tagLibDomain;
    }

    public String getTagLibDomain() {
        return tagLibDomain;
    }

    public void save() throws ScmToolsException {

        FileOutputStream statusFileOs = null;
        try {

            Properties prop = new Properties();
            prop.setProperty(STATUS_FILE_KEY_WORKSPACES, ScmCommon.listToString(wsList));
            prop.setProperty(STATUS_FILE_KEY_MDSURL, ScmCommon.listToString(sdbUrl));
            prop.setProperty(STATUS_FILE_KEY_MDSUSER, sdbUser);
            if (sdbPassword != null) {
                prop.setProperty(STATUS_FILE_KEY_MDSPASSWD, ScmPasswordMgr.getInstance()
                        .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, sdbPassword));
            }

            prop.setProperty(STATUS_FILE_KEY_THREAD, String.valueOf(thread));
            prop.setProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE, currentWorkspace);
            prop.setProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE_FILE_COUNT,
                    String.valueOf(currentWorkspaceFileCount));
            prop.setProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE_PROCESSED_FILE_COUNT,
                    String.valueOf(currentWorkspaceProcessedFileCount));
            prop.setProperty(STATUS_FILE_KEY_FILE_ID_MARKER, fileIdMarker);
            prop.setProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE_PROCESSING_SCOPE,
                    currentWorkspaceProcessingScope.name());
            prop.setProperty(STATUS_FILE_KEY_TAG_LIB_DOMAIN, tagLibDomain);

            if (failedFile != null) {
                for (Map.Entry<String, List<FileBasicInfo>> entry : failedFile.entrySet()) {
                    String wsName = entry.getKey();
                    List<FileBasicInfo> files = entry.getValue();
                    BasicBSONList filesBson = new BasicBSONList();
                    for (FileBasicInfo file : files) {
                        filesBson.add(file.asStatusFailedFileBson());
                    }
                    prop.setProperty(STATUS_FILE_KEY_FAILED_FILE_PREFIX + wsName,
                            filesBson.toString());
                }
            }

            prop.setProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE_EMPTY_CUSTOM_TAG_PROCESSED,
                    String.valueOf(isCurrentWorkspaceEmptyCustomTagProcessed));

            statusFileOs = new FileOutputStream(statusFilePath);
            prop.store(statusFileOs, null);
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to save status file: statusFile=" + statusFilePath,
                    ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            IOUtils.close(statusFileOs);
        }
    }

    public static UpgradeTagStatus load(String statusFilePath) throws ScmToolsException {
        UpgradeTagStatus status = new UpgradeTagStatus();
        status.statusFilePath = statusFilePath;
        Properties prop = new Properties();
        FileInputStream statusFileIs = null;

        try {
            statusFileIs = new FileInputStream(statusFilePath);
            prop.load(statusFileIs);

            String wsListStr = prop.getProperty(STATUS_FILE_KEY_WORKSPACES);
            if (wsListStr == null || wsListStr.isEmpty()) {
                throw new ScmToolsException(
                        "invalid status file, workspace list is empty: " + statusFilePath,
                        ScmBaseExitCode.SYSTEM_ERROR);
            }
            String[] wsArr = wsListStr.split(",");
            status.setWsList(Arrays.asList(wsArr));

            String sdbUrlStr = prop.getProperty(STATUS_FILE_KEY_MDSURL);
            if (sdbUrlStr == null || sdbUrlStr.isEmpty()) {
                throw new ScmToolsException(
                        "invalid status file, mdsurl is empty: " + statusFilePath,
                        ScmBaseExitCode.SYSTEM_ERROR);
            }
            status.sdbUrl = Arrays.asList(sdbUrlStr.split(","));

            status.sdbUser = prop.getProperty(STATUS_FILE_KEY_MDSUSER);
            status.sdbPassword = prop.getProperty(STATUS_FILE_KEY_MDSPASSWD);
            if (status.sdbPassword != null && !status.sdbPassword.isEmpty()) {
                status.sdbPassword = ScmPasswordMgr.getInstance()
                        .decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, status.sdbPassword);
            }

            String threadStr = prop.getProperty(STATUS_FILE_KEY_THREAD);
            if (threadStr != null && !threadStr.isEmpty()) {
                status.thread = Integer.parseInt(threadStr);
            }

            status.currentWorkspace = prop.getProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE);
            String currentWorkspaceFileCountStr = prop
                    .getProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE_FILE_COUNT);
            if (currentWorkspaceFileCountStr != null && !currentWorkspaceFileCountStr.isEmpty()) {
                status.currentWorkspaceFileCount = Long.parseLong(currentWorkspaceFileCountStr);
            }

            String currentWorkspaceProcessedFileCountStr = prop
                    .getProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE_PROCESSED_FILE_COUNT);
            if (currentWorkspaceProcessedFileCountStr != null
                    && !currentWorkspaceProcessedFileCountStr.isEmpty()) {
                status.currentWorkspaceProcessedFileCount = Long
                        .parseLong(currentWorkspaceProcessedFileCountStr);
            }

            status.fileIdMarker = prop.getProperty(STATUS_FILE_KEY_FILE_ID_MARKER,
                    CommonDefine.FILE_ID_MARKER_BEGIN);
            status.currentWorkspaceProcessingScope = FileScope.valueOf(prop.getProperty(
                    STATUS_FILE_KEY_CURRENT_WORKSPACE_PROCESSING_SCOPE, FileScope.CURRENT.name()));

            status.tagLibDomain = prop.getProperty(STATUS_FILE_KEY_TAG_LIB_DOMAIN);
            status.failedFile = new HashMap<>();
            for (String confKey : prop.stringPropertyNames()) {
                if (confKey.startsWith(STATUS_FILE_KEY_FAILED_FILE_PREFIX)
                        && confKey.length() > STATUS_FILE_KEY_FAILED_FILE_PREFIX.length()) {
                    String wsName = confKey.substring(confKey.indexOf(".") + 1);
                    String failedFilesStr = prop.getProperty(confKey);
                    BasicBSONList failedFilesBson = (BasicBSONList) JSON.parse(failedFilesStr);
                    List<FileBasicInfo> failedFiles = new ArrayList<>();
                    for (Object failedFileObj : failedFilesBson) {
                        BSONObject failedFileBson = (BSONObject) failedFileObj;
                        failedFiles.add(new FileBasicInfo(failedFileBson));
                        status.allFailedFileCount++;
                    }
                    status.failedFile.put(wsName, failedFiles);
                }
            }
            status.isCurrentWorkspaceEmptyCustomTagProcessed = Boolean.parseBoolean(
                    prop.getProperty(STATUS_FILE_KEY_CURRENT_WORKSPACE_EMPTY_CUSTOM_TAG_PROCESSED,
                            "false"));
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmToolsException("invalid status file: statusFile=" + statusFilePath,
                    ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            IOUtils.close(statusFileIs);
        }

        return status;
    }

    public String getStatusFilePath() {
        return statusFilePath;
    }

    public int getThread() {
        return thread;
    }

    public List<String> getWsList() {
        return wsList;
    }

    public void setWsList(List<String> wsList) {
        this.wsList = wsList;
    }

    public List<String> getSdbUrl() {
        return sdbUrl;
    }

    public void setSdbUrl(List<String> sdbUrl) {
        this.sdbUrl = sdbUrl;
    }

    public String getSdbUser() {
        return sdbUser;
    }

    public void setSdbUser(String sdbUser) {
        this.sdbUser = sdbUser;
    }

    public String getSdbPassword() {
        return sdbPassword;
    }

    public void setSdbPassword(String sdbPassword) {
        this.sdbPassword = sdbPassword;
    }

    public String getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(String currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }

    public long getCurrentWorkspaceFileCount() {
        return currentWorkspaceFileCount;
    }

    public void setCurrentWorkspaceFileCount(long currentWorkspaceFileCount) {
        this.currentWorkspaceFileCount = currentWorkspaceFileCount;
    }

    public long getCurrentWorkspaceProcessedFileCount() {
        return currentWorkspaceProcessedFileCount;
    }

    public void setCurrentWorkspaceProcessedFileCount(long currentWorkspaceProcessedFileCount) {
        this.currentWorkspaceProcessedFileCount = currentWorkspaceProcessedFileCount;
    }

    public void incProcessedFileOfCurrentWorkspace(long inc) {
        this.currentWorkspaceProcessedFileCount += inc;
    }

    public String getFileIdMarker() {
        return fileIdMarker;
    }

    public void setFileIdMarker(String fileIdMarker) {
        this.fileIdMarker = fileIdMarker;
    }

    public FileScope getCurrentWorkspaceProcessingScope() {
        return currentWorkspaceProcessingScope;
    }

    public void setCurrentWorkspaceProcessingScope(FileScope currentWorkspaceProcessingScope) {
        this.currentWorkspaceProcessingScope = currentWorkspaceProcessingScope;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public Map<String, List<FileBasicInfo>> getFailedFile() {
        return failedFile;
    }

    public int getAllFailedFileCount() {
        return allFailedFileCount;
    }

    public int getWorkspaceFailedFileCount(String ws) {
        if (failedFile == null) {
            return 0;
        }
        List<FileBasicInfo> files = failedFile.get(ws);
        if (files == null) {
            return 0;
        }
        return files.size();
    }

    public void setFailedFile(Map<String, List<FileBasicInfo>> failedFile) {
        this.failedFile = failedFile;

        if (failedFile == null) {
            this.allFailedFileCount = 0;
        }
        else {
            this.allFailedFileCount = 0;
            for (List<FileBasicInfo> files : failedFile.values()) {
                this.allFailedFileCount += files.size();
            }
        }
    }

}
