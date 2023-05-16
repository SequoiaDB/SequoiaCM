package com.sequoiacm.client.element.tag;

import org.bson.BSONObject;

/**
 * Tag condition for search file.
 */
public class ScmTagCondition {
    private final BSONObject bsonObject;

    ScmTagCondition(BSONObject bsonObject) {
        this.bsonObject = bsonObject;
    }

    /**
     * Get BSON object. for internal use.
     * 
     * @return
     */
    public BSONObject getBsonObject() {
        return bsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ScmTagCondition that = (ScmTagCondition) o;

        return bsonObject != null ? bsonObject.equals(that.bsonObject) : that.bsonObject == null;
    }

    @Override
    public int hashCode() {
        return bsonObject != null ? bsonObject.hashCode() : 0;
    }

    @Override
    public String toString() {
        return bsonObject == null ? "{}" : bsonObject.toString();
    }
}
