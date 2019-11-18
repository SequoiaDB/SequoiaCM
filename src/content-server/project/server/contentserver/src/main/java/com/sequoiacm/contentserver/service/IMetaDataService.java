package com.sequoiacm.contentserver.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataClass;

public interface IMetaDataService {

    List<MetadataClass> listClass(String wsName, BSONObject filter) throws ScmServerException;

    MetadataClass getClassInfoWithAttr(String wsName, String classId) throws ScmServerException;

    MetadataClass createClass(String user, String wsName, BSONObject classInfo) throws ScmServerException;

    MetadataClass updateClass(String user, String wsName, String classId, BSONObject updator)
            throws ScmServerException;

    void deleteClass(String wsName, String classId) throws ScmServerException;

    void attachAttr(String user, String wsName, String classId, String attrId)
            throws ScmServerException;

    void detachAttr(String user, String wsName, String classId, String attrId)
            throws ScmServerException;

    MetadataAttr createAttr(String user, String wsName, BSONObject attrInfo) throws ScmServerException;
    
    List<MetadataAttr> listAttr(String wsName, BSONObject filter) throws ScmServerException;
    
    MetadataAttr getAttrInfo(String wsName, String attrId) throws ScmServerException;
    
    MetadataAttr updateAttr(String user, String wsName, String attrId, BSONObject updator)
            throws ScmServerException;
    
    void deleteAttr(String wsName, String attrId) throws ScmServerException;
}
