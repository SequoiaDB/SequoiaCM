package com.sequoiacm.diagnose.utils;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RefUtils {

    @SuppressWarnings("unchecked")
    public static <T> List<T> initInstancesAnnotatedWith(
            final Class<? extends Annotation> annotation) throws RuntimeException {
        List<T> ret = new ArrayList<>();
        try {
            Reflections reflections = new Reflections("com.sequoiacm.diagnose.command");
            Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotation);
            for (Class<?> c : classes) {
                if (Modifier.isAbstract(c.getModifiers())) {
                    throw new RuntimeException("class is abstract:" + c.getName());
                }
                if (c.isInterface()) {
                    throw new RuntimeException("class is interface:" + c.getName());
                }
                Object obj = c.newInstance();
                ret.add((T) obj);
            }
            return ret;
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "failed instance the class with annotation:" + annotation.getName(), e);
        }
    }
}
