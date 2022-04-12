package com.sequoiacm.contentserver.service.impl;

public interface Converter<T> {
    String toJSON(T t);
}
