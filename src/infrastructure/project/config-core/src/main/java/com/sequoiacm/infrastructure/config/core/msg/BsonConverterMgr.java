package com.sequoiacm.infrastructure.config.core.msg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BsonConverterMgr {
    private static final Logger logger = LoggerFactory.getLogger(BsonConverterMgr.class);
    private Map<String, BsonConverter> converterMap = new HashMap<>();

    @Autowired
    public BsonConverterMgr(List<BsonConverter> converters) {
        for (BsonConverter converter : converters) {
            logger.info("register bson converter:" + converter.getConfigName());
            converterMap.put(converter.getConfigName(), converter);
        }
    }

    public BsonConverter getMsgConverter(String configName) {
        BsonConverter converter = converterMap.get(configName);
        if (converter == null) {
            throw new IllegalArgumentException("no such msg convertor:configName=" + configName);
        }
        return converter;
    }

}
