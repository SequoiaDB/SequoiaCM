package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.HashSet;
import java.util.Set;

public class ScmFileVersionHelper {
    private static final Set<String> UNIFIED_FIELD = new HashSet<>();
    static {
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_NAME);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_BATCH_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_INNER_USER);
    }

    public static boolean isUnifiedField(String field) {
        return UNIFIED_FIELD.contains(field);
    }

    public static void resetNewFileVersion(BSONObject newFileVersion,
            BSONObject latestFileVersion) {
        // 修正所有版本一致的属性
        for (String field : UNIFIED_FIELD) {
            newFileVersion.put(field, latestFileVersion.get(field));
        }

        newFileVersion.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, BsonUtils
                .getIntegerChecked(latestFileVersion, FieldName.FIELD_CLFILE_MAJOR_VERSION) + 1);
        newFileVersion.put(FieldName.FIELD_CLFILE_MINOR_VERSION, 0);
    }

    public static BSONObject deleteNullMarkerInHistory(ScmWorkspaceInfo ws, String fileId,
            TransactionContext transactionContext, BSONObject latestVersion)
            throws ScmMetasourceException, ScmServerException {
        MetaFileHistoryAccessor accessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource()
                .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), transactionContext);
        BSONObject deleter = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(deleter, fileId);
        deleter.put(FieldName.FIELD_CLFILE_NULL_MARKER, true);
        return accessor.queryAndDelete(deleter, null, latestVersion);
    }

    public static boolean fileVersionHasNullMarker(BSONObject fileVersion) {
        return BsonUtils.getBooleanOrElse(fileVersion, FieldName.FIELD_CLFILE_NULL_MARKER, false);
    }

    public static void updateLatestVersionAsNewVersion(ScmWorkspaceInfo ws, String fileId,
            BSONObject newFileVersion, BSONObject latestFileVersion,
            TransactionContext transactionContext)
            throws ScmMetasourceException, ScmServerException {
        MetaFileAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                .getFileAccessor(ws.getMetaLocation(), ws.getName(), transactionContext);
        BSONObject matcher = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
        accessor.update(matcher, new BasicBSONObject("$set", newFileVersion));

        ScmFileOperateUtils.updateBucketFileForUpdateFile(newFileVersion, latestFileVersion,
                transactionContext);
        ScmFileOperateUtils.updateFileRelForUpdateFile(ws, fileId, latestFileVersion, newFileVersion,
                transactionContext);
    }

    // return old
    public static BSONObject updateLatestVersionAndRel(ScmWorkspaceInfo ws, String fileId,
            BSONObject updater, TransactionContext transactionContext)
            throws ScmMetasourceException, ScmServerException {
        return updateLatestVersionAndRel(ws, fileId, null, updater, transactionContext);
    }

    // return old
    public static BSONObject updateLatestVersionAndRel(ScmWorkspaceInfo ws, String fileId,
            BSONObject additionMatcher, BSONObject updater, TransactionContext transactionContext)
            throws ScmMetasourceException, ScmServerException {
        MetaFileAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                .getFileAccessor(ws.getMetaLocation(), ws.getName(), transactionContext);
        BSONObject fileIdMatcher = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(fileIdMatcher, fileId);

        BasicBSONList andArr = new BasicBSONList();
        andArr.add(fileIdMatcher);
        if (additionMatcher != null) {
            andArr.add(additionMatcher);
        }
        BasicBSONObject matcher = new BasicBSONObject().append("$and", andArr);

        BSONObject oldFileVersion = accessor.queryAndUpdate(matcher,
                new BasicBSONObject("$set", updater), null, false);
        if (oldFileVersion == null) {
            return null;
        }


        ScmFileOperateUtils.updateBucketFileForUpdateFile(updater, oldFileVersion,
                transactionContext);
        ScmFileOperateUtils.updateFileRelForUpdateFile(ws, fileId, oldFileVersion, updater,
                transactionContext);
        return oldFileVersion;
    }

    public static void insertVersionToHistory(ScmWorkspaceInfo ws, BSONObject latestFileVersion,
            TransactionContext transactionContext)
            throws ScmMetasourceException, ScmServerException {
        MetaFileHistoryAccessor accessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource()
                .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), transactionContext);
        accessor.insert(latestFileVersion);
    }
}
