package com.sequoiacm.config.metasource;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;

public abstract class TableDaoBase implements TableDao {
    private static final Logger logger = LoggerFactory.getLogger(TableDaoBase.class);

    @Override
    public BSONObject queryOne(BSONObject matcher, BSONObject seletor, BSONObject orderBy)
            throws MetasourceException {
        MetaCursor cursor = query(matcher, seletor, orderBy);
        try {
            if (cursor.hasNext()) {
                BSONObject ret = cursor.getNext();
                if (ret != null) {
                    ret.removeField("_id");
                }
                return ret;
            }
            else {
                return null;
            }
        }
        finally {
            cursor.close();
        }
    }

    @Override
    public int generateId() throws MetasourceException {
        final String FieldIdName = "id";
        BSONObject orderBy = new BasicBSONObject(FieldIdName, -1);
        MetaCursor cursor = null;
        try {
            cursor = query(null, null, orderBy);
            if (cursor.hasNext()) {
                BSONObject rec = cursor.getNext();
                logger.info("The record with the largest id: {}", rec);
                Integer id = (Integer) rec.get(FieldIdName);
                if (id != null && id != Integer.MAX_VALUE) {
                    return id + 1 > 0 ? id + 1 : 1;
                }
                logger.debug("cl exist a abnormal record: {}" + rec);
            }
            else {
                return 1;
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        int id = 1;
        BSONObject matcher = new BasicBSONObject();
        while (true) {
            matcher.put(FieldIdName, id);
            BSONObject queryRes = queryOne(matcher, null, null);

            if (queryRes == null) {
                return id;
            }

            logger.debug("id already be used, try query next:{}" + id);
            if (id == Integer.MAX_VALUE) {
                break;
            }
            id++;
        }

        throw new MetasourceException(ScmConfError.SYSTEM_ERROR, "failed to generate id");
    }

}
