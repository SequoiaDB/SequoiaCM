package com.sequoiacm.infrastructrue.security.core.serial.gson;

import org.springframework.core.convert.converter.Converter;

import com.google.gson.TypeAdapter;

public abstract class ScmGsonTypeAdapter<S, T> extends TypeAdapter<T> implements Converter<S, T> {
}