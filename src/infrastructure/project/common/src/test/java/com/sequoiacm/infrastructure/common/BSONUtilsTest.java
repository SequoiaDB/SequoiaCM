package com.sequoiacm.infrastructure.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BSONUtilsTest {

    @Test
    public void test() {
        BSONObject bsonObject = new BasicBSONObject();
        String key = "k";
        String value = "v";
        bsonObject.put(key, value);

        Assert.assertTrue(value.equals(BsonUtils.getString(bsonObject, key)));
    }
}
