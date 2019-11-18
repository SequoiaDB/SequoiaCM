package com.sequoiacm.strategy;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.infrastructure.strategy.core.ConnectivityStrategy;
import com.sequoiacm.infrastructure.strategy.core.StrategyFactory;
import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;

public class TestInit {

    private List<StrategyInfo> strategyList = new ArrayList<>();
    
    @BeforeClass
    public void setUp() {
        strategyList.add(new StrategyInfo(-1, -1, StrategyType.NETWORK.getName()));
    }
    
    @Test
    public void testInit() throws StrategyException {
        ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, 1);
//        strategy.checkTransferSite(wsLocationSiteIds, sourceSiteId, targetSiteId);
        Assert.assertEquals(strategy.strategyType(), StrategyType.NETWORK);
    }
}
