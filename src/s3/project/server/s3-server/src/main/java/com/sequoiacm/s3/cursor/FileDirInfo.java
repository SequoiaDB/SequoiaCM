package com.sequoiacm.s3.cursor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.remote.ScmDirInfo;
import com.sequoiacm.s3.remote.ScmFileInfo;
import com.sequoiacm.s3.utils.CommonUtil;

public class FileDirInfo {
    public static final String DATA_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String formatDate(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATA_PATTERN);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(time));
    }

    private String id;
    private String fullName;
    private boolean isDir;
    private long size;
    private String updateTime;
    private String etag;
    private String user;
    private String name;

    public FileDirInfo(String parentDir, ScmFileInfo fileInfo) {
        fullName = CommonUtil.concatPath(parentDir, fileInfo.getName());
        name = fileInfo.getName();
        isDir = false;
        size = fileInfo.getSize();
        updateTime = formatDate(fileInfo.getUpdateTime());
        etag = fileInfo.getCustomMetaEtag();
        user = fileInfo.getUser();
        id = fileInfo.getId();
    }

    public FileDirInfo(String parentDir, ScmDirInfo dir) {
        fullName = CommonUtil.concatPath(parentDir, dir.getName()) + S3CommonDefine.SCM_DIR_SEP;
        isDir = true;
        id = dir.getId();
    }

    public String getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public String getEtag() {
        return etag;
    }

    public long getSize() {
        return size;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public boolean isDir() {
        return isDir;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRelativePath(String parent) {
        if (!parent.endsWith(S3CommonDefine.SCM_DIR_SEP)) {
            parent += S3CommonDefine.SCM_DIR_SEP;
        }
        if (!fullName.startsWith(parent)) {
            throw new RuntimeException("wrong parent:" + parent + ", " + fullName);
        }

        return fullName.substring(parent.length(), fullName.length());
    }
}
