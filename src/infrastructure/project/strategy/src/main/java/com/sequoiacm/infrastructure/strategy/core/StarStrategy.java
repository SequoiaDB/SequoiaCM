package com.sequoiacm.infrastructure.strategy.core;

import java.util.List;

import com.sequoiacm.infrastructure.strategy.common.StrategyDefine;
import com.sequoiacm.infrastructure.strategy.common.StrategyTools;
import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;

public class StarStrategy implements ConnectivityStrategy {

    private int mainSiteId;

    public StarStrategy(int mainSiteId) {
        this.mainSiteId = mainSiteId;
    }

    @Override
    public StrategyType strategyType() {
        return StrategyType.STAR;
    }

    @Override
    public void checkTransferSite(List<Integer> wsLocationSiteIds, int sourceSiteId,
            int targetSiteId) throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, sourceSiteId);
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, targetSiteId);

        if (sourceSiteId == targetSiteId) {
            throw new StrategyException("The source site and target site cannot be the same"
                    + ":sourceSite=" + sourceSiteId + ",targetSite=" + targetSiteId);
        }
        if (sourceSiteId != mainSiteId && targetSiteId != mainSiteId) {
            throw new StrategyException(
                    "Under the star strategy, cannot transfer file from branch site to branch site"
                            + ":sourceSite=" + sourceSiteId + ",targetSite=" + targetSiteId);
        }
    }

    @Override
    public void checkMoveFileSite(List<Integer> wsLocationSiteIds, int sourceSiteId,
            int targetSiteId) throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, sourceSiteId);
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, targetSiteId);

        if (sourceSiteId == targetSiteId) {
            throw new StrategyException("The source site and target site cannot be the same"
                    + ":sourceSite=" + sourceSiteId + ",targetSite=" + targetSiteId);
        }
        if (sourceSiteId != mainSiteId && targetSiteId != mainSiteId) {
            throw new StrategyException(
                    "Under the star strategy, cannot move file from branch site to branch site"
                            + ":sourceSite=" + sourceSiteId + ",targetSite=" + targetSiteId);
        }
    }

    @Override
    public void checkCleanSite(List<Integer> wsLocationSiteIds, int localSiteId)
            throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, localSiteId);
        /*
         * if (localSiteId == mainSiteId) { throw new StrategyException(
         * "Under the star strategy, the site that performing the clean task cannot be main site"
         * + ":siteId=" + localSiteId); }
         */
    }

    @Override
    public void checkCacheSite(List<Integer> wsLocationSiteIds, int localSiteId)
            throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, localSiteId);
        // unlimited
        /*
         * if (localSiteId == mainSiteId) { throw new StrategyException(
         * "Under the star strategy, the site that performing the cache task cannot be main site"
         * + ":siteId=" + localSiteId); }
         */
    }

    @Override
    public SiteInfo getNearestSite(List<Integer> wsLocationSiteIds, List<Integer> fileLocationSites,
            int localSiteId) throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, localSiteId);
        checkFileLocationSiteNotEmpty(fileLocationSites);
        SiteInfo siteInfo = new SiteInfo();
        if (localSiteId != mainSiteId) {
            siteInfo.setId(mainSiteId);
            siteInfo.setFlag(StrategyDefine.SiteType.FLAG_GOTO_SITE);
        }
        else {
            // according to file location site order
            Integer id = StrategyTools.getSiteIdInFileLocationOrder(fileLocationSites, localSiteId);
            if (null == id) {
                throw new StrategyException("Not found optional site:wsLocationSite="
                        + wsLocationSiteIds + ",fileLocationSite=" + fileLocationSites);
            }
            siteInfo.setId(id);
            siteInfo.setFlag(StrategyDefine.SiteType.FLAG_NORMAL_SITE);
        }
        return siteInfo;
    }

    @Override
    public int getAsyncTransferTargetSite(List<Integer> wsLocationSiteIds, int sourceSiteId)
            throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, sourceSiteId);
        return mainSiteId;
    }

    private void checkSiteInWorkspaceOrNot(List<Integer> wsLocationSiteIds, int localSiteId)
            throws StrategyInvalidArgumentException {
        if (null == wsLocationSiteIds || wsLocationSiteIds.size() == 0) {
            throw new StrategyInvalidArgumentException(
                    "Workspace location site list cannot be empty");
        }
        if (!wsLocationSiteIds.contains(localSiteId)) {
            throw new StrategyInvalidArgumentException("site[" + localSiteId
                    + "] is not in the workspace location site list" + wsLocationSiteIds);
        }
    }

    private void checkFileLocationSiteNotEmpty(List<Integer> fileLocationSiteIds)
            throws StrategyInvalidArgumentException {
        if (null == fileLocationSiteIds || fileLocationSiteIds.size() == 0) {
            throw new StrategyInvalidArgumentException("File location site list cannot be empty");
        }
    }

}
