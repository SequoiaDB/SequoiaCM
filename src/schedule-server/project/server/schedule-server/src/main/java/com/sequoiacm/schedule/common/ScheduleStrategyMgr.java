package com.sequoiacm.schedule.common;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.strategy.core.ConnectivityStrategy;
import com.sequoiacm.infrastructure.strategy.core.StrategyFactory;
import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;
import com.sequoiacm.schedule.core.meta.SiteInfo;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.exception.ScheduleException;

public class ScheduleStrategyMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleStrategyMgr.class);

    private static final ScheduleStrategyMgr INSTANCE = new ScheduleStrategyMgr();

    private ConnectivityStrategy strategy;

    public static ScheduleStrategyMgr getInstance() {
        return INSTANCE;
    }

    public void init(List<BSONObject> strategies, SiteInfo rootSite) throws ScheduleException {
        if (null == rootSite) {
            logger.warn("The system is not create root site,unable to initialize strategy");
            return;
        }
        try {
            List<StrategyInfo> strategyInfos = new ArrayList<>();
            // parse strategy
            for (BSONObject strategy : strategies) {
                int sourceSiteId = (int) getValueCheckNotNull(strategy,
                        FieldName.Strategy.FIELD_SOURCE_SITE);
                int targetSiteId = (int) getValueCheckNotNull(strategy,
                        FieldName.Strategy.FIELD_TARGET_SITE);
                String connectivity = (String) getValueCheckNotNull(strategy,
                        FieldName.Strategy.FIELD_CONNECTIVITY);
                StrategyInfo sInfo = new StrategyInfo(sourceSiteId, targetSiteId, connectivity);
                strategyInfos.add(sInfo);
            }
            this.strategy = StrategyFactory.createStrategy(strategyInfos, rootSite.getId());
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "init strategy failed", e);
        }
    }

    private static Object getValueCheckNotNull(BSONObject obj, String key)
            throws ScheduleException {
        Object value = obj.get(key);
        if (value == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "field is not exist:fieldName=" + key);
        }
        return value;
    }

    public StrategyType strategyType() throws ScheduleException {
        checkStrategyNull();
        return this.strategy.strategyType();
    }

    public void checkTransferSite(WorkspaceInfo wsInfo, int sourceSiteId, int targetSiteId)
            throws ScheduleException {
        checkStrategyNull();
        List<Integer> wsLocationSiteIds = getWorkspaceSiteList(wsInfo);
        try {
            this.strategy.checkTransferSite(wsLocationSiteIds, sourceSiteId, targetSiteId);
        }
        catch (StrategyInvalidArgumentException e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "invalid argument", e);
        }
        catch (StrategyException e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT, e.getMessage(),
                    e);
        }
    }

    public void checkCleanSite(WorkspaceInfo wsInfo, int localSiteId) throws ScheduleException {
        checkStrategyNull();
        List<Integer> wsLocationSiteIds = getWorkspaceSiteList(wsInfo);
        try {
            this.strategy.checkCleanSite(wsLocationSiteIds, localSiteId);
        }
        catch (StrategyInvalidArgumentException e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "invalid argument", e);
        }
        catch (StrategyException e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT, e.getMessage(),
                    e);
        }
    }

    public void removeRootSite(int siteId) throws ScheduleException {
        checkStrategyNull();
        if (strategy.strategyType() == StrategyType.STAR) {
            strategy = null;
        }
    }

    private List<Integer> getWorkspaceSiteList(WorkspaceInfo wsInfo) {
        return wsInfo.getSiteIdList();
    }

    private void checkStrategyNull() throws ScheduleException {
        if (strategy == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.ROOT_SITE_NOT_EXISTS,
                    "The system is not root site, unable to initialize strategy");
        }
    }
}
