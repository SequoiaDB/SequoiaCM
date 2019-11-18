package com.sequoiacm.deploy.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

public class RefUtil {

    @SuppressWarnings("unchecked")
    public static <T> List<T> initInstancesAnnotatedWith(
            final Class<? extends Annotation> annotation) throws Exception {
        ArrayList<T> ret = new ArrayList<>();
        try {
            Reflections reflections = new Reflections("com.sequoiacm.deploy");
            Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotation);
            for (Class<?> c : classes) {
                if (Modifier.isAbstract(c.getModifiers())) {
                    throw new Exception("class is abstract:" + c.getName());
                }
                if (c.isInterface()) {
                    throw new Exception("class is interface:" + c.getName());
                }
                Object obj = c.newInstance();
                ret.add((T) obj);
            }
            return ret;
        }
        catch (Exception e) {
            throw new Exception(
                    "failed instance the class with annotattion:" + annotation.getName(), e);
        }
    }

}
