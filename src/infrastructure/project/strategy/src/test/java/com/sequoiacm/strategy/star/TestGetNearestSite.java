package com.sequoiacm.strategy.star;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.infrastructure.strategy.common.StrategyDefine;
import com.sequoiacm.infrastructure.strategy.core.ConnectivityStrategy;
import com.sequoiacm.infrastructure.strategy.core.StrategyFactory;
import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;

public class TestGetNearestSite {

    private List<StrategyInfo> strategyList = new ArrayList<>();
    List<Integer> wsLocationSiteIds = new ArrayList<>();
    
    @BeforeClass
    public void setUp() {
        strategyList.add(new StrategyInfo(-1, -1, StrategyType.STAR.getName()));
        
        wsLocationSiteIds.add(1);
        wsLocationSiteIds.add(2);
        wsLocationSiteIds.add(3);
    }
    
    @Test
    public void testGetNearestSiteNormal() {
        int localSiteId = 2;
        int mainSiteId = 1;
        
        List<Integer> fileLocationSites = new ArrayList<>();
        fileLocationSites.add(3);
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            SiteInfo siteInfo = strategy.getNearestSite(wsLocationSiteIds, fileLocationSites, localSiteId);
            Assert.assertEquals(siteInfo.getId(), mainSiteId);
            Assert.assertEquals(siteInfo.getFlag(), StrategyDefine.SiteType.FLAG_GOTO_SITE);
            
            siteInfo = strategy.getNearestSite(wsLocationSiteIds, fileLocationSites, siteInfo.getId());
            Assert.assertEquals(siteInfo.getId(), 3);
            Assert.assertEquals(siteInfo.getFlag(), StrategyDefine.SiteType.FLAG_NORMAL_SITE);
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetNearestSiteInvalid() {
        int localSiteId = 2;
        int mainSiteId = 1;
        
        List<Integer> fileLocationSites = new ArrayList<>();
        fileLocationSites.add(1);
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            SiteInfo siteInfo = strategy.getNearestSite(wsLocationSiteIds, fileLocationSites, localSiteId);
            Assert.assertEquals(siteInfo.getId(), mainSiteId);
            Assert.assertEquals(siteInfo.getFlag(), StrategyDefine.SiteType.FLAG_GOTO_SITE);
            try {
                siteInfo = strategy.getNearestSite(wsLocationSiteIds, fileLocationSites, siteInfo.getId());
            }
            catch (StrategyException e) {
                Assert.assertEquals(StrategyException.class, e.getClass(), e.getMessage());
            }
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetNearestSiteInvalidArg() {
        int localSiteId = 2;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.getNearestSite(wsLocationSiteIds, null, localSiteId);
        }
        catch (Exception e) {
            Assert.assertEquals(StrategyInvalidArgumentException.class, e.getClass(), e.getMessage());
        }
    }
}
