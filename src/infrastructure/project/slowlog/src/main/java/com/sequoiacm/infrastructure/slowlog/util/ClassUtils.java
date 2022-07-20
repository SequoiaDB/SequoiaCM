package com.sequoiacm.infrastructure.slowlog.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.util.ArrayList;
import java.util.List;

public class ClassUtils {

    public static List<ClassMetaInfo> scanClasses(String packageName, ClassLoader classLoader)
            throws Exception {
        List<ClassMetaInfo> classMetaInfoList = new ArrayList<>();
        ResourcePatternResolver resourcePatternResolver = null;
        if (classLoader == null) {
            resourcePatternResolver = new PathMatchingResourcePatternResolver();
        }
        else {
            resourcePatternResolver = new PathMatchingResourcePatternResolver(classLoader);
        }
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                + org.springframework.util.ClassUtils.convertClassNameToResourcePath(packageName)
                + "/**/*.class";
        Resource[] resources = resourcePatternResolver.getResources(pattern);
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(
                resourcePatternResolver);
        for (Resource resource : resources) {
            MetadataReader reader = readerFactory.getMetadataReader(resource);
            ClassMetadata classMetadata = reader.getClassMetadata();
            ClassMetaInfo metaInfo = new ClassMetaInfo();
            metaInfo.setClassName(classMetadata.getClassName());
            metaInfo.setAnnotation(classMetadata.isAnnotation());
            metaInfo.setInterface(classMetadata.isInterface());
            metaInfo.setSuperClassName(classMetadata.getSuperClassName());
            classMetaInfoList.add(metaInfo);
        }
        return classMetaInfoList;
    }

    public static List<ClassMetaInfo> scanClasses(String packageName) throws Exception {
        return scanClasses(packageName, null);
    }
}
