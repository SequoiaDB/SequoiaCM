package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaAttrAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.exception.SDBError;

public class SdbAttrAccessor extends SdbMetaAccessor implements MetaAttrAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbAttrAccessor.class);

    public SdbAttrAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public void insert(BSONObject insertor) throws SdbMetasourceException {
        try {
            super.insert(insertor);
        }
        catch (SdbMetasourceException e) {
            if (e.getErrcode() ==
                    SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.METADATA_ATTR_EXIST);
            }
            throw e;
        }
    }
    
    @Override
    public void delete(String attrId) throws ScmMetasourceException {
        BSONObject deletor = new BasicBSONObject(FieldName.Attribute.FIELD_ID, attrId);
        delete(deletor);
    }

    @Override
    public boolean update(String attrId, BSONObject newAttrInfo) throws ScmMetasourceException {
        BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                newAttrInfo);
        BSONObject matcher = new BasicBSONObject(FieldName.Attribute.FIELD_ID, attrId);

        try {
            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            if (e.getErrcode() ==
                    SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                e.setScmError(ScmError.METADATA_ATTR_EXIST);
            }
            throw e;
        }
    }

}
