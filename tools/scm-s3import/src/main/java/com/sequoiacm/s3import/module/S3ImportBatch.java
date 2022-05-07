package com.sequoiacm.s3import.module;

import com.sequoiacm.s3import.task.S3ImportTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class S3ImportBatch {

    private S3Bucket bucket;
    private List<S3ImportTask> taskList = new ArrayList<>();

    // 确保线程安全
    private Queue<String> errorKeys = new ConcurrentLinkedQueue<>();
    private Queue<CompareResult> errorSyncKeys = new ConcurrentLinkedQueue<>();
    private boolean hasAbortedTask;

    public S3ImportBatch(S3Bucket bucket) {
        this.bucket = bucket;
    }

    public S3ImportBatch(S3Bucket bucket, List<S3ImportTask> taskList) {
        this.bucket = bucket;
        this.taskList = taskList;
        attach(this.taskList);
    }

    public void addTask(S3ImportTask task) {
        this.taskList.add(attach(task));
    }

    public List<S3ImportTask> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<S3ImportTask> taskList) {
        this.taskList = taskList;
        attach(this.taskList);
    }

    private S3ImportTask attach(S3ImportTask task) {
        task.setBatch(this);
        return task;
    }

    private void attach(List<S3ImportTask> taskList) {
        for (S3ImportTask task : taskList) {
            task.setBatch(this);
        }
    }

    public Queue<String> getErrorKeys() {
        return errorKeys;
    }

    public void addErrorKey(String key) {
        errorKeys.add(key);
    }

    public Queue<CompareResult> getErrorSyncKeys() {
        return errorSyncKeys;
    }

    public void addErrorSyncKey(String key, String syncType) {
        this.errorSyncKeys.add(new CompareResult(key, syncType));
    }

    public boolean hasAbortedTask() {
        return hasAbortedTask;
    }

    public void setHasAbortedTask(boolean hasAbortedTask) {
        this.hasAbortedTask = hasAbortedTask;
    }

    public void success() {
        bucket.getProgress().success();
    }

    public void success(String syncType) {
        bucket.getProgress().success(syncType);
    }

    public void failed() {
        bucket.getProgress().failed();
    }
}
