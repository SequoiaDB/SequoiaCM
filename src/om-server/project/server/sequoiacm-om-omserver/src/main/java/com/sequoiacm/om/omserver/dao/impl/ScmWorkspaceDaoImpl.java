package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.BSONObject;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDataLocation;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.session.ScmOmSessionImpl;

public class ScmWorkspaceDaoImpl implements ScmWorkspaceDao {
    private ScmOmSessionImpl session;
    private Map<String, OmWorkspaceDetail> workspaceCache = new ConcurrentHashMap<>();

    public ScmWorkspaceDaoImpl(ScmOmSessionImpl session) {
        this.session = session;
    }

    @Override
    public List<OmWorkspaceBasicInfo> getWorkspaceList(long skip, int limit)
            throws ScmInternalException, ScmOmServerException {
        List<OmWorkspaceBasicInfo> res = new ArrayList<>();
        ScmSession con = session.getConnection();
        ScmCursor<ScmWorkspaceInfo> cursor = null;
        try {
            cursor = ScmFactory.Workspace.listWorkspace(con,
                    ScmQueryBuilder.start(FieldName.FIELD_CLWORKSPACE_ID).is(1).get(), skip, limit);
            while (cursor.hasNext()) {
                ScmWorkspaceInfo wsInfo = cursor.getNext();
                res.add(transfromToOmBasicWsInfo(wsInfo));
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get workspace list, " + e.getMessage(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }

    private OmWorkspaceBasicInfo transfromToOmBasicWsInfo(ScmWorkspaceInfo wsInfo) {
        OmWorkspaceBasicInfo basicInfo = new OmWorkspaceBasicInfo();
        basicInfo.setName(wsInfo.getName());
        basicInfo.setDescription(wsInfo.getDesc());
        basicInfo.setCreateUser(wsInfo.getCreateUser());
        basicInfo.setCreateTime(wsInfo.getCreateTime());
        return basicInfo;
    }

    @Override
    public OmWorkspaceDetail getWorkspaceDetail(String wsName)
            throws ScmInternalException, ScmOmServerException {
        if (workspaceCache.get(wsName) != null) {
            return workspaceCache.get(wsName);
        }

        ScmSession connection = session.getConnection();
        ScmWorkspace ws = null;
        try {
            ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }

        OmWorkspaceDetail ret = transfromToOmWsDetail(ws);
        workspaceCache.put(wsName, ret);
        return ret;
    }

    private OmWorkspaceDetail transfromToOmWsDetail(ScmWorkspace ws) {
        OmWorkspaceDetail ret = new OmWorkspaceDetail();
        ret.setCreateTime(ws.getCreateTime());
        ret.setCreateUser(ws.getCreateUser());
        ret.setDescription(ws.getDescription());
        ret.setName(ws.getName());
        ret.setUpdateTime(ws.getUpdateTime());
        ret.setUpdateUser(ws.getUpdateUser());

        List<OmWorkspaceDataLocation> retDatalocations = new ArrayList<>();
        for (ScmDataLocation site : ws.getDataLocations()) {
            OmWorkspaceDataLocation retDataLocation = new OmWorkspaceDataLocation();
            retDataLocation.setSiteName(site.getSiteName());
            retDataLocation.setSiteType(site.getType().toString());
            BSONObject option = generateOption(site);
            retDataLocation.setOptions(option);
            retDatalocations.add(retDataLocation);
        }

        ret.setDataLocations(retDatalocations);
        BSONObject option = generateOption(ws.getMetaLocation());
        ret.setMetaOption(option);
        return ret;
    }

    private BSONObject generateOption(ScmLocation site) {
        BSONObject siteBOSN = site.getBSONObject();
        siteBOSN.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        return siteBOSN;
    }

}
