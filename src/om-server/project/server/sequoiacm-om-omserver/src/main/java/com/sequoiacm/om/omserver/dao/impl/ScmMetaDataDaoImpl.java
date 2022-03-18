package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmMetaDataDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmAttributeBasicInfo;
import com.sequoiacm.om.omserver.module.OmClassBasic;
import com.sequoiacm.om.omserver.module.OmClassDetail;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.util.ArrayList;
import java.util.List;

public class ScmMetaDataDaoImpl implements ScmMetaDataDao {

    private ScmOmSession session;

    public ScmMetaDataDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public List<OmClassBasic> getClassList(String wsName, BSONObject condition, BSONObject orderBy,
            int skip, int limit) throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmCursor<ScmClassBasicInfo> cursor = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            cursor = ScmFactory.Class.listInstance(ws, condition, orderBy, skip, limit);
            List<OmClassBasic> res = new ArrayList<>();
            while (cursor.hasNext()) {
                ScmClassBasicInfo classBasicInfo = cursor.getNext();
                OmClassBasic omClassBasic = transformToOmClassBasic(classBasicInfo);
                res.add(omClassBasic);
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get class list, " + e.getMessage(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public OmClassDetail getClassDetail(String wsName, String classId) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            ScmClass scmClass = ScmFactory.Class.getInstance(ws, new ScmId(classId));
            return transformToClassDetail(scmClass);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get class info, " + e.getMessage(), e);
        }
    }

    @Override
    public long countClass(String wsName, BSONObject condition) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            return ScmFactory.Class.countInstance(ws, condition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count class, " + e.getMessage(),
                    e);
        }
    }

    private OmClassBasic transformToOmClassBasic(ScmClassBasicInfo classBasicInfo) {
        OmClassBasic omClassBasic = new OmClassBasic();
        omClassBasic.setId(classBasicInfo.getId().get());
        omClassBasic.setName(classBasicInfo.getName());
        omClassBasic.setDescription(classBasicInfo.getDescription());
        omClassBasic.setCreateUser(classBasicInfo.getCreateUser());
        omClassBasic.setCreateTime(classBasicInfo.getCreateTime());
        return omClassBasic;
    }

    private OmClassDetail transformToClassDetail(ScmClass scmClass) {
        OmClassDetail omClassDetail = new OmClassDetail();
        omClassDetail.setId(scmClass.getId().get());
        omClassDetail.setName(scmClass.getName());
        omClassDetail.setDescription(scmClass.getDescription());
        omClassDetail.setCreateUser(scmClass.getCreateUser());
        omClassDetail.setCreateTime(scmClass.getCreateTime());
        omClassDetail.setUpdateUser(scmClass.getUpdateUser());
        omClassDetail.setUpdateTime(scmClass.getUpdateTime());
        List<OmAttributeBasicInfo> attrs = new ArrayList<>();
        for (ScmAttribute scmAttribute : scmClass.listAttrs()) {
            attrs.add(new OmAttributeBasicInfo(scmAttribute));
        }
        omClassDetail.setAttrList(attrs);
        return omClassDetail;
    }
}
