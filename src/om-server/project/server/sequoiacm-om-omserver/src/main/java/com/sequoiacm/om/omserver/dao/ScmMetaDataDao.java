package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmClassBasic;
import com.sequoiacm.om.omserver.module.OmClassDetail;
import org.bson.BSONObject;

import java.util.List;

public interface ScmMetaDataDao {

    List<OmClassBasic> getClassList(String wsName, BSONObject condition, BSONObject orderBy,
            int skip, int limit) throws ScmInternalException;

    OmClassDetail getClassDetail(String wsName, String classId) throws ScmInternalException;

    long countClass(String wsName, BSONObject condition) throws ScmInternalException;
}
