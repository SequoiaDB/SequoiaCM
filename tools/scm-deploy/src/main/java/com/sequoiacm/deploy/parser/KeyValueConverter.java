package com.sequoiacm.deploy.parser;

import java.util.Map;

public interface KeyValueConverter<T> {
    T convert(Map<String, String> keyValue);
}
