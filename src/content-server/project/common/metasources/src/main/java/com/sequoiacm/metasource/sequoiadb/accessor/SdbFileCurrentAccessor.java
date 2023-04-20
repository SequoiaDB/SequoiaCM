package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import com.sequoiadb.exception.SDBError;

public class SdbFileCurrentAccessor extends SdbFileBaseAccessor implements MetaFileAccessor{
    private static final Logger logger = LoggerFactory.getLogger(SdbFileCurrentAccessor.class);

    public SdbFileCurrentAccessor(SdbMetaSourceLocation location, SdbMetaSource metasource,
            String csName, String clName, TransactionContext context) {
        super(location, metasource, csName, clName, context);
    }

    @Override
    public void insert(BSONObject insertor) throws ScmMetasourceException {
        super.insert(insertor);
    }

    @Override
    public BSONObject updateFileInfo(String fileId, int majorVersion, int minorVersion,
            BSONObject newFileInfo) throws SdbMetasourceException {
        try {
            BSONObject updator = new BasicBSONObject("$set", newFileInfo);
            BSONObject matcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            if (majorVersion != -1 && minorVersion != -1) {
                matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
                matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            }
            return queryAndUpdate(matcher, updator, null);
        }
        catch (SdbMetasourceException e) {
            logger.error("updateFileInfo failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
            + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "updateFileInfo failed:table=" + getCsName() + "." + getClName() + ",fileId="
                            + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion,
                            e);
        }
    }

    @Override
    public BSONObject updateFileInfo(String fileId, int majorVersion, int minorVersion,
            BSONObject newFileInfo, BSONObject matcher) throws SdbMetasourceException {
        try {
            BSONObject updator = new BasicBSONObject("$set", newFileInfo);
            if (null == matcher) {
                matcher = new BasicBSONObject();
            }
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            if (majorVersion != -1 && minorVersion != -1) {
                matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
                matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            }
            return queryAndUpdate(matcher, updator, null);
        }
        catch (SdbMetasourceException e) {
            logger.error("updateFileInfo failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
            + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "updateFileInfo failed:table=" + getCsName() + "." + getClName() + ",fileId="
                            + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion,
                            e);
        }
    }

    @Override
    public boolean updateTransId(String fileId, int majorVersion, int minorVersion, int status,
            String transId) throws SdbMetasourceException {
        try {
            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.FIELD_CLFILE_EXTRA_STATUS, status);
            newValue.put(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID, transId);
            BSONObject updator = new BasicBSONObject();
            updator.put("$set", newValue);

            BSONObject matcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            matcher.put(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID, "");

            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("updateTransId failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
            + minorVersion + ",transId=" + transId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "updateTransId failed:table=" + getCsName() + "." + getClName() + ",fileId="
                            + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion + ",transId=" + transId,
                            e);
        }
    }

    @Override
    public void unmarkTransId(String fileId, int majorVersion, int minorVersion, int status)
            throws SdbMetasourceException {
        try {
            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.FIELD_CLFILE_EXTRA_STATUS, status);
            newValue.put(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID, "");
            BSONObject updator = new BasicBSONObject();
            updator.put("$set", newValue);

            BSONObject matcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);

            updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("unmarkTransId failed:table=" + getCsName() + "." + getClName()
            + ",fileId=" + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
            + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "unmarkTransId failed:table=" + getCsName() + "." + getClName() + ",fileId="
                            + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion,
                            e);
        }
    }

    @Override
    public boolean isIndexFieldExist(String fieldName) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            return SequoiadbHelper.isIndexFieldExist(sdb, fieldName, getCsName(), getClName());
        }
        finally {
            if (sdb != null) {
                releaseConnection(sdb);
            }
        }
    }
}
