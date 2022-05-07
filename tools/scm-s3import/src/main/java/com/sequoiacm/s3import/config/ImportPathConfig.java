package com.sequoiacm.s3import.config;

import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonUtils;
import com.sequoiacm.s3import.module.S3Bucket;

import java.io.File;

public class ImportPathConfig {

    private static volatile ImportPathConfig INSTANCE;
    private String userWorkDir; // 用户执行路径
    private String basePath;   // 工具所在目录
    private String confPath;   // 配置文件路径

    private String workPath;   // 工作目录
    private String workConfPath;
    private String workEnvFilePath;
    private String logConfFilePath;

    private String migrateProgressFilePath;
    private String compareProgressFilePath;
    private String logPath;
    private String errorPath;
    private String compareResultPath;

    public static ImportPathConfig getInstance() throws ScmToolsException {
        if (INSTANCE == null) {
            synchronized (ImportPathConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ImportPathConfig();
                }
            }
        }
        return INSTANCE;
    }

    private ImportPathConfig() throws ScmToolsException {
        this.userWorkDir = CommonUtils.getStandardFilePath(ScmCommon.getUserWorkingDir());
        this.basePath =  CommonUtils.getStandardFilePath(System.getProperty("user.dir", "."));
        this.confPath = basePath + "conf" + File.separator + "s3import.properties";
    }

    public void initWorkPath(String workPath) throws ScmToolsException {
        workPath = CommonUtils.getStandardFilePath(workPath);
        File workPathDir = new File(workPath);
        if (!workPathDir.isAbsolute()) {
            workPath = this.userWorkDir + workPath;
        }
        this.workPath = workPath;
        this.workConfPath = workPath + "conf" + File.separator;
        this.workEnvFilePath = workConfPath + "work_env.json";
        this.logConfFilePath = workConfPath + "logback.xml";
        this.migrateProgressFilePath = workPath + "migrate_progress.json";
        this.compareProgressFilePath = workPath + "compare_progress.json";
        this.logPath = workPath + "log" + File.separator;
        this.errorPath = workPath + "error" + File.separator;
        this.compareResultPath = workPath + "compare_result" + File.separator;
        CommonUtils.createDir(this.workConfPath);
    }

    public String getConfPath() {
        return confPath;
    }

    public void setConfPath(String confPath) {
        File confFile = new File(confPath);
        if (!confFile.isAbsolute()) {
            this.confPath = this.userWorkDir + confPath;
        }
        else {
            this.confPath = confPath;
        }
    }

    public String getErrorKeyFilePath(S3Bucket s3Bucket) {
        // bucketName_destBucketName_error_key_list
        return errorPath + s3Bucket.getName() + "_" + s3Bucket.getDestName()
                + "_error_key_list";
    }

    public String getWorkEnvFilePath() {
        return workEnvFilePath;
    }

    public String getLogConfFilePath() {
        return logConfFilePath;
    }

    public String getMigrateProgressFilePath() {
        return migrateProgressFilePath;
    }

    public String getCompareProgressFilePath() {
        return compareProgressFilePath;
    }

    public String getLogPath() {
        return logPath;
    }

    public String getCompareResultPath() {
        return compareResultPath;
    }
}
