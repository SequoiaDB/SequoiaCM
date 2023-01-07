package com.sequoiacm.tools.common;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RefUtil {
    public static <T> List<T> initInstancesImplWith(java.lang.Class<T> type) {
        ArrayList<T> ret = new ArrayList<>();
        try {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setExpandSuperTypes(false).forPackages("com.sequoiacm"));
            Set<Class<? extends T>> classes = reflections.getSubTypesOf(type);
            for (Class<?> c : classes) {
                if (Modifier.isAbstract(c.getModifiers())) {
                    continue;
                }
                if (c.isInterface()) {
                    continue;
                }

                Object obj = c.newInstance();
                ret.add((T) obj);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "failed instance the class with interface:" + type.getName(), e);
        }

        return ret;
    }
}
