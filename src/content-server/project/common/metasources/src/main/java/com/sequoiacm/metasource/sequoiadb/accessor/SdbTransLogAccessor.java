package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaTransLogAccessor;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;

public class SdbTransLogAccessor extends SdbMetaAccessor implements MetaTransLogAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbTransLogAccessor.class);

    public SdbTransLogAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public void delete(String transId) throws SdbMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            deletor.put(FieldName.FIELD_CLTRANS_ID, transId);
            delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete failed:table=" + getCsName() + "." + getClName()
                    + ",transId=" + transId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + getCsName() + "." + getClName()
                    + ",transId=" + transId, e);
        }
    }
}