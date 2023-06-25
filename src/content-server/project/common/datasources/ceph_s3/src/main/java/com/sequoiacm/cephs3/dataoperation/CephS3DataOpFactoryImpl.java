package com.sequoiacm.cephs3.dataoperation;

import static com.sequoiacm.metasource.MetaSourceDefine.CsName.CS_SCMSYSTEM;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sequoiacm.infrastructure.common.ScmIdParser;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3BreakpointFileContext;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataInfoFetcher;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.IndexDef;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.MetaSourceDefine;
import com.sequoiacm.metasource.ScmMetasourceException;

public class CephS3DataOpFactoryImpl implements ScmDataOpFactory {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataOpFactoryImpl.class);

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo, ScmDataWriterContext writerContext)
            throws ScmDatasourceException {
        CephS3DataLocation dataLocation = (CephS3DataLocation) location;

        String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
        BucketNameOption bucketNameOption;
        if (!Strings.isNullOrEmpty(dataLocation.getUserBucketName())) {
            bucketNameOption = BucketNameOption
                    .fixedBucketNameOption(dataLocation.getUserBucketName());
        }
        else {
            bucketNameOption = BucketNameOption.ruleBucketNameOption(wsName,
                    dataLocation.getBucketName(wsName, dataInfo.getCreateTime(), timezone));
        }

        try {
            return new CephS3DataWriterImpl(bucketNameOption,
                    dataLocation.getObjectId(dataInfo.getId(), wsName, dataInfo.getCreateTime(),
                            timezone),
                    service, wsName, dataLocation, siteId, writerContext);
        }
        catch (CephS3Exception e) {
            logger.error("build ceph s3 writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            if (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)) {
                throw new CephS3Exception(e.getS3StatusCode(), e.getS3ErrorCode(),
                        "bucket is not exist:" + bucketNameOption,
                        ScmError.STORE_SPACE_IS_NOT_EXIST, e);
            }
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph s3 writer failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephS3Exception("build ceph s3 writer failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws ScmDatasourceException {
        return createWriter(siteId, wsName, location, service, dataInfo,
                new ScmDataWriterContext());
    }

    @Override
    public ScmDataReader createReader(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephS3Exception {
        try {
            CephS3DataLocation dataLocation = (CephS3DataLocation) location;
            String bucketName = dataInfo.getTableName();
            String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
            if (Strings.isNullOrEmpty(bucketName)) {
                bucketName = dataLocation.getBucketName(wsName, dataInfo.getCreateTime(), timezone);
            }
            return new CephS3DataReaderImpl(bucketName,
                    dataLocation.getObjectId(dataInfo.getId(), wsName, dataInfo.getCreateTime(),
                            timezone),
                    service, dataLocation);
        }
        catch (CephS3Exception e) {
            logger.error("build ceph s3 reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph s3 reader failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephS3Exception("build ceph s3 reader failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws CephS3Exception {
        try {
            CephS3DataLocation dataLocation = (CephS3DataLocation) location;
            String bucketName = dataInfo.getTableName();
            String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
            if (Strings.isNullOrEmpty(bucketName)) {
                bucketName = dataLocation.getBucketName(wsName, dataInfo.getCreateTime(), timezone);
            }
            return new CephS3DataDeletorImpl(bucketName,
                    dataLocation.getObjectId(dataInfo.getId(), wsName, dataInfo.getCreateTime(),
                            timezone),
                    service, dataLocation);
        }
        catch (CephS3Exception e) {
            logger.error("build ceph s3 deletor failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph s3 deletor failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw new CephS3Exception("build ceph s3 deletor failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }

    @Override
    public ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext, ScmDataWriterContext writerContext)
            throws ScmDatasourceException {
        CephS3DataLocation dataLocation = (CephS3DataLocation) location;
        BucketNameOption bucketNameOption;
        String timezone = ScmIdParser.getTimezoneName(dataId);
        if (!Strings.isNullOrEmpty(dataLocation.getUserBucketName())) {
            bucketNameOption = BucketNameOption
                    .fixedBucketNameOption(dataLocation.getUserBucketName());
        }
        else {
            bucketNameOption = BucketNameOption
                    .ruleBucketNameOption(wsName,
                            dataLocation.getBucketName(wsName, createTime, timezone));
        }
        return new CephS3BreakpointDataWriter(bucketNameOption,
                dataLocation.getObjectId(dataId, wsName, createTime, timezone),
                new CephS3BreakpointFileContext(extraContext), service, writeOffset, dataLocation,
                wsName, writerContext);
    }

    @Override
    public ScmSeekableDataWriter createSeekableDataWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, ScmDataInfo dataInfo, boolean createData,
            long writeOffset, BSONObject extraContext) throws ScmDatasourceException {
        throw new CephS3Exception(CephS3Exception.ERR_CODE_OPERATION_UNSUPPORTED,
                "do not support seekable upload");
    }

    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service, ScmLocation location, String wsName)
            throws ScmDatasourceException {
        if (Strings.isNullOrEmpty(wsName) || location == null) {
            throw new CephS3Exception("build ceph s3 data table deletor failed:wsName=" + wsName);
        }
        CephS3DataLocation dataLocation = (CephS3DataLocation) location;
        return new CephS3DataTableDeletor(dataLocation, service, tableNames, wsName);
    }
    @Override
    public ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException {
        throw new CephS3Exception(CephS3Exception.ERR_CODE_OPERATION_UNSUPPORTED,"Unable to process");
    }

    @Override
    public void init(int siteId, MetaSource metaSource, ScmLockManager lockManager)
            throws ScmDatasourceException {
        try {
            MetaAccessor metaAccessor = metaSource.createMetaAccessor(
                    CS_SCMSYSTEM + "." + MetaSourceDefine.SystemClName.CL_DATA_TABLE_NAME_ACTIVE);
            IndexDef indexDef = new IndexDef();
            indexDef.setUnique(true);
            indexDef.setUnionKeys(Arrays.asList(FieldName.FIELD_CLACTIVE_WORKSPACE_NAME,
                    FieldName.FIELD_CLACTIVE_SITE_ID, FieldName.FIELD_CLACTIVE_RULE_TABLE_NAME));
            metaAccessor.ensureTable(Arrays.asList(indexDef));
            CephS3BucketManager.getInstance().init(siteId, metaSource, lockManager);
        }
        catch (ScmMetasourceException e) {
            throw new ScmDatasourceException("failed to init cephs3 op factory", e);
        }

    }

    @Override
    public ScmDataInfoFetcher createDataInfoFetcher(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws ScmDatasourceException {
        try {
            CephS3DataLocation dataLocation = (CephS3DataLocation) location;
            String bucketName = dataInfo.getTableName();
            String timezone = ScmIdParser.getTimezoneName(dataInfo.getId());
            if (Strings.isNullOrEmpty(bucketName)) {
                bucketName = dataLocation.getBucketName(wsName, dataInfo.getCreateTime(), timezone);
            }
            return new CephS3DataInfoFetcher(bucketName,
                    dataLocation.getObjectId(dataInfo.getId(), wsName, dataInfo.getCreateTime(),
                            timezone),
                    service, dataLocation);
        }
        catch (CephS3Exception e) {
            logger.error("build CephS3DataInfoFetcher failed:siteId=" + siteId + ",wsName=" + wsName
                    + ",fileId=" + dataInfo.getId());
            throw e;
        }
        catch (Exception e) {
            logger.error("build ceph CephS3DataInfoFetcher failed:siteId=" + siteId + ",wsName="
                    + wsName + ",fileId=" + dataInfo.getId());
            throw new CephS3Exception("build CephS3DataInfoFetcher reader failed:siteId=" + siteId
                    + ",wsName=" + wsName + ",fileId=" + dataInfo.getId(), e);
        }
    }
}
