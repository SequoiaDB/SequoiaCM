package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaClassAttrRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiadb.exception.SDBError;

public class SdbClassAttrRelAccessor  extends SdbMetaAccessor implements MetaClassAttrRelAccessor{

    private static final Logger logger = LoggerFactory.getLogger(SdbAttrAccessor.class);
    
    public SdbClassAttrRelAccessor(SdbMetaSource metasource, String csName, String clName,
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
                e.setScmError(ScmError.METADATA_ATTR_ALREADY_IN_CLASS);
            }
            throw e;
        }
    }

    @Override
    public void deleteByClassId(String classId) throws ScmMetasourceException {
        BSONObject deletor = new BasicBSONObject(FieldName.ClassAttrRel.FIELD_CLASS_ID, classId);
        delete(deletor);
    }
    
    @Override
    public void delete(String classId, String attrId) throws ScmMetasourceException {
        BasicBSONObject deletor = new BasicBSONObject();
        deletor.put(FieldName.ClassAttrRel.FIELD_CLASS_ID, classId);
        deletor.put(FieldName.ClassAttrRel.FIELD_ATTR_ID, attrId);
        delete(deletor);
    }
}
