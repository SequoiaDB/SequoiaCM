package com.sequoiacm.metasource.sequoiadb.accessor;

import java.util.Date;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaSessionAccessor;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;

public class SdbSessionAccessor extends SdbMetaAccessor implements MetaSessionAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbSessionAccessor.class);

    public SdbSessionAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public void delete(String sessionId) throws SdbMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            deletor.put(FieldName.FIELD_CLSESSION_ID, sessionId);

            delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete failed:table=" + getCsName() + "." + getClName()
                    + ",sessionId=" + sessionId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + getCsName() + "." + getClName()
                    + ",sessionId=" + sessionId, e);
        }
    }

    @Override
    public boolean updateDate(String sessionId, Date date) throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.FIELD_CLSESSION_ID, sessionId);

            BSONObject tmp = new BasicBSONObject();
            tmp.put(FieldName.FIELD_CLSESSION_LASTACTIVE_TIME, date.getTime());
            BSONObject updator = new BasicBSONObject();
            updator.put("$set", tmp);

            return updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete failed:table=" + getCsName() + "." + getClName()
                    + ",sessionId=" + sessionId + ",date=" + date);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + getCsName() + "." + getClName()
                    + ",sessionId=" + sessionId + ",date=" + date, e);
        }
    }

}
