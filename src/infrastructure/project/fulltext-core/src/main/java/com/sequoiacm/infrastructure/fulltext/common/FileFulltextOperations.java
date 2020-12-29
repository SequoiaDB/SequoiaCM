package com.sequoiacm.infrastructure.fulltext.common;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;

public class FileFulltextOperations extends ArrayList<FileFulltextOperation> {
    public BSONObject toBSON() {
        BasicBSONList bsonList = new BasicBSONList();
        for (FileFulltextOperation fop : this) {
            bsonList.add(fop.toBSON());
        }
        return bsonList;
    }
}
