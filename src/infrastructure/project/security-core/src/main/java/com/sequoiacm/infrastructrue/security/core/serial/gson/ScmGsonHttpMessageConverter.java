package com.sequoiacm.infrastructrue.security.core.serial.gson;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ScmGsonHttpMessageConverter extends GsonHttpMessageConverter {
    Set<Type> typeSet;

    private ScmGsonHttpMessageConverter(Gson gson, Set<Type> typeSet) {
        setGson(gson);
        this.typeSet = typeSet;
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        // just support type or List<type> format, all support types is store in typeSet
        if (type instanceof ParameterizedType) {
            // List<type>
            ParameterizedType pType = (ParameterizedType) type;
            Type[] paramTypes = pType.getActualTypeArguments();
            if (paramTypes != null && paramTypes.length > 0) {
                Type firstParamType = paramTypes[0];
                return supports(firstParamType);
            }
        }

        return supports(type);
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        // just support type or List<type> format, all support types is store in typeSet
        if (type instanceof ParameterizedType) {
            // List<type>
            ParameterizedType pType = (ParameterizedType) type;
            Type[] paramTypes = pType.getActualTypeArguments();
            if (paramTypes != null && paramTypes.length > 0) {
                Type firstParamType = paramTypes[0];
                return supports(firstParamType);
            }
        }

        return supports(type);
    }

    private boolean supports(Type t) {
        if (null != typeSet) {
            return typeSet.contains(t);
        }

        return false;
    }

    public static Builder start() {
        return new Builder();
    }

    public static class Builder {
        GsonBuilder gb = new GsonBuilder();
        Set<Type> typeSet = new HashSet<>();

        public Builder registerTypeAdapter(Type type, Object typeAdapter) {
            gb.registerTypeAdapter(type, typeAdapter);
            typeSet.add(type);
            return this;
        }

        public ScmGsonHttpMessageConverter build() {
            return new ScmGsonHttpMessageConverter(gb.create(), typeSet);
        }
    }
}
