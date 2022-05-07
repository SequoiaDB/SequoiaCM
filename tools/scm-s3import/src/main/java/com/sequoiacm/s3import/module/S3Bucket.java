package com.sequoiacm.s3import.module;

import com.google.gson.JsonObject;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.command.CompareCommand;
import com.sequoiacm.s3import.command.MigrateCommand;
import com.sequoiacm.s3import.command.RetryCommand;
import com.sequoiacm.s3import.command.SyncCommand;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.fileoperation.S3ImportFileResource;
import com.sequoiacm.s3import.progress.*;

import java.util.*;

public class S3Bucket implements Comparable<S3Bucket> {

    private String name;
    private String destName;
    private Progress progress;
    private boolean isEnableVersionControl;
    private Queue<String> errorKeyList;
    private String compareResultFilePath;
    private S3ImportFileResource resultFileResource;

    public S3Bucket(String name, String destName, String commandType) throws ScmToolsException {
        this.name = name;
        this.destName = destName;
        initProgress(commandType);
    }

    public S3Bucket(JsonObject progress, String commandType) throws ScmToolsException {
        this.name = progress.get("bucket").getAsString();
        this.destName = progress.get("dest_bucket").getAsString();
        initProgress(commandType);
        this.progress.init(progress);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDestName() {
        return destName;
    }

    public JsonObject getCurrentProgress() {
        JsonObject jsonObject = progress.toProgressJson();
        jsonObject.addProperty("bucket", this.name);
        jsonObject.addProperty("dest_bucket", this.destName);
        return jsonObject;
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public void initProgress(String commandType) throws ScmToolsException {
        switch (commandType) {
            case MigrateCommand.NAME:
                progress = new MigrateProgress();
                break;
            case RetryCommand.NAME:
                progress = new RetryProgress();
                break;
            case CompareCommand.NAME:
                progress = new CompareProgress();
                break;
            case SyncCommand.NAME:
                progress = new SyncProgress();
                break;
            default:
                throw new ScmToolsException("Unrecognized command type, type=" + commandType,
                        S3ImportExitCode.INVALID_ARG);
        }
    }

    public boolean isEnableVersionControl() {
        return isEnableVersionControl;
    }

    public void setEnableVersionControl(boolean enableVersionControl) {
        isEnableVersionControl = enableVersionControl;
    }

    public Queue<String> getErrorKeyList() {
        return errorKeyList;
    }

    public void setErrorKeyList(Queue<String> errorKeyList) {
        this.errorKeyList = errorKeyList;
    }

    public String getCompareResultFilePath() {
        return compareResultFilePath;
    }

    public void setCompareResultFilePath(String compareResultFilePath) {
        this.compareResultFilePath = compareResultFilePath;
    }

    public S3ImportFileResource getResultFileResource() {
        return resultFileResource;
    }

    public void setResultFileResource(S3ImportFileResource resultFileResource) {
        this.resultFileResource = resultFileResource;
    }

    public void releaseResultFileResource() {
        if (this.resultFileResource != null) {
            this.resultFileResource.release();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        S3Bucket s3Bucket = (S3Bucket) o;
        return Objects.equals(name, s3Bucket.name) && Objects.equals(destName, s3Bucket.destName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, destName);
    }

    @Override
    public int compareTo(S3Bucket o) {
        if (this.name.equals(o.name)) {
            return this.destName.compareTo(o.destName);
        }
        return this.name.compareTo(o.name);
    }
}
