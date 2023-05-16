package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.FileTableCreator;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import static com.sequoiacm.contentserver.model.FileFieldExtraDefine.EXTERNAL_DATA_UNIFIED_FIELD;
import static com.sequoiacm.contentserver.model.FileFieldExtraDefine.UNIFIED_FIELD;

public class ScmFileVersionHelper {

    public static boolean isUnifiedField(String field) {
        if (UNIFIED_FIELD.contains(field)) {
            return true;
        }
        if (field.startsWith(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + ".")) {
            String extEmbedField = field
                    .substring((FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + ".").length());
            return EXTERNAL_DATA_UNIFIED_FIELD.contains(extEmbedField);
        }
        return false;
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

    public static BSONObject deleteVersionInHistory(ScmWorkspaceInfo ws, String fileId,
            ScmVersion fileVersion, TransactionContext transactionContext, BSONObject latestVersion)
            throws ScmMetasourceException, ScmServerException {
        MetaFileHistoryAccessor accessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource()
                .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), transactionContext);
        BSONObject additionalMatcher = new BasicBSONObject();
        additionalMatcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, fileVersion.getMajorVersion());
        additionalMatcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, fileVersion.getMinorVersion());
        return accessor.queryAndDelete(fileId, latestVersion, additionalMatcher, null);
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

    public static void insertVersionToHistory(ScmWorkspaceInfo ws, FileMeta latestFileVersion,
            TransactionContext transactionContext)
            throws ScmMetasourceException, ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        try {
            MetaFileHistoryAccessor accessor = contentModule.getMetaService().getMetaSource()
                    .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), transactionContext);
            accessor.insert(latestFileVersion.toRecordBSON());
        }
        catch (ScmMetasourceException e) {
            // 捕获下写历史表产生子表不存在的异常，然后执行下子表创建及挂载
            if (e.getScmError() == ScmError.FILE_TABLE_NOT_FOUND) {

                try {
                    FileTableCreator.createSubFileTable(
                            (SdbMetaSource) contentModule.getMetaService().getMetaSource(), ws,
                            latestFileVersion.toRecordBSON());
                }
                catch (Exception ex) {
                    throw new ScmServerException(ScmError.METASOURCE_ERROR,
                            "insert file failed, create file table failed:ws=" + ws.getName()
                                    + ", file=" + latestFileVersion.getId(),
                            ex);
                }
                insertVersionToHistory(ws, latestFileVersion, transactionContext);
                return;
            }
            throw e;
        }
    }

    public static boolean isLatestVersion(ScmVersion version, ScmVersion latestVersion) {
        if (version.equals(latestVersion)) {
            return true;
        }
        if (version.getMajorVersion() == -1 && version.getMinorVersion() == -1) {
            return true;
        }
        return false;
    }
}
