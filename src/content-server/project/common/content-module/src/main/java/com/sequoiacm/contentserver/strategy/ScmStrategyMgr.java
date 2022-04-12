package com.sequoiacm.contentserver.strategy;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.strategy.common.StrategyDefine;
import com.sequoiacm.infrastructure.strategy.core.ConnectivityStrategy;
import com.sequoiacm.infrastructure.strategy.core.StrategyFactory;
import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;
import org.bson.BSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScmStrategyMgr {
    
    private static final ScmStrategyMgr INSTANCE = new ScmStrategyMgr();
    
    private ConnectivityStrategy strategy;

    private ScmStrategyMgr() {
    }
    
    public static ScmStrategyMgr getInstance() {
        return INSTANCE;
    }
    
    public void init(List<BSONObject> strategies, int mainSiteId) throws ScmServerException {
        try {
            List<StrategyInfo> strategyInfos = new ArrayList<>();
            // parse strategy
            for (BSONObject strategy : strategies) {
                int sourceSiteId = (int) getValueCheckNotNull(strategy, FieldName.Strategy.FIELD_SOURCE_SITE);
                int targetSiteId = (int) getValueCheckNotNull(strategy, FieldName.Strategy.FIELD_TARGET_SITE);
                String connectivity = 
                        (String) getValueCheckNotNull(strategy, FieldName.Strategy.FIELD_CONNECTIVITY);
                StrategyInfo sInfo = new StrategyInfo(sourceSiteId, targetSiteId, connectivity);
                strategyInfos.add(sInfo);
            }
            this.strategy = StrategyFactory.createStrategy(strategyInfos, mainSiteId);
        }
        catch (Exception e) {
            throw new ScmSystemException("init strategy failed", e);
        }
    }
    
    private static Object getValueCheckNotNull(BSONObject obj, String key) 
            throws ScmMissingArgumentException {
        Object value = obj.get(key);
        if (value == null) {
            throw new ScmMissingArgumentException("field is not exist:fieldName=" + key);
        }
        return value;
    }
    
    public StrategyType strategyType() {
        return this.strategy.strategyType();
    }
    
    public void checkTransferSite(ScmWorkspaceInfo wsInfo, int sourceSiteId, int targetSiteId) 
            throws ScmServerException {
        List<Integer> wsLocationSiteIds = getWsLocationSites(wsInfo);
        try {
            this.strategy.checkTransferSite(wsLocationSiteIds, sourceSiteId, targetSiteId);
        }
        catch (StrategyInvalidArgumentException e) {
            throw new ScmSystemException("invalid argument", e);
        }
        catch (StrategyException e) {
            throw new ScmOperationUnsupportedException(e.getMessage(), e);
        }
    }
    
    public void checkCleanSite(ScmWorkspaceInfo wsInfo, int localSiteId) throws ScmServerException {
        List<Integer> wsLocationSiteIds = getWsLocationSites(wsInfo);
        try {
            this.strategy.checkCleanSite(wsLocationSiteIds, localSiteId);
        }
        catch (StrategyInvalidArgumentException e) {
            throw new ScmSystemException("invalid argument", e);
        }
        catch (StrategyException e) {
            throw new ScmOperationUnsupportedException(e.getMessage(), e);
        }
    }
    
    public void checkCacheSite(ScmWorkspaceInfo wsInfo, int localSiteId) throws ScmServerException {
        List<Integer> wsLocationSiteIds = getWsLocationSites(wsInfo);
        try {
            this.strategy.checkCacheSite(wsLocationSiteIds, localSiteId);
        }
        catch (StrategyInvalidArgumentException e) {
            throw new ScmSystemException("invalid argument", e);
        }
        catch (StrategyException e) {
            throw new ScmOperationUnsupportedException(e.getMessage(), e);
        }
    }
    
    public SiteInfo getNearestSite(ScmWorkspaceInfo wsInfo, List<Integer> fileLocationSites, 
            int localSiteId, String fileId) throws ScmServerException {
        List<Integer> wsLocationSiteIds = getWsLocationSites(wsInfo);
        try {
            return this.strategy.getNearestSite(wsLocationSiteIds, fileLocationSites, localSiteId);
        }
        catch (StrategyInvalidArgumentException e) {
            throw new ScmSystemException("invalid argument", e);
        }
        catch (StrategyException e) {
            throw new ScmSystemException("getNearestSite failed:workspace="
                    + wsInfo.getName() + ",siteList=" + fileLocationSites + ",fileId=" + fileId, e);
        }
    }
    
    public int getAsyncCacheRemoteSite(ScmWorkspaceInfo wsInfo, List<Integer> fileLocationSites, 
            int localSiteId, String fileId) throws ScmServerException {
        SiteInfo siteInfo = getNearestSite(wsInfo, fileLocationSites, localSiteId, fileId);
        if (siteInfo.getFlag() == StrategyDefine.SiteType.FLAG_GOTO_SITE
                && !fileLocationSites.contains(siteInfo.getId())) {
            throw new ScmServerException(ScmError.DATA_NOT_EXIST, 
                    "file is not exist in main site:workspace=" 
                    + wsInfo.getName() + ",fileId=" + fileId + ",mainSiteId=" + siteInfo.getId());
        }
        return siteInfo.getId();
    }
    
    public int getDefaultAsyncTransferTargetSite(ScmWorkspaceInfo wsInfo, int localSiteId) throws ScmServerException {
        List<Integer> wsLocationSiteIds = getWsLocationSites(wsInfo);
        try {
            return this.strategy.getAsyncTransferTargetSite(wsLocationSiteIds, localSiteId);
        }
        catch (StrategyInvalidArgumentException e) {
            throw new ScmSystemException(e.getMessage(), e);
        }
        catch (StrategyException e) {
            throw new ScmOperationUnsupportedException(e.getMessage(), e);
        }
    }
    
    private List<Integer> getWsLocationSites(ScmWorkspaceInfo wsInfo) {
        Set<Integer> siteIds = wsInfo.getDataSiteIds();
        return new ArrayList<Integer>(siteIds);
    }

}
