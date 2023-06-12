package com.sequoiacm.infrastructure.common;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

public class RefUtil {

    public static <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder().setExpandSuperTypes(false).forPackages("com.sequoiacm"));
        return reflections.getSubTypesOf(type);
    }
}
