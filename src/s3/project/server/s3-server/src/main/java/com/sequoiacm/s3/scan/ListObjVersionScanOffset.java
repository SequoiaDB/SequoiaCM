package com.sequoiacm.s3.scan;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.utils.VersionUtil;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListObjVersionScanOffset extends S3ScanOffset {
    private static final Logger logger = LoggerFactory.getLogger(ListObjVersionScanOffset.class);
    private ScmVersion scmVersionIdMarker;
    private String objKeyStartAfter;
    private boolean hasReach = false;
    private String commonPrefix;
    private IScmBucketService scmBucketService;
    private String bucketName;

    public ListObjVersionScanOffset(IScmBucketService scmBucketService, String bucketName,
            String objKeyStartAfter, String versionIdMarker, String commonPrefix)
            throws S3ServerException {
        if (objKeyStartAfter != null && objKeyStartAfter.length() <= 0) {
            objKeyStartAfter = null;
        }
        this.commonPrefix = commonPrefix;

        if (commonPrefix != null) {
            if (objKeyStartAfter == null) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "objKeyStartAfter must be not null when commonPrefix is not null: commonPrefix="
                                + commonPrefix);
            }
            if (!objKeyStartAfter.startsWith(commonPrefix)) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "objKeyStartAfter must start with commonPrefix: objKeyStartAfter="
                                + objKeyStartAfter + ", commonPrefix=" + commonPrefix);
            }
        }

        this.objKeyStartAfter = objKeyStartAfter;
        this.commonPrefix = commonPrefix;
        this.scmBucketService = scmBucketService;
        this.bucketName = bucketName;
        try {
            parseVersionMarker(objKeyStartAfter, versionIdMarker);
        }
        catch (ScmServerException e) {
            throw new S3ServerException(S3Error.OBJECT_LIST_VERSIONS_FAILED,
                    "failed to parse version id marker: bucket=" + bucketName
                            + ", objKeyStartAfter=" + objKeyStartAfter + ", versionIdMarker="
                            + versionIdMarker,
                    e);
        }
    }

    private void parseVersionMarker(String objKeyStartAfter, String versionIdMarker)
            throws ScmServerException, S3ServerException {
        if (versionIdMarker == null) {
            this.scmVersionIdMarker = new ScmVersion(Integer.MAX_VALUE, Integer.MAX_VALUE);
            return;
        }
        if (versionIdMarker.equals(S3CommonDefine.NULL_VERSION_ID)) {
            try {
                BSONObject file = scmBucketService.getFileNullMarkerVersion(bucketName,
                        objKeyStartAfter);
                this.scmVersionIdMarker = new ScmVersion(
                        (int) file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION),
                        (int) file.get(FieldName.FIELD_CLFILE_MINOR_VERSION));
            }
            catch (ScmServerException e) {
                if (e.getError() == ScmError.FILE_NOT_FOUND) {
                    logger.debug(
                            "null marker file not exist, ignore version id marker: bucket={}, keyMarker={}, versionIdMarker={}",
                            bucketName, objKeyStartAfter, versionIdMarker, e);
                    this.scmVersionIdMarker = new ScmVersion(Integer.MAX_VALUE, Integer.MAX_VALUE);
                }
                else {
                    throw e;
                }
            }
            return;
        }
        this.scmVersionIdMarker = VersionUtil.parseVersion(versionIdMarker);
    }

    @Override
    public boolean isReach(BSONObject record) {
        if (hasReach) {
            return true;
        }

        if (objKeyStartAfter == null) {
            hasReach = true;
            return true;
        }

        String keyName = (String) record.get(FieldName.BucketFile.FILE_NAME);
        if (commonPrefix != null) {
            if (isReachCommonPrefix(keyName, commonPrefix)) {
                hasReach = true;
                return true;
            }
            return false;
        }

        int keyCompareResult = objKeyStartAfter.compareTo(keyName);
        if (keyCompareResult > 0) {
            return false;
        }
        if (keyCompareResult == 0) {
            if (hasSpecifyVersionMarker()) {
                int majorVersion = (int) record.get(FieldName.BucketFile.FILE_MAJOR_VERSION);
                int minorVersion = (int) record.get(FieldName.BucketFile.FILE_MINOR_VERSION);
                if (!hasReachVersionMarker(majorVersion, minorVersion)) {
                    return false;
                }
                hasReach = true;
                return true;
            }
            return false;
        }
        hasReach = true;
        return true;
    }

    private boolean hasReachVersionMarker(int majorVersion, int minorVersion) {
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        if (version.compareTo(scmVersionIdMarker) >= 0) {
            return false;
        }
        return true;
    }

    @Override
    public BSONObject getOrderBy() {
        BasicBSONObject orderBy = new BasicBSONObject();
        orderBy.put(FieldName.BucketFile.FILE_NAME, 1);
        orderBy.put(FieldName.BucketFile.FILE_MAJOR_VERSION, -1);
        orderBy.put(FieldName.BucketFile.FILE_MINOR_VERSION, -1);
        return orderBy;
    }

    @Override
    public BSONObject getOptimizedMatcher() {
        BasicBSONObject majorKeyMatcher = new BasicBSONObject();
        if (objKeyStartAfter != null) {
            if (hasSpecifyVersionMarker()) {
                majorKeyMatcher.put("$gte", objKeyStartAfter);
            }
            else {
                majorKeyMatcher.put("$gt", objKeyStartAfter);
            }
            return new BasicBSONObject(FieldName.BucketFile.FILE_NAME, majorKeyMatcher);
        }
        return null;

    }

    boolean hasSpecifyVersionMarker() {
        return scmVersionIdMarker.getMajorVersion() != Integer.MAX_VALUE
                && scmVersionIdMarker.getMinorVersion() != Integer.MAX_VALUE;
    }

    @Override
    public S3ScanOffset createOffsetByRecord(BSONObject b, String commonPrefix)
            throws S3ServerException {
        return new ListObjVersionScanOffset(scmBucketService, bucketName,
                (String) b.get(FieldName.BucketFile.FILE_NAME),
                b.get(FieldName.BucketFile.FILE_MAJOR_VERSION) + "."
                        + b.get(FieldName.BucketFile.FILE_MINOR_VERSION),
                commonPrefix);
    }

    @Override
    public String toString() {
        return "ListObjVersionScanOffset{" + "versionIdMarker=" + scmVersionIdMarker
                + ", objKeyStartAfter='" + objKeyStartAfter + '\'' + ", hasReach=" + hasReach
                + ", commonPrefix='" + commonPrefix + '\'' + ", orderBy=" + getOrderBy()
                + ", optimizedMatcher=" + getOptimizedMatcher() + '}';
    }

    public String getObjKeyStartAfter() {
        return objKeyStartAfter;
    }

    public String getCommonPrefix() {
        return commonPrefix;
    }

    public String getVersionIdMarker() {
        return scmVersionIdMarker.getMajorVersion() + "." + scmVersionIdMarker.getMinorVersion();
    }
}
