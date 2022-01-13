package com.sequoiacm.contentserver.service;

import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataClass;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import org.bson.BSONObject;

import java.util.List;

public interface IMetaDataService {

    List<MetadataClass> listClass(ScmUser user, String wsName, BSONObject filter)
            throws ScmServerException;

    MetadataClass getClassInfoWithAttr(ScmUser user, String wsName, String classId)
            throws ScmServerException;

    MetadataClass getClassInfoWithAttrByName(ScmUser user, String wsName, String className)
            throws ScmServerException;

    MetadataClass createClass(ScmUser user, String wsName, BSONObject classInfo)
            throws ScmServerException;

    MetadataClass updateClass(ScmUser user, String wsName, String classId, BSONObject updator)
            throws ScmServerException;

    void deleteClass(ScmUser user, String wsName, String classId) throws ScmServerException;

    void deleteClassByName(ScmUser user, String workspaceName, String className)
            throws ScmServerException;

    void attachAttr(ScmUser user, String wsName, String classId, String attrId)
            throws ScmServerException;

    void detachAttr(ScmUser user, String wsName, String classId, String attrId)
            throws ScmServerException;

    MetadataAttr createAttr(ScmUser user, String wsName, BSONObject attrInfo)
            throws ScmServerException;

    List<MetadataAttr> listAttr(ScmUser user, String wsName, BSONObject filter)
            throws ScmServerException;

    MetadataAttr getAttrInfo(ScmUser user, String wsName, String attrId) throws ScmServerException;

    MetadataAttr updateAttr(ScmUser user, String wsName, String attrId, BSONObject updator)
            throws ScmServerException;

    void deleteAttr(ScmUser user, String wsName, String attrId) throws ScmServerException;
}
