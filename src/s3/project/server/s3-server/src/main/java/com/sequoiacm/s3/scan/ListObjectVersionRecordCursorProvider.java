package com.sequoiacm.s3.scan;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
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
    private MetaAccessor bucketFileAccessor;
    private MetaFileHistoryAccessor historyFileAccessor;

    public ListObjectVersionRecordCursorProvider(ScmBucket bucket)
            throws ScmServerException, ScmMetasourceException {
        ContentModuleMetaSource metasource = ScmContentModule.getInstance().getMetaService()
                .getMetaSource();
        ScmWorkspaceInfo ws = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(bucket.getWorkspace());
        historyFileAccessor = metasource.getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(),
                null);
        bucketFileAccessor = bucket.getFileTableAccessor(null);
        this.bucket = bucket;
    }

    @Override
    public RecordWrapperCursor<ListVersionRecordWrapper> createRecordCursor(BSONObject matcher,
                                                                            BSONObject orderby) throws ScmMetasourceException {
        MetaCursor bucketFileCursor = bucketFileAccessor.query(matcher, null, orderby);
        try {
            BasicBSONList andArr = new BasicBSONList();
            if (matcher != null) {
                andArr.add(matcher);
            }
            andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, bucket.getId()));
            MetaCursor historyFileCursor = historyFileAccessor
                    .query(new BasicBSONObject("$and", andArr), orderby);
            return new ListVersionCursor(bucketFileCursor, historyFileCursor);
        }
        catch (Exception e) {
            bucketFileCursor.close();
            throw e;
        }
    }
}

class ListVersionCursor implements RecordWrapperCursor<ListVersionRecordWrapper> {
    private MetaCursor bucketFileCursor;
    private MetaCursor historyCursor;
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

        int historyRecMajorVersion = (int) historyRecordWrapper.getRecord().get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int historyRecMinorVersion = (int) historyRecordWrapper.getRecord().get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        ScmVersion historyRecVersion = new ScmVersion(historyRecMajorVersion,
                historyRecMinorVersion);
        int bucketRecMajorVersion = (int) bucketFileRecordWrapper
                .getRecord().get(FieldName.BucketFile.FILE_MAJOR_VERSION);
        int bucketRecMinorVersion = (int) bucketFileRecordWrapper
                .getRecord().get(FieldName.BucketFile.FILE_MINOR_VERSION);
        ScmVersion bucketRecVersion = new ScmVersion(bucketRecMajorVersion, bucketRecMinorVersion);

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

    @Override
    public void close() {
        historyCursor.close();
        bucketFileCursor.close();
    }
}
