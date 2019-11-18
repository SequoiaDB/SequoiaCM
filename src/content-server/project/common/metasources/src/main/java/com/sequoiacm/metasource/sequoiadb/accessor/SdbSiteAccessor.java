package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;

import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;


public class SdbSiteAccessor extends SdbMetaAccessor {

    public SdbSiteAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public void insert(BSONObject insertor) throws SdbMetasourceException {
        //not support yet
        SdbMetasourceException e= new SdbMetasourceException(
                SDBError.SDB_SYS.getErrorCode(), "not support");
        e.setScmError(ScmError.OPERATION_UNSUPPORTED);
        throw e;
    }

    @Override
    public void update(BSONObject matcher, BSONObject updater) throws SdbMetasourceException {
        //not support yet
        SdbMetasourceException e= new SdbMetasourceException(
                SDBError.SDB_SYS.getErrorCode(), "not support");
        e.setScmError(ScmError.OPERATION_UNSUPPORTED);
        throw e;
    }
}
