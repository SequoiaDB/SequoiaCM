package com.sequoiacm.mappingutil.config;

import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.common.CommonUtils;

import java.io.File;

public class PathConfig {

    private static volatile PathConfig instance = null;

    private String workPath;           // 工作目录
    private String progressFilePath;   // 进度监控文件

    private String logPath;

    private String workConfPath;
    private String workConfFilePath;
    private String logConfFilePath;

    private String errorPath;
    private String errorIdPath;
    private String unAttachableIdPath;

    private PathConfig() {

    }

    public static PathConfig getInstance() throws ScmToolsException {
        if (instance == null) {
            synchronized (PathConfig.class) {
                if (instance == null) {
                    instance = new PathConfig();
                }
            }
        }

        return instance;
    }

    public void init(String workPath) throws ScmToolsException {
        workPath = CommonUtils.getStandardDirPath(workPath);
        this.workPath = workPath;

        this.progressFilePath = workPath + "mapping_progress.json";
        this.logPath = workPath + "log" + File.separator;

        this.workConfPath = workPath + "conf" + File.separator;
        this.workConfFilePath = workConfPath + "work.conf";
        this.logConfFilePath = workConfPath + "logback.xml";
        ScmCommon.createDir(this.workConfPath);

        this.errorPath = workPath + "error" + File.separator;
        this.errorIdPath = errorPath + "error_file_id.list";
        this.unAttachableIdPath = errorPath + "unattachable_file_id.list";
    }

    public String getProgressFilePath() {
        return progressFilePath;
    }

    public String getWorkPath() {
        return workPath;
    }

    public String getWorkConfFilePath() {
        return workConfFilePath;
    }

    public String getLogConfFilePath() {
        return logConfFilePath;
    }

    public String getLogPath() {
        return logPath;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public String getErrorFileIdPath() {
        return errorIdPath;
    }

    public String getUnAttachableFileIdPath() {
        return unAttachableIdPath;
    }
}
