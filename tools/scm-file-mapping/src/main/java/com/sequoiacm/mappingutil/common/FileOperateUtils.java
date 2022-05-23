package com.sequoiacm.mappingutil.common;

import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.config.PathConfig;
import com.sequoiacm.mappingutil.exception.ScmExitCode;
import com.sequoiacm.mappingutil.exec.MappingProgress;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;

import java.io.*;
import java.util.Queue;

public class FileOperateUtils {

    public static void copyLogXml2WorkPath(File logConfFile) throws ScmToolsException {
        ScmFileResource fileResource = ScmResourceFactory.getInstance()
                .createFileResource(logConfFile);
        InputStream is = null;
        try {
            // note: jar 包内的资源无法使用常规的文件操作
            is = FileOperateUtils.class.getClassLoader().getResourceAsStream("logback_admin.xml");
            String xmlContent = readInputStream(is);
            String logDirPath = PathConfig.getInstance().getLogPath();
            String logFile = logDirPath + CommonDefine.LogConf.FILE;
            String fileNamePattern = logDirPath + CommonDefine.LogConf.FILE_NAME_PATTERN;
            xmlContent = xmlContent.replace(CommonDefine.LogConf.DEFAULT_FILE, logFile)
                    .replace(CommonDefine.LogConf.DEFAULT_FILE_NAME_PATTERN, fileNamePattern);
            fileResource.writeFile(xmlContent);
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to read log configuration file",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(is);
            fileResource.release();
        }
    }

    private static String readInputStream(InputStream is) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            return content.toString();
        }
        finally {
            ScmCommon.closeResource(isr, br);
        }
    }

    public static void updateProgress(MappingProgress progress) throws ScmToolsException {
        PathConfig pathConfig = PathConfig.getInstance();

        File progressFile = new File(pathConfig.getProgressFilePath());
        ScmFileResource resource = ScmResourceFactory.getInstance()
                .createFileResource(progressFile);
        try {
            resource.writeFile(CommonUtils.toJsonString(progress.toJson()));
        }
        finally {
            resource.release();
        }
    }

    public static void appendErrorKeyList(MappingProgress progress) throws ScmToolsException {
        PathConfig pathConfig = PathConfig.getInstance();

        Queue<ScmBucketAttachFailure> unAttachableKeys = progress.getUnAttachableKeys();
        if (unAttachableKeys.size() > 0) {
            StringBuilder sb = new StringBuilder();
            while (unAttachableKeys.size() > 0) {
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                ScmBucketAttachFailure scmBucketAttachFailure = unAttachableKeys.poll();
                sb.append(scmBucketAttachFailure.getError()).append(",")
                        .append(scmBucketAttachFailure.getFileId());
            }
            appendFileContent(pathConfig.getUnAttachableFileIdPath(), sb.toString());
        }

        Queue<String> errorKeys = progress.getErrorKeys();
        if (errorKeys.size() > 0) {
            StringBuilder sb = new StringBuilder();
            while (errorKeys.size() > 0) {
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(errorKeys.poll());
            }
            appendFileContent(pathConfig.getErrorFileIdPath(), sb.toString());
        }
    }

    private static void appendFileContent(String filePath, String content)
            throws ScmToolsException {
        ScmFileResource resource = ScmResourceFactory.getInstance()
                .createFileResource(new File(filePath));
        try {
            resource.writeFile(content, true);
        }
        finally {
            resource.release();
        }
    }
}
