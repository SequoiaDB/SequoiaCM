package com.sequoiacm.om.omserver.common;

import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import org.bson.BSONObject;
import org.springframework.util.StringUtils;

import java.io.InputStream;

public class ScmFileUtil {

    public static OmFileBasic transformToFileBasicInfo(ScmFileBasicInfo fileBasicInfo) {
        OmFileBasic omFileBasic = new OmFileBasic();
        omFileBasic.setId(fileBasicInfo.getFileId().get());
        omFileBasic.setName(fileBasicInfo.getFileName());
        omFileBasic.setMimeType(fileBasicInfo.getMimeType());
        omFileBasic.setSize(0);
        omFileBasic.setUser(fileBasicInfo.getUser());
        omFileBasic.setCreateTime(fileBasicInfo.getCreateDate());
        omFileBasic.setUpdateTime(fileBasicInfo.getCreateDate());
        omFileBasic.setMajorVersion(fileBasicInfo.getMajorVersion());
        omFileBasic.setMinorVersion(fileBasicInfo.getMinorVersion());
        omFileBasic.setDeleteMarker(fileBasicInfo.isDeleteMarker());
        return omFileBasic;
    }

    public static OmFileBasic transformToFileBasicInfo(ScmFile file) {
        OmFileBasic omFileBasic = new OmFileBasic();
        omFileBasic.setId(file.getFileId().get());
        omFileBasic.setName(file.getFileName());
        omFileBasic.setMimeType(file.getMimeType());
        omFileBasic.setSize(file.getSize());
        omFileBasic.setUser(file.getUser());
        omFileBasic.setCreateTime(file.getCreateTime());
        omFileBasic.setUpdateTime(file.getUpdateTime());
        omFileBasic.setMajorVersion(file.getMajorVersion());
        omFileBasic.setMinorVersion(file.getMinorVersion());
        omFileBasic.setDeleteMarker(file.isDeleted());
        return omFileBasic;
    }

    public static void createFile(ScmFile scmFile, OmFileInfo fileInfo, BSONObject uploadConfig,
            InputStream is) throws ScmException {
        // 设置文件属性
        scmFile.setMimeType(ScmFileUtil.getMimeTypeByFileName(fileInfo.getName()));
        scmFile.setTitle(fileInfo.getTitle());
        scmFile.setAuthor(fileInfo.getAuthor());
        scmFile.setDirectory(fileInfo.getDirectoryId());
        // 设置标签
        ScmTags scmTags = new ScmTags();
        scmTags.addTags(fileInfo.getTags());
        scmFile.setTags(scmTags);
        // 添加自由标签
        scmFile.setCustomMetadata(fileInfo.getCustomMetadata());
        // 设置元数据
        if (!StringUtils.isEmpty(fileInfo.getClassId())) {
            ScmClassProperties classProperties = new ScmClassProperties(fileInfo.getClassId());
            classProperties.addProperties(fileInfo.getClassProperties());
            scmFile.setClassProperties(classProperties);
        }
        // 添加文件内容，开始上传
        scmFile.setContent(is);
        scmFile.save(getScmUploadConf(uploadConfig));
    }

    private static String getMimeTypeByFileName(String fileName) {
        String[] nameSplit = fileName.split("\\.");
        MimeType mimeType = null;
        if (1 < nameSplit.length) {
            mimeType = MimeType.getBySuffix(nameSplit[nameSplit.length - 1]);
        }
        return mimeType == null ? "" : mimeType.getType();
    }

    private static ScmUploadConf getScmUploadConf(BSONObject uploadConfig) {
        Boolean isOverwrite = BsonUtils.getBooleanOrElse(uploadConfig,
                CommonDefine.RestArg.FILE_IS_OVERWRITE, false);
        Boolean isNeedMd5 = BsonUtils.getBooleanOrElse(uploadConfig,
                CommonDefine.RestArg.FILE_IS_NEED_MD5, false);
        return new ScmUploadConf(isOverwrite, isNeedMd5);
    }
}
