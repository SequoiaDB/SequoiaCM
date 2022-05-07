package com.sequoiacm.s3import.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

public class RefUtils {

    @SuppressWarnings("unchecked")
    public static <T> List<T> initInstancesAnnotatedWith(
            final Class<? extends Annotation> annotation) throws RuntimeException {
        ArrayList<T> ret = new ArrayList<>();
        try {
            Reflections reflections = new Reflections("com.sequoiacm.s3import.command");
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
                    "Failed instance the class with annotation:" + annotation.getName(), e);
        }
    }
}
