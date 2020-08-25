package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import com.sequoiadb.exception.SDBError;

public class SdbFileHistoryAccessor extends SdbFileBaseAccessor implements MetaFileHistoryAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbFileHistoryAccessor.class);

    public SdbFileHistoryAccessor(SdbMetaSourceLocation location, SdbMetaSource metasource,
            String csName, String clName, TransactionContext context) {
        super(location, metasource, csName, clName, context);
    }

    @Override
    public void delete(String fileId) throws ScmMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(deletor, fileId);
            super.delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete history file failed:table={}.{},fileId={}", getCsName(),
                    getClName(), fileId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + getCsName() + "." + getClName() + ",fileId=" + fileId,
                    e);
        }
    }

    @Override
    public boolean updateMd5(String fileId, int majorVersion, int minorVersion, String md5)
            throws ScmMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject();
            // matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            BasicBSONObject updator = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_MD5, md5);
            updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, updator);
            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("update md5 failed:table=" + getCsName() + "." + getClName() + ",fileId="
                    + fileId + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion
                    + ",md5=" + md5);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update md5 failed:table=" + getCsName() + "." + getClName() + ",fileId="
                            + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion + ",md5=" + md5,
                    e);
        }
    }

}