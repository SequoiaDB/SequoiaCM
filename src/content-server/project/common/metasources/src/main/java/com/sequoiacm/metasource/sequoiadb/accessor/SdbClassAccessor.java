package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaClassAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.exception.SDBError;

public class SdbClassAccessor extends SdbMetaAccessor implements MetaClassAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbClassAccessor.class);

    public SdbClassAccessor(SdbMetaSource metasource, String csName, String clName,
            TransactionContext context) {
        super(metasource, csName, clName, context);
    }

    @Override
    public void insert(BSONObject insertor) throws ScmMetasourceException {
        try {
            super.insert(insertor);
        }
        catch (SdbMetasourceException e) {
            if (e.getErrcode() ==
                    SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.METADATA_CLASS_EXIST);
            }
            throw e;
        }
    }
    
    @Override
    public void delete(String classId) throws ScmMetasourceException {
        BSONObject deletor = new BasicBSONObject(FieldName.Class.FIELD_ID, classId);
        delete(deletor);
    }

    @Override
    public boolean update(String classId, BSONObject newClassInfo) throws ScmMetasourceException {
        BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                newClassInfo);
        BSONObject matcher = new BasicBSONObject(FieldName.Class.FIELD_ID, classId);

        try {
            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            if (e.getErrcode() ==
                    SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.METADATA_CLASS_EXIST);
            }
            throw e;
        }
    }
}
