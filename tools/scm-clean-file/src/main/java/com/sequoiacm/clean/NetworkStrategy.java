package com.sequoiacm.clean;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;

public class NetworkStrategy {

    public static List<Integer> getCleanTaskVerifySites(List<Integer> wsLocationSiteIds,
            List<Integer> fileLocationSites, int localSiteId) throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, localSiteId);
        checkFileLocationSiteNotNull(fileLocationSites);

        List<Integer> siteList = new ArrayList<>();
        int index = wsLocationSiteIds.indexOf(localSiteId);
        if (index != -1) {
            for (int i = index + 1, len = wsLocationSiteIds.size(); i < len; i++) {
                Integer tmpId = wsLocationSiteIds.get(i);
                if (fileLocationSites.contains(tmpId)) {
                    siteList.add(tmpId);
                }
            }
        }
        /*
         * if (siteList.size() == 0) { throw new
         * StrategyException("the sites behind the local site does not contain this files"
         * ); }
         */
        return siteList;
    }

    private static void checkSiteInWorkspaceOrNot(List<Integer> wsLocationSiteIds, int localSiteId)
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

    private static void checkFileLocationSiteNotNull(List<Integer> fileLocationSiteIds)
            throws StrategyInvalidArgumentException {
        if (null == fileLocationSiteIds) {
            throw new StrategyInvalidArgumentException("File location site list cannot be null");
        }
    }
}
