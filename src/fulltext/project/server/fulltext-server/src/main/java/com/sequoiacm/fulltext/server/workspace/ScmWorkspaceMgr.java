package com.sequoiacm.fulltext.server.workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.fulltext.es.client.base.EsClient;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;

import javax.annotation.PreDestroy;

@Component
public class ScmWorkspaceMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceMgr.class);
    private Map<String, ScmWorkspaceInfo> wsInfos = new ConcurrentHashMap<>();
    private ConfServiceClient confClient;

    @Autowired
    private EsClient esClient;

    private ScmTimer initWorkspaceTimer = null;

    private volatile boolean isInitialized = false;

    @Autowired
    public ScmWorkspaceMgr(ConfServiceClient confClient) {
        this.confClient = confClient;
        try {
            init();
        }
        catch (Exception e) {
            logger.warn("failed to initialize workspace manager, retry later: ", e);
            asyncReInit(this, 5000);
        }
    }

    private void init() throws FullTextException, ScmConfigException {
        List<WorkspaceConfig> wsList = confClient.getWorkspaceList();
        for (WorkspaceConfig ws : wsList) {
            ScmWorkspaceInfo wsInfo = createWsInfo(ws);
            addWs(wsInfo);
        }
        confClient.subscribe(ScmBusinessTypeDefine.WORKSPACE, this::noWsNotify);
        isInitialized = true;
    }

    private void noWsNotify(EventType type, String businessName, NotifyOption notification)
            throws FullTextException {
        if (type == EventType.DELTE) {
            removeWs(businessName);
            return;
        }

        if (type == EventType.CREATE) {
            addWs(businessName);
            return;
        }

        if (type == EventType.UPDATE) {
            updateWs(businessName);
            return;
        }

    }

    public List<ScmWorkspaceFulltextExtData> getWorkspaceExtDataList() {
        checkIsInited();
        ArrayList<ScmWorkspaceFulltextExtData> ret = new ArrayList<>();
        for (ScmWorkspaceInfo ws : wsInfos.values()) {
            ret.add(ws.getExternalData());
        }
        return ret;
    }

    public Collection<ScmWorkspaceInfo> getWorkspaces() {
        checkIsInited();
        Collection<ScmWorkspaceInfo> ret = wsInfos.values();
        return Collections.unmodifiableCollection(ret);
    }

    // 返回 null 表示工作区不存在
    public ScmWorkspaceFulltextExtData getWorkspaceExtData(String ws) {
        checkIsInited();
        ScmWorkspaceInfo wsInfo = getWorkspaceInfo(ws);
        if (wsInfo == null) {
            return null;
        }
        return wsInfo.getExternalData();
    }

    public ScmWorkspaceInfo getWorkspaceInfo(String ws) {
        checkIsInited();
        return wsInfos.get(ws);
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
        ScmWorkspaceFulltextExtData fulltextExtData = wsInfo.getExternalData();
        if (fulltextExtData != null && fulltextExtData.getIndexDataLocation() != null) {
            try {
                esClient.dropIndexAsync(fulltextExtData.getIndexDataLocation());
            }
            catch (Exception e) {
                logger.warn("failed to remove index:ws={}, index={}", wsInfo.getName(),
                        fulltextExtData.getIndexDataLocation(), e);
            }
        }
    }

    void updateWs(String wsName) throws FullTextException {
        WorkspaceConfig wsConf = confClient.getWorkspace(wsName);
        ScmWorkspaceInfo wsInfo = createWsInfo(wsConf);
        wsInfos.put(wsName, wsInfo);

    }

    void addWs(String wsName) throws FullTextException {
        WorkspaceConfig wsConf = confClient.getWorkspace(wsName);
        ScmWorkspaceInfo wsInfo = createWsInfo(wsConf);
        addWs(wsInfo);
    }

    void addWs(ScmWorkspaceInfo wsInfo) {
        wsInfos.put(wsInfo.getName(), wsInfo);

    }

    private void checkIsInited() {
        if (!isInitialized) {
            throw new IllegalStateException("Workspace info manager is not init yet");
        }
    }

    private void asyncReInit(ScmWorkspaceMgr workspaceMgr, int interval) {
        if (initWorkspaceTimer == null) {
            initWorkspaceTimer = ScmTimerFactory.createScmTimer();
        }
        initWorkspaceTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    workspaceMgr.init();
                    cancel();
                }
                catch (Exception e) {
                    logger.warn("failed to init workspace manager", e);
                }
            }
        }, interval, interval);
    }

    @PreDestroy
    private void destroy() {
        if (initWorkspaceTimer != null) {
            initWorkspaceTimer.cancel();
        }
    }
}