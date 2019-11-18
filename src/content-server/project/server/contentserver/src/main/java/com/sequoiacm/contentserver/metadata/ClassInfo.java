package com.sequoiacm.contentserver.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;

public class ClassInfo {
    private Set<String> requiredAttrName = new HashSet<String>();
    private Map<String, AttrInfo> classAttrsInfo = new HashMap<String, AttrInfo>();
    private String id;
    private String name;

    public ClassInfo(String id) {
        this.id = id;
    }
    
    public ClassInfo(BSONObject classBson) {
        id = (String) classBson.get(FieldName.Class.FIELD_ID);
        name = (String) classBson.get(FieldName.Class.FIELD_NAME);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return classAttrsInfo.isEmpty();
    }

    public Set<String> getRequiredAttrName() {
        return requiredAttrName;
    }

    public boolean isAttrInfoExist(String attrName) {
        return classAttrsInfo.containsKey(attrName);
    }

    public void addAttrInfo(AttrInfo attrInfo) {
        this.classAttrsInfo.put(attrInfo.getName(), attrInfo);
        if (attrInfo.isRequired()) {
            this.requiredAttrName.add(attrInfo.getName());
        }
    }

    public AttrInfo getAttrInfo(String attrName) {
        return this.classAttrsInfo.get(attrName);
    }

    public Set<String> getClassAttrNameSet() {
        return this.classAttrsInfo.keySet();
    }

}
