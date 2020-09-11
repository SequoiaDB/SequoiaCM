package com.sequoiacm.fulltext.server.workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.config.ConfVersionConfig;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;

@Component
public class ScmWorkspaceMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceMgr.class);
    private Map<String, ScmWorkspaceInfo> wsInfos = new ConcurrentHashMap<>();
    private ConfServiceClient confClient;

    private List<ScmWorkspaceEventListener> wsEventListeners;

    @Autowired
    public ScmWorkspaceMgr(ConfServiceClient confClient, ConfVersionConfig versionConfig,  List<ScmWorkspaceEventListener> wsEventListeners)
            throws FullTextException {
        this.confClient = confClient;
        this.wsEventListeners = wsEventListeners;
        confClient.registerSubscriber(
                new ScmWorkspaceSubscriber(this, versionConfig.getWorkspaceHeartbeat()));
        List<WorkspaceConfig> wsList = confClient.getWorkspaceList();
        for (WorkspaceConfig ws : wsList) {
            ScmWorkspaceInfo wsInfo = createWsInfo(ws);
            addWs(wsInfo);
        }
    }

    public List<ScmWorkspaceFulltextExtData> getWorkspaceExtDataList() {
        ArrayList<ScmWorkspaceFulltextExtData> ret = new ArrayList<>();
        for (ScmWorkspaceInfo ws : wsInfos.values()) {
            ret.add(ws.getExternalData());
        }
        return ret;
    }

    public Collection<ScmWorkspaceInfo> getWorkspaces() {
        Collection<ScmWorkspaceInfo> ret = wsInfos.values();
        return Collections.unmodifiableCollection(ret);
    }

    // 返回 null 表示工作区不存在
    public ScmWorkspaceFulltextExtData getWorkspaceExtData(String ws) {
        ScmWorkspaceInfo wsInfo = wsInfos.get(ws);
        if (wsInfo == null) {
            return null;
        }
        return wsInfo.getExternalData();
    }

    private ScmWorkspaceInfo createWsInfo(WorkspaceConfig ws) {
        ScmWorkspaceInfo wsInfo = new ScmWorkspaceInfo();
        wsInfo.setId(ws.getWsId());
        wsInfo.setExternalData(new ScmWorkspaceFulltextExtData(ws.getWsName(), ws.getWsId(),
                ws.getExternalData()));
        wsInfo.setName(ws.getWsName());
        List<Integer> sites = new ArrayList<>();
        BasicBSONList dataLocations = ws.getDataLocations();
        for (Object dataLocation : dataLocations) {
            BSONObject dataBson = (BSONObject) dataLocation;
            sites.add(BsonUtils.getIntegerChecked(dataBson,
                    FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID));
        }
        wsInfo.setSites(sites);
        return wsInfo;
    }

    void removeWs(String wsName) {
        ScmWorkspaceInfo wsInfo = wsInfos.remove(wsName);
        if (wsInfo == null) {
            return;
        }
        for (ScmWorkspaceEventListener l : wsEventListeners) {
            l.onWorkspaceRemove(wsInfo);
        }
    }

    void updateWs(String wsName) throws FullTextException {
        WorkspaceConfig wsConf = confClient.getWorkspace(wsName);
        ScmWorkspaceInfo wsInfo = createWsInfo(wsConf);
        wsInfos.put(wsName, wsInfo);
        for (ScmWorkspaceEventListener l : wsEventListeners) {
            l.onWorkspaceUpdate(wsInfo);
        }
    }

    void addWs(String wsName) throws FullTextException {
        WorkspaceConfig wsConf = confClient.getWorkspace(wsName);
        ScmWorkspaceInfo wsInfo = createWsInfo(wsConf);
        addWs(wsInfo);
    }

    void addWs(ScmWorkspaceInfo wsInfo) {
        wsInfos.put(wsInfo.getName(), wsInfo);
        for (ScmWorkspaceEventListener l : wsEventListeners) {
            l.onWorkspaceAdd(wsInfo);
        }
    }
}
