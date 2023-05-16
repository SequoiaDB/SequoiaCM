package com.sequoiacm.tools.tag.common;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;

import java.util.List;

public class CommUtil {

    public static long getWorkspaceFileCount(Sequoiadb db, String wsName) {
        long ret = 0;
        CollectionSpace wsMetaCS = db.getCollectionSpace(wsName + "_META");
        DBCollection fileCl = wsMetaCS.getCollection("FILE");
        ret += fileCl.getCount();

        DBCollection fileHistoryCl = wsMetaCS.getCollection("FILE_HISTORY");
        ret += fileHistoryCl.getCount();
        return ret;
    }

    public static BSONObject containTagCondition() {
        BSONObject fileContainsCustomTag = (BSONObject) JSON.parse(
                "{$and: [  {custom_tag: {$exists: 1}},   {$not: [{custom_tag: {}}] },   {$not: [{custom_tag: null}]}  ]}");
        BSONObject fileContainsTag = (BSONObject) JSON.parse(
                "{$and: [ {tags: {$exists: 1}}, {$not: [{tags: []}]},  {$not: [{tags: null}]}  ]}");
        BasicBSONList orArr = new BasicBSONList();
        orArr.add(fileContainsCustomTag);
        orArr.add(fileContainsTag);
        return new BasicBSONObject("$or", orArr);
    }
}
