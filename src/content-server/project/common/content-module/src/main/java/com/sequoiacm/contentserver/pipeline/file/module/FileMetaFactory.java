package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.tag.TagLibMgr;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class FileMetaFactory {
    @Autowired
    private TagLibMgr tagLibMgr;

    @Autowired
    private BucketInfoManager bucketInfoManager;

    public FileMeta createFileMetaByUserInfo(String ws, BSONObject userFileObject, String user,
            boolean checkProps) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws);
        if (userFileObject == null) {
            userFileObject = new BasicBSONObject();
        }

        FileMeta ret;
        if (!wsInfo.newVersionTag()) {
            ret = new FileMetaV1(bucketInfoManager);
        }
        else {
            ret = new FileMetaV2(tagLibMgr, bucketInfoManager, wsInfo);
        }
        ret.loadInfoFromUserInfo(ws, userFileObject, user, checkProps);
        return ret;
    }

    public FileMeta createFileMetaByRecord(String ws, BSONObject record) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws);
        FileMeta ret;
        if (!wsInfo.newVersionTag()) {
            ret = new FileMetaV1(bucketInfoManager);
        }
        else {
            ret = new FileMetaV2(tagLibMgr, bucketInfoManager, wsInfo);
        }

        ret.loadInfoFromRecord(record);
        return ret;
    }

    public FileMeta createDeleteMarker(String ws, String fileName, String user, long bucketId)
            throws ScmServerException {
        BSONObject obj = new BasicBSONObject(FieldName.FIELD_CLFILE_NAME, fileName);
        FileMeta ret = createFileMetaByUserInfo(ws, obj, user, false);
        ret.setBucketId(bucketId);
        ret.setDeleteMarker(true);
        Date createTime = new Date();
        ret.resetFileIdAndFileTime(ScmIdGenerator.FileId.get(createTime), createTime);
        return ret;
    }
}
