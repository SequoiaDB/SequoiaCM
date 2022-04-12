package com.sequoiacm.contentserver.common;

import org.bson.BSONObject;

public abstract class AbstractBsonConverter<T> implements BsonConverter<T> {

    @SuppressWarnings("unchecked")
    private  <F> F get(BSONObject object, String field) {
        return (F) object.get(field);
    }

    private <F> F getChecked(BSONObject object, String field) {
        F f = get(object, field);
        if (f == null) {
            throw new IllegalArgumentException("missing field " + field);
        }
        return f;
    }

    private <F> F getOrElse(BSONObject object, String field, F defaultValue) {
        F f = get(object, field);
        if (f == null) {
            f = defaultValue;
        }
        return f;
    }

    protected String getString(BSONObject object, String field) {
        return get(object, field);
    }

    protected String getStringChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    protected String getStringOrElse(BSONObject object, String field, String defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    protected Long getLong(BSONObject object, String field) {
        return get(object, field);
    }

    protected Long getLongChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    protected Long getLongOrElse(BSONObject object, String field, long defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    protected Integer getInteger(BSONObject object, String field) {
        return get(object, field);
    }

    protected Integer getIntegerChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    protected Integer getIntegerOrElse(BSONObject object, String field, int defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    protected Boolean getBoolean(BSONObject object, String field) {
        return get(object, field);
    }

    protected Boolean getBooleanChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    protected Boolean getBooleanOrElse(BSONObject object, String field, boolean defaultValue) {
        return getOrElse(object, field, defaultValue);
    }
    
    protected BSONObject getBson(BSONObject object, String field) {
        return get(object, field);
    }
    
    protected BSONObject getBsonChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }
    
    protected BSONObject getBsonOrElse(BSONObject object, String field, BSONObject defaultValue) {
        return getOrElse(object, field, defaultValue);
    }
}
