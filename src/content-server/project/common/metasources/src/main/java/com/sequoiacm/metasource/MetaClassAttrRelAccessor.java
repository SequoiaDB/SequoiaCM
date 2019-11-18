package com.sequoiacm.metasource;

public interface MetaClassAttrRelAccessor extends MetaAccessor {

    void deleteByClassId(String classId) throws ScmMetasourceException;

    void delete(String classId, String attrId) throws ScmMetasourceException;
}
