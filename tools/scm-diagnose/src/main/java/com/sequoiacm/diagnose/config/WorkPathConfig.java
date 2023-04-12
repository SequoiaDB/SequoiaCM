package com.sequoiacm.diagnose.config;

import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkPathConfig {
    private static volatile WorkPathConfig INSTANCE;
    private String workPath; // 工作路径
    private String userWorkDir; // 用户执行工具时目录
    private String compareResultPath; // 数据一致性检测结果路径
    private String residueResultPath; // 数据残留检测结果路径
    private String secretPath; // 密码文件路径
    private String residueListPath; // 残留列表文件路径
    private String compareResultFilePath; // 数据一致性检测结果文件路径
    private String residueErrorFilePath; // 数据残留检测结果文件路径
    private String nullMd5FilePath; // 无 md5 值文件路径

    public static WorkPathConfig getInstance() throws ScmToolsException {
        if (null == INSTANCE) {
            synchronized (WorkPathConfig.class) {
                if (null == INSTANCE) {
                    INSTANCE = new WorkPathConfig();
                }
            }
        }
        return INSTANCE;
    }

    private WorkPathConfig() throws ScmToolsException {
        this.userWorkDir = getStandardFilePath(ScmCommon.getUserWorkingDir());
    }

    public void initWorkPath(String path) throws ScmToolsException {
        path = getStandardFilePath(path);
        File workPathDir = new File(path);
        if (!workPathDir.isAbsolute()) {
            path = CommonUtils.getNormalizationPath(this.userWorkDir + path);
        }
        this.workPath = path + File.separator;
        this.secretPath = workPath + "secret" + File.separator;

        Date date = new Date();
        String runTime = formatDate(date);
        this.compareResultPath = workPath + "compare_result" + File.separator + runTime
                + File.separator;
        this.residueResultPath = workPath + "residue_result" + File.separator + runTime
                + File.separator;

        this.residueErrorFilePath = this.residueResultPath + "error_list";
        this.residueListPath = this.residueResultPath + "residue_list";
        this.compareResultFilePath = this.compareResultPath + "result";
        this.nullMd5FilePath = this.compareResultPath + "null_md5";
        ScmCommon.createDir(this.secretPath);
    }

    private String getStandardFilePath(String filePath) {
        return filePath.endsWith(File.separator) ? filePath : filePath + File.separator;
    }

    public String getSecretPath() {
        return secretPath;
    }

    public String getUserWorkDir() {
        return userWorkDir;
    }

    public String getResidueErrorFilePath() {
        return residueErrorFilePath;
    }

    public String getResidueIdFilePath() {
        return this.residueListPath;
    }

    public String getCompareResultFilePath() {
        return this.compareResultFilePath;
    }

    public String getNullMd5FilePath() {
        return nullMd5FilePath;
    }

    public String getCompareResultPath() {
        return compareResultPath;
    }

    public String getResidueResultPath() {
        return residueResultPath;
    }

    private String formatDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return format.format(date);
    }
}
