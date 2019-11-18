package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaDirAccessor;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.exception.SDBError;

public class SdbDirAccessor extends SdbMetaAccessor implements MetaDirAccessor {
    private Logger logger = LoggerFactory.getLogger(SdbDirAccessor.class);

    public SdbDirAccessor(SdbMetaSource metasource, String csName, String clName,
            TransactionContext transactionContext) {
        super(metasource, csName, clName, transactionContext);
    }

    @Override
    public void insert(BSONObject insertor) throws SdbMetasourceException {
        try {
            super.insert(insertor);
        }
        catch (SdbMetasourceException e) {
            if (e.getErrcode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.DIR_EXIST);
            }
            throw e;
        }
    }

    @Override
    public void updateDirInfo(String id, BSONObject newDirInfo) throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.FIELD_CLDIR_ID, id);

            BSONObject updator = new BasicBSONObject();
            updator.put(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, newDirInfo);
            super.update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            if (e.getErrcode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.DIR_EXIST);
            }
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update directory failed:table=" + getCsName() + "." + getClName() + ",dirId="
                            + id,
                            e);
        }
    }

    @Override
    public void delete(String id) throws SdbMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            deletor.put(FieldName.FIELD_CLDIR_ID, id);
            super.delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete directory failed:table={}.{},dirId={}", getCsName(), getClName(),
                    id);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete directory failed:table=" + getCsName() + "." + getClName() + ",dirId="
                            + id,
                            e);
        }
    }
}
