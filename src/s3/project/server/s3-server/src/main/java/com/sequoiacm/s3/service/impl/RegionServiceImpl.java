package com.sequoiacm.s3.service.impl;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.service.RegionService;

@Component
public class RegionServiceImpl implements RegionService {
    private static final Logger logger = LoggerFactory.getLogger(RegionServiceImpl.class);

    @Autowired
    private MetaSourceService metaSourceService;

    @Override
    public void setDefaultRegion(String ws) throws ScmServerException {
        try {
            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfo(ws);
            if (wsInfo == null) {
                throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                        "set default region failed, workspace not exist:" + ws);
            }
            if (wsInfo.isEnableDirectory()) {
                throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                        "set default region failed, please disable workspace directory feature first:"
                                + ws);
            }
            MetaAccessor accessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.DEFAULT_REGION_TABLE_NAME);
            BSONObject set = new BasicBSONObject(S3CommonDefine.DEFAULT_REGION_FIELD_WORKSPACE, ws);
            accessor.upsert(null, new BasicBSONObject("$set", set));
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to set default region: ws=" + ws,
                    e);
        }
    }

    @Override
    public String getDefaultRegionForScm() throws ScmServerException {
        try {
            String region = getDefaultRegionInternal();
            if (region == null) {
                throw new ScmServerException(ScmError.S3_REGION_NOT_EXIST, "no default region");
            }
            return region;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to get default region", e);
        }
    }

    private String getDefaultRegionInternal() throws ScmServerException, ScmMetasourceException {
        MetaAccessor accessor = metaSourceService.getMetaSource()
                .createMetaAccessor(S3CommonDefine.DEFAULT_REGION_TABLE_NAME);
        BSONObject record = accessor.queryOne(null, null, null);
        if (record != null) {
            return BsonUtils.getStringChecked(record,
                    S3CommonDefine.DEFAULT_REGION_FIELD_WORKSPACE);
        }
        return null;
    }

    @Override
    public String getDefaultRegionForS3() throws S3ServerException {
        try {
            String region = getDefaultRegionInternal();
            if (region == null) {
                throw new S3ServerException(S3Error.REGION_NO_SUCH_REGION, "no default region");
            }
            return region;
        }
        catch (ScmMetasourceException | ScmServerException e) {
            throw new S3ServerException(S3Error.SYSTEM_ERROR, "failed to get default region", e);
        }
    }

}
