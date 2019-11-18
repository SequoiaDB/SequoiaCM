package com.sequoiacm.infrastructure.strategy.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;

public class StrategyFactory {

    private static final Logger logger = LoggerFactory.getLogger(StrategyFactory.class);
    
    public static ConnectivityStrategy createStrategy(List<StrategyInfo> strategies, int mainSiteId) 
            throws StrategyException {
        
        StrategyType strategyType = null;
        for (StrategyInfo strategy : strategies) {
            if (strategy.getSourceSiteId() == -1 && strategy.getTargetSiteId() == -1) {
                strategyType = StrategyType.getType(strategy.getConnectivity());
            }
        }
        if (null == strategyType) {
            logger.info("no global strategy is configured,use default 'star'");
            strategyType = StrategyType.STAR;
        }
        
        logger.info("global connectivity strategy:" + strategyType.name());
        
        switch (strategyType) {
            case STAR:
                return new StarStrategy(mainSiteId);
                
            case NETWORK:
                return new NetworkStrategy(strategies);

            default:
                throw new StrategyException("Unknown strategy type: " + strategyType.getName());
        }
    }
    
    /*public static ConnectivityStrategy createStrategy(List<BSONObject> strategies, int mainSiteId) 
            throws StrategyException {
        List<StrategyInfo> strategyInfos = new ArrayList<>();
        StrategyType strategyType = null;
        // parse strategy
        for (BSONObject strategy : strategies) {
            int sourceSiteId = (int) getValueCheckNotNull(strategy, FieldName.Strategy.FIELD_SOURCE_SITE);
            int targetSiteId = (int) getValueCheckNotNull(strategy, FieldName.Strategy.FIELD_TARGET_SITE);
            String connectivity = 
                    (String) getValueCheckNotNull(strategy, FieldName.Strategy.FIELD_CONNECTIVITY);
            
            StrategyInfo sInfo = new StrategyInfo(sourceSiteId, targetSiteId, connectivity);
            strategyInfos.add(sInfo);
            if (sourceSiteId == -1 && targetSiteId == -1) {
                strategyType = StrategyType.getType(connectivity);
            }
        }
        if (null == strategyType) {
            logger.info("no global strategy is configured,use default 'star'");
            strategyType = StrategyType.STAR;
        }
        
        logger.info("global connectivity strategy:" + strategyType.name());
        
        switch (strategyType) {
            case STAR:
                return new StarStrategy(mainSiteId);
                
            case NETWORK:
                return new NetworkStrategy(strategyInfos);

            default:
                throw new StrategyException("Unknown strategy type: " + strategyType.getName());
        }
    }*/
}
