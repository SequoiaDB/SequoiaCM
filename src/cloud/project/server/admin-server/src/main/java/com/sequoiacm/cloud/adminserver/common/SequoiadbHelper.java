package com.sequoiacm.cloud.adminserver.common;

import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class SequoiadbHelper {
    public static final String DOLLAR_SET = "$set";
    public static final String DOLLAR_GTE = "$gte";
    public static final String DOLLAR_LT = "$lt";

    public static DBCollection getCL(Sequoiadb db, String csName, String clName) {
        CollectionSpace cs = db.getCollectionSpace(csName);
        return cs.getCollection(clName);
    }
    
    public static BSONObject queryOne(MetaAccessor accessor, BSONObject matcher, BSONObject order)
            throws StatisticsException {
        MetaCursor cursor = null;
        try {
            cursor = accessor.query(matcher, null, order);
            if (cursor.hasNext()) {
                return cursor.getNext();
            }
            else {
                return null;
            }
        }
//        catch (ScmMetasourceException e) {
//            throw new ScmServerException(e.getScmError(), "Failed to query: " + matcher.toString(),
//                    e);
//        }
        finally {
            closeCursor(cursor);
        }
    }
    
    public static void closeCursor(MetaCursor cursor) {
        if (null != cursor) {
            cursor.close();
        }
    }
}
