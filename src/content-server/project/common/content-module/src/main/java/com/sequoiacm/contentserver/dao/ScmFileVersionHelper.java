package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.model.ScmVersion;
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

    // external_data 属性内，需要所有版本保持一致的字段
    private static final Set<String> EXTERNAL_DATA_UNIFIED_FIELD = new HashSet<>();
    static {
        EXTERNAL_DATA_UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_FILE_EXT_NAME_BEFORE_ATTACH);
    }

    public static boolean isUnifiedField(String field) {
        return UNIFIED_FIELD.contains(field);
    }

    public static void resetNewFileVersion(BSONObject newFileVersion,
            BSONObject latestFileVersion) throws ScmInvalidArgumentException {
        // 修正所有版本一致的属性
        for (String field : UNIFIED_FIELD) {
            newFileVersion.put(field, latestFileVersion.get(field));
        }
        BSONObject latestFileExt = BsonUtils.getBSON(latestFileVersion,
                FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        if (latestFileExt != null) {
            BSONObject newVersionFileExt = BsonUtils.getBSON(newFileVersion,
                    FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
            if (newVersionFileExt == null) {
                newVersionFileExt = new BasicBSONObject();
                newFileVersion.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA, newVersionFileExt);
            }
            for (String field : EXTERNAL_DATA_UNIFIED_FIELD) {
                newVersionFileExt.put(field, latestFileExt.get(field));
            }
        }

        // 生成新版本的版本号
        ScmVersion newVersion = genNewVersionByFile(latestFileVersion);
        newFileVersion.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, newVersion.getMajorVersion());
        newFileVersion.put(FieldName.FIELD_CLFILE_MINOR_VERSION, newVersion.getMinorVersion());
    }

    public static ScmVersion genNewVersionByFile(BSONObject currentFileVersion) {
        int majorVersion = BsonUtils
                .getNumberChecked(currentFileVersion, FieldName.FIELD_CLFILE_MAJOR_VERSION)
                .intValue();
        int minorVersion = BsonUtils
                .getNumberChecked(currentFileVersion, FieldName.FIELD_CLFILE_MINOR_VERSION)
                .intValue();
        if (majorVersion == CommonDefine.File.NULL_VERSION_MAJOR
                && minorVersion == CommonDefine.File.NULL_VERSION_MINOR) {
            String versionSerial = BsonUtils.getStringChecked(currentFileVersion,
                    FieldName.FIELD_CLFILE_VERSION_SERIAL);
            ScmVersion version = ScmFileVersionHelper.parseVersionSerial(versionSerial);
            return new ScmVersion(version.getMajorVersion() + 1, 0);
        }
        else {
            return new ScmVersion(majorVersion + 1, 0);
        }
    }

    public static BSONObject deleteNullVersionInHistory(ScmWorkspaceInfo ws, String fileId,
            TransactionContext transactionContext, BSONObject latestVersion)
            throws ScmMetasourceException, ScmServerException {
        MetaFileHistoryAccessor accessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource()
                .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), transactionContext);
        BSONObject deleter = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(deleter, fileId);
        deleter.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, CommonDefine.File.NULL_VERSION_MAJOR);
        deleter.put(FieldName.FIELD_CLFILE_MINOR_VERSION, CommonDefine.File.NULL_VERSION_MINOR);
        return accessor.queryAndDelete(deleter, null, latestVersion);
    }

    public static ScmVersion parseVersionSerial(String versionSerial) {
        String[] versionArr = versionSerial.split("\\.");
        if (versionArr.length != 2) {
            throw new IllegalArgumentException("invalid version serial :" + versionSerial);
        }
        return new ScmVersion(Integer.parseInt(versionArr[0]), Integer.parseInt(versionArr[1]));
    }

    public static boolean isSpecifiedVersion(BSONObject fileVersion, int expectMajorVersion,
            int expectMinorVersion) {
        Number major = BsonUtils.getNumberChecked(fileVersion,
                FieldName.FIELD_CLFILE_MAJOR_VERSION);
        if (major.intValue() != expectMajorVersion) {
            return false;
        }

        Number minor = BsonUtils.getNumberChecked(fileVersion,
                FieldName.FIELD_CLFILE_MINOR_VERSION);
        if (minor.intValue() != expectMinorVersion) {
            return false;
        }
        return true;
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
