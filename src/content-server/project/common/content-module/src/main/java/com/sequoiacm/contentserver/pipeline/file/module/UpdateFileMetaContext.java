package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.metasource.TransactionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateFileMetaContext {
    private String fileId;
    private String ws;
    private TransactionContext transactionContext;
    private FileMeta currentLatestVersion;
    private String updateUser;
    private Date updateTime;
    private Map<String, List<FileMetaUpdater>> fileMetaUpdaters = new HashMap<>();
    private ScmVersion expectVersion = new ScmVersion(-1, -1);

    private FileMeta latestVersionAfterUpdate;
    private boolean isContainAllHistoryVersionUpdater;
    private boolean hasGlobalUpdater;
    private FileMeta expectUpdatedFileMeta;
    private boolean hasUserFieldUpdater;

    public void recordUpdatedFileMeta(FileMeta updatedFileMeta) {
        if (updatedFileMeta.getVersion().equals(expectVersion)) {
            this.expectUpdatedFileMeta = updatedFileMeta;
            return;
        }

        if (!expectVersion.isAssigned()) {
            if (updatedFileMeta.getVersion().equals(currentLatestVersion.getVersion())) {
                this.expectUpdatedFileMeta = updatedFileMeta;
            }
        }
    }

    public FileMeta getExpectUpdatedFileMeta() {
        return expectUpdatedFileMeta;
    }

    public void setExpectVersion(ScmVersion expectVersion) {
        if (expectVersion != null) {
            this.expectVersion = expectVersion;
        }
    }

    public ScmVersion getExpectVersion() {
        return expectVersion;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setContainAllHistoryVersionUpdater(boolean containAllHistoryVersionUpdater) {
        isContainAllHistoryVersionUpdater = containAllHistoryVersionUpdater;
    }

    public void setLatestVersionAfterUpdate(FileMeta latestVersionAfterUpdate) {
        this.latestVersionAfterUpdate = latestVersionAfterUpdate;
    }

    public FileMeta getLatestVersionAfterUpdate() {
        return latestVersionAfterUpdate;
    }

    public void setCurrentLatestVersion(FileMeta currentLatestVersion) {
        this.currentLatestVersion = currentLatestVersion;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public FileMeta getCurrentLatestVersion() {
        return currentLatestVersion;
    }

    public List<FileMetaUpdater> getFileMetaUpdaterList() {
        List<FileMetaUpdater> updaters = new ArrayList<>();
        for (List<FileMetaUpdater> l : fileMetaUpdaters.values()) {
            updaters.addAll(l);
        }
        return Collections.unmodifiableList(updaters);
    }

    public void addFileMetaUpdater(FileMetaUpdater updater) {
        List<FileMetaUpdater> updaters = fileMetaUpdaters.get(updater.getKey());
        if (updaters == null) {
            updaters = new ArrayList<>();
            fileMetaUpdaters.put(updater.getKey(), updaters);
        }
        updaters.add(updater);
        if (updater.isGlobal()) {
            hasGlobalUpdater = true;
        }
        if(updater.isUserField()){
            hasUserFieldUpdater = true;
        }
    }

    public boolean isHasGlobalUpdater() {
        return hasGlobalUpdater;
    }

    public void addFileMetaUpdater(List<FileMetaUpdater> updaters) {
        for (FileMetaUpdater u : updaters) {
            addFileMetaUpdater(u);
        }
    }

    public boolean isContainUpdateKey(String key) {
        return fileMetaUpdaters.containsKey(key);
    }

    public List<FileMetaUpdater> getFileMetaUpdater(String key) {
        List<FileMetaUpdater> updaters = fileMetaUpdaters.get(key);
        if (updaters == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(updaters);
    }

    public FileMetaUpdater getFirstFileMetaUpdater(String key) {
        List<FileMetaUpdater> list = getFileMetaUpdater(key);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getWs() {
        return ws;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public void setTransactionContext(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    public boolean containAllHistoryVersionUpdater() {
        return this.isContainAllHistoryVersionUpdater;
    }

    public boolean isHasUserFieldUpdater() {
        return hasUserFieldUpdater;
    }
}
