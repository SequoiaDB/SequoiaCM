package com.sequoiacm.contentserver.common;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.core.convert.converter.Converter;

public class BSONObjectConverter implements Converter<String, BSONObject> {
    @Override
    public BSONObject convert(String source) {
        return (BSONObject) JSON.parse(source);
    }
}
