package com.sequoiacm.s3import.common;

import com.google.gson.JsonArray;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import com.sequoiacm.s3import.command.SubCommand;
import com.sequoiacm.s3import.config.ImportPathConfig;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.module.CompareResult;
import com.sequoiacm.s3import.module.S3Bucket;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Queue;

public class FileOperateUtils {

    public static void copyLogXml2WorkPath(File logConfFile) throws ScmToolsException {
        ScmFileResource fileResource = ScmResourceFactory.getInstance()
                .createFileResource(logConfFile);
        InputStream is = null;
        try {
            // note: jar 包内的资源无法使用常规的文件操作
            is = SubCommand.class.getClassLoader().getResourceAsStream("logback.xml");
            String xmlContent = CommonUtils.readInputStream(is);
            String logDirPath = ImportPathConfig.getInstance().getLogPath();
            String logFile = logDirPath + CommonDefine.LogConf.FILE;
            String fileNamePattern = logDirPath + CommonDefine.LogConf.FILE_NAME_PATTERN;
            xmlContent = xmlContent.replace(CommonDefine.LogConf.DEFAULT_FILE, logFile)
                    .replace(CommonDefine.LogConf.DEFAULT_FILE_NAME_PATTERN, fileNamePattern);
            fileResource.writeFile(xmlContent);
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to read log configuration file",
                    S3ImportExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(is);
            fileResource.release();
        }
    }

    public static void updateProgress(String progressFilePath, List<S3Bucket> bucketList)
            throws ScmToolsException {
        File progressFile = new File(progressFilePath);
        ScmFileResource resource = ScmResourceFactory.getInstance()
                .createFileResource(progressFile);
        try {
            JsonArray progresses = new JsonArray();
            for (S3Bucket s3Bucket : bucketList) {
                progresses.add(s3Bucket.getCurrentProgress());
            }
            resource.writeFile(CommonUtils.toPrettyJson(progresses));
        }
        finally {
            resource.release();
        }
    }

    public static void appendErrorKeyList(S3Bucket s3Bucket, Queue<String> errorKeys)
            throws ScmToolsException {
        if (errorKeys == null || errorKeys.size() == 0) {
            return;
        }

        writeErrorKeyList(s3Bucket, errorKeys, true);
    }

    public static void overwriteErrorKeyList(S3Bucket s3Bucket, Queue<String> errorKeys)
            throws ScmToolsException {
        writeErrorKeyList(s3Bucket, errorKeys, false);
    }

    private static void writeErrorKeyList(S3Bucket s3Bucket, Queue<String> errorKeys,
            boolean isAppend) throws ScmToolsException {
        StringBuilder sb = new StringBuilder();
        while (errorKeys.size() > 0) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(errorKeys.poll());
        }

        File file = new File(ImportPathConfig.getInstance().getErrorKeyFilePath(s3Bucket));
        ScmFileResource resource = ScmResourceFactory.getInstance()
                .createFileResource(file);
        try {
            resource.writeFile(sb.toString(), isAppend);
        }
        finally {
            resource.release();
        }
    }

    public static void appendCompareResult(S3Bucket s3Bucket, String resultDirPath,
            Queue<CompareResult> compareResults) throws ScmToolsException {
        if (compareResults == null || compareResults.size() == 0) {
            return;
        }

        writeCompareResult(s3Bucket, resultDirPath, compareResults, true);
    }

    private static void writeCompareResult(S3Bucket s3Bucket, String resultDirPath,
            Queue<CompareResult> compareResults, boolean isAppend) throws ScmToolsException {
        StringBuilder sb = new StringBuilder();
        while (compareResults.size() > 0) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(compareResults.poll());
        }

        String resultPath = resultDirPath + File.separator + s3Bucket.getName() + "_"
                + s3Bucket.getDestName();
        File file = new File(resultPath);
        ScmFileResource resource = ScmResourceFactory.getInstance()
                .createFileResource(file);
        try {
            resource.writeFile(sb.toString(), isAppend);
        }
        finally {
            resource.release();
        }
    }

    public static void moveFile(String filePath, String targetDir) throws ScmToolsException {
        try {
            FileUtils.moveFileToDirectory(new File(filePath), new File(targetDir), true);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "Failed to move file, filePath=" + filePath + ", targetDir=" + targetDir,
                    S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }
}
