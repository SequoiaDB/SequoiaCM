package com.sequoiacm.contentserver.metadata.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.contentserver.metadata.ClassInfo;

public class WSClassInfo {

    private Map<String, ClassInfo> classInfoMap = new ConcurrentHashMap<>();

    public void addClassInfo(ClassInfo classInfo) {
        classInfoMap.put(classInfo.getId(), classInfo);
    }

    public ClassInfo getClassInfo(String classId) {
        return classInfoMap.get(classId);
    }

}
