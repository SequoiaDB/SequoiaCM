package com.sequoiacm.config.metasource.sequoiadb;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class SequoiadbHelper {
    public static final String DOLLAR = "$";
    public static final String DOLLAR0 = "$0";
    public static final String DOLLAR_UNSET = "$unset";
    public static final String DOLLAR_PUSH = "$push";
    public static final String DOLLAR_AND = "$and";
    public static final String DOLLAR_PULL = "$pull";
    public static final String DOLLAR_SET = "$set";
    public static final String DOLLAR_INC = "$inc";
    public static final String DOLLAR_IN = "$in";

    public static DBCollection getCL(String csName, String clName, Sequoiadb db) {
        CollectionSpace cs = db.getCollectionSpace(csName);
        return cs.getCollection(clName);
    }
}
