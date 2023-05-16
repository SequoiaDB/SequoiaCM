package com.sequoiacm.contentserver.pipeline.file.bucket;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddFileVersionBucketFilter implements Filter<AddFileVersionContext> {
    @Autowired
    private BucketInfoManager bucketInfoManager;

    @Override
    public PipelineResult executionPhase(AddFileVersionContext context) throws ScmServerException {
        // 文件若处于桶下，新增版本需要根据桶的版本控制状态，调整新增版本的版本号，并将桶关系表更新下，映射至本次新增的版本
        if (context.getNewVersion().getBucketId() == null) {
            return PipelineResult.success();
        }

        ScmBucket bucket = bucketInfoManager.getBucketById(context.getNewVersion().getBucketId());
        if (bucket == null) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                    "failed to create file version, bucket not exist: bucketId="
                            + context.getNewVersion().getBucketId() + ", fileName="
                            + context.getNewVersion().getName() + ", fileId="
                            + context.getFileId());
        }


        if (context.getNewVersion().getEtag() == null
                && !context.getNewVersion().isDeleteMarker()) {
            if (context.getNewVersion().getMd5() == null
                    || context.getNewVersion().getMd5().isEmpty()) {
                throw new ScmServerException(ScmError.SYSTEM_ERROR,
                        "failed to create file version, etag not exist: bucketId="
                                + context.getNewVersion().getBucketId() + ", fileName="
                                + context.getNewVersion().getName() + ", fileId="
                                + context.getFileId());
            }
            context.getNewVersion().setEtag(SignUtil.toHex(context.getNewVersion().getMd5()));
        }

        if (bucket.getVersionStatus() != ScmBucketVersionStatus.Enabled) {
            // 未开启版本控制，版本号需要置null，同时将真实的版本序号设置到 version serial
            context.getNewVersion().setVersionSerial(context.getNewVersion().getMajorVersion() + "."
                    + context.getNewVersion().getMinorVersion());
            context.getNewVersion().setMajorVersion(CommonDefine.File.NULL_VERSION_MAJOR);
            context.getNewVersion().setMinorVersion(CommonDefine.File.NULL_VERSION_MINOR);
            // 通知 core filter 删除老的null版本
            context.setShouldDeleteVersion(new ScmVersion(CommonDefine.File.NULL_VERSION_MAJOR,
                    CommonDefine.File.NULL_VERSION_MINOR));
        }

        try {
            BSONObject bucketFileUpdater = ScmMetaSourceHelper
                    .createBucketFileUpdatorByFileUpdator(context.getNewVersion().toRecordBSON());
            MetaAccessor bucketFileAccessor = bucket
                    .getFileTableAccessor(context.getTransactionContext());
            bucketFileAccessor.update(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME,
                            context.getNewVersion().getName()),
                    new BasicBSONObject(ScmMetaSourceHelper.SEQUOIADB_MODIFIER_SET,
                            bucketFileUpdater));
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to update bucket relation: bucket=" + bucket.getName() + ", fileName="
                            + context.getNewVersion().getName() + ", fileId=" + context.getFileId(),
                    e);
        }
        return PipelineResult.success();
    }
}
