package com.sequoiacm.infrastructure.strategy.common;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;

public class StrategyTools {

    public static Integer getSiteIdInOrder(List<Integer> wsLocationSiteIds, List<Integer> fileLocationSiteIds, 
            int localSiteId) {
        for (Integer id : wsLocationSiteIds) {
            if (fileLocationSiteIds.contains(id) && id != localSiteId) {
                return id;
            }
        }
        return null;
    }
    
    public static Integer getSiteIdInFileLocationOrder(List<Integer> fileLocationSiteIds, int localSiteId) {
        if (fileLocationSiteIds.contains(localSiteId)) {
            fileLocationSiteIds.remove(new Integer(localSiteId));
        }
        if (fileLocationSiteIds.size() > 0) {
            return fileLocationSiteIds.get(0);
        }
        return null;
    }
    
    public static boolean isConnected(List<StrategyInfo> strategies, int sourceSiteId, int targetSiteId) {
        // TODO current version, any site can be connected under the network strategy
        return true;
    }
    
    public static boolean isLastSiteInList(List<Integer> siteIdList, int siteId) {
        return siteIdList.get(siteIdList.size()-1) == siteId;
    }
    
    public static List<Integer> getSiteIdsAfterSpecifiedSite(List<Integer> siteIds, int siteId) 
            throws StrategyException {
        List<Integer> siteList = new ArrayList<>();
        int index = siteIds.indexOf(siteId);
        if (index != -1) {
            for (int i=index+1,len=siteIds.size(); i < len; i++) {
                siteList.add(siteIds.get(i));
            }
        }
        else {
            throw new StrategyException(
                    "site[" + siteId + "] is not in the site list " + siteIds);
        }
        return siteList;
    }
}
