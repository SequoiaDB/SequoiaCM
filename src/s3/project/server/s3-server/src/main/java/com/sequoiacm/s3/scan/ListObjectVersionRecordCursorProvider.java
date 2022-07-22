package com.sequoiacm.s3.scan;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.IndexName;
import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

public class ListObjectVersionRecordCursorProvider implements S3ScanRecordCursorProvider {
    private final ScmBucket bucket;
    private final MetaAccessor bucketFileAccessor;
    private final MetaFileHistoryAccessor historyFileAccessor;

    public ListObjectVersionRecordCursorProvider(ScmBucket bucket)
            throws ScmServerException, ScmMetasourceException {
        ContentModuleMetaSource metasource = ScmContentModule.getInstance().getMetaService()
                .getMetaSource();
        ScmWorkspaceInfo ws = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        historyFileAccessor = metasource.getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(),
                null);
        bucketFileAccessor = bucket.getFileTableAccessor(null);
        this.bucket = bucket;
    }

    @Override
    public RecordWrapperCursor<ListVersionRecordWrapper> createRecordCursor(BSONObject matcher,
                                                                            BSONObject orderby) throws ScmMetasourceException {
        BSONObject bucketFileHint = new BasicBSONObject();
        bucketFileHint.put("", IndexName.BucketFile.FILE_NAME_UNIQUE_IDX);
        MetaCursor bucketFileCursor = bucketFileAccessor.query(matcher, null, orderby,
                bucketFileHint, 0, -1, 0);
        try {
            BasicBSONList andArr = new BasicBSONList();
            if (matcher != null) {
                andArr.add(matcher);
            }
            andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, bucket.getId()));

            BSONObject historyFileHint = new BasicBSONObject();
            historyFileHint.put("", IndexName.HistoryFile.NAME_VERSION_UNION_IDX);

            BasicBSONObject historyOrderBy = new BasicBSONObject();
            historyOrderBy.put(FieldName.BucketFile.FILE_NAME, 1);
            historyOrderBy.put(FieldName.BucketFile.FILE_VERSION_SERIAL, -1);
            historyOrderBy.put(FieldName.BucketFile.FILE_MAJOR_VERSION, -1);
            historyOrderBy.put(FieldName.BucketFile.FILE_MINOR_VERSION, -1);

            MetaCursor historyFileCursor = historyFileAccessor
                    .query(new BasicBSONObject("$and", andArr), historyOrderBy, historyFileHint, 0,
                            -1);

            return new ListVersionCursor(bucketFileCursor,
                    new HistoryCursorWrapper(historyFileCursor));
        }
        catch (Exception e) {
            bucketFileCursor.close();
            throw e;
        }
    }
}

class StackStyleAccessCursor {
    private final MetaCursor cursor;
    private BSONObject current;

    public StackStyleAccessCursor(MetaCursor cursor) {
        this.cursor = cursor;
    }

    public BSONObject peek() throws ScmMetasourceException {
        if (current != null) {
            return current;
        }

        current = cursor.getNext();
        return current;
    }

    public BSONObject pop() throws ScmMetasourceException {
        if (current != null) {
            BSONObject tmp = current;
            current = null;
            return tmp;
        }

        return cursor.getNext();
    }

    public void close() {
        current = null;
        cursor.close();
    }
}

class HistoryCursorWrapper implements MetaCursor {
    private final StackStyleAccessCursor historyCursor;

    private BSONObject nullVersionRecord;
    private ScmVersion nullVersionRecordVersionSerial;

    public HistoryCursorWrapper(MetaCursor historyCursor) {
        this.historyCursor = new StackStyleAccessCursor(historyCursor);
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return historyCursor.peek() != null || nullVersionRecord != null;
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        if (nullVersionRecord == null) {
            BSONObject record = historyCursor.pop();
            if (record == null) {
                return null;
            }
            if (!isNullVersion(record)) {
                return record;
            }
            nullVersionRecord = record;
            nullVersionRecordVersionSerial = ScmFileVersionHelper.parseVersionSerial(BsonUtils
                    .getStringChecked(nullVersionRecord, FieldName.FIELD_CLFILE_VERSION_SERIAL));
        }

        BSONObject next = historyCursor.peek();
        if (isFrontNullVersion(next)) {
            historyCursor.pop();
            return next;
        }

        BSONObject tmp = nullVersionRecord;
        nullVersionRecordVersionSerial = null;
        nullVersionRecord = null;
        return tmp;
    }

    private boolean isFrontNullVersion(BSONObject record) {
        if (record == null) {
            return false;
        }
        if (!isFileIdSameAsNullVersion(record)) {
            return false;
        }

        return isFileVersionGreatThanNullVersion(record);
    }

    private boolean isFileIdSameAsNullVersion(BSONObject record) {
        return record.get(FieldName.FIELD_CLFILE_ID)
                .equals(nullVersionRecord.get(FieldName.FIELD_CLFILE_ID));
    }

    private boolean isFileVersionGreatThanNullVersion(BSONObject file) {
        int major = BsonUtils.getNumberChecked(file, FieldName.FIELD_CLFILE_MAJOR_VERSION)
                .intValue();
        int minor = BsonUtils.getNumberChecked(file, FieldName.FIELD_CLFILE_MINOR_VERSION)
                .intValue();
        ScmVersion fileVersion = new ScmVersion(major, minor);
        int res = fileVersion.compareTo(nullVersionRecordVersionSerial);
        return res > 0;
    }

    private boolean isNullVersion(BSONObject record) {
        return ScmFileVersionHelper.isSpecifiedVersion(record, CommonDefine.File.NULL_VERSION_MAJOR,
                CommonDefine.File.NULL_VERSION_MINOR);
    }

    @Override
    public void close() {
        nullVersionRecord = null;
        nullVersionRecordVersionSerial = null;
        historyCursor.close();
    }
}

class ListVersionCursor implements RecordWrapperCursor<ListVersionRecordWrapper> {
    private final MetaCursor bucketFileCursor;
    private final MetaCursor historyCursor;
    private ListVersionRecordWrapper historyRecordWrapper;
    private ListVersionRecordWrapper bucketFileRecordWrapper;

    public ListVersionCursor(MetaCursor bucketFileCursor, MetaCursor historyCursor) {
        this.bucketFileCursor = bucketFileCursor;
        this.historyCursor = historyCursor;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        if (historyRecordWrapper != null || bucketFileRecordWrapper != null) {
            return true;
        }
        return bucketFileCursor.hasNext() || historyCursor.hasNext();
    }

    private ListVersionRecordWrapper getHistoryRecordAndSetNull() {
        ListVersionRecordWrapper tmp = historyRecordWrapper;
        historyRecordWrapper = null;
        return tmp;
    }

    private ListVersionRecordWrapper getBucketFileRecordAndSetNull() {
        ListVersionRecordWrapper tmp = bucketFileRecordWrapper;
        bucketFileRecordWrapper = null;
        return tmp;
    }

    private void fetchHistoryRecord() throws ScmMetasourceException {
        BSONObject rec = historyCursor.getNext();
        if (rec != null) {
            historyRecordWrapper = new ListVersionRecordWrapper(rec, false);
        }
        else {
            historyRecordWrapper = null;
        }
    }

    private void fetchBucketFileRecord() throws ScmMetasourceException {
        BSONObject rec = bucketFileCursor.getNext();
        if (rec != null) {
            bucketFileRecordWrapper = new ListVersionRecordWrapper(rec, true);
        }
        else {
            bucketFileRecordWrapper = null;
        }
    }

    @Override
    public ListVersionRecordWrapper getNext() throws ScmMetasourceException {
        if (historyRecordWrapper == null) {
            fetchHistoryRecord();
        }
        if (bucketFileRecordWrapper == null) {
            fetchBucketFileRecord();
        }

        if (bucketFileRecordWrapper == null) {
            return getHistoryRecordAndSetNull();
        }
        if (historyRecordWrapper == null) {
            return getBucketFileRecordAndSetNull();
        }

        String historyRecFileName = (String) historyRecordWrapper.getRecord().get(FieldName.FIELD_CLFILE_NAME);
        String bucketRecFileName = (String) bucketFileRecordWrapper.getRecord().get(FieldName.BucketFile.FILE_NAME);
        int fileNameCmpRes = historyRecFileName.compareTo(bucketRecFileName);
        if (fileNameCmpRes > 0) {
            return getBucketFileRecordAndSetNull();
        }
        if (fileNameCmpRes < 0) {
            return getHistoryRecordAndSetNull();
        }

        ScmVersion historyRecVersion = getVersionFromRecord(historyRecordWrapper.getRecord());
        ScmVersion bucketRecVersion = getVersionFromRecord(bucketFileRecordWrapper.getRecord());

        int fileVersionCmpRes = historyRecVersion.compareTo(bucketRecVersion);
        if (fileVersionCmpRes > 0) {
            return getHistoryRecordAndSetNull();
        }
        else if (fileVersionCmpRes < 0) {
            return getBucketFileRecordAndSetNull();
        }
        historyRecordWrapper = null;
        return getBucketFileRecordAndSetNull();
    }

    private ScmVersion getVersionFromRecord(BSONObject record) {
        if (ScmFileVersionHelper.isSpecifiedVersion(record, CommonDefine.File.NULL_VERSION_MAJOR,
                CommonDefine.File.NULL_VERSION_MINOR)) {
            return ScmFileVersionHelper.parseVersionSerial(
                    BsonUtils.getStringChecked(record, FieldName.FIELD_CLFILE_VERSION_SERIAL));
        }
        int major = BsonUtils.getNumberChecked(record, FieldName.FIELD_CLFILE_MAJOR_VERSION)
                .intValue();
        int minor = BsonUtils.getNumberChecked(record, FieldName.FIELD_CLFILE_MINOR_VERSION)
                .intValue();
        return new ScmVersion(major, minor);
    }

    @Override
    public void close() {
        historyCursor.close();
        bucketFileCursor.close();
    }
}
