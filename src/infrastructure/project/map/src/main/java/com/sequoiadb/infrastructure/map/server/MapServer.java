package com.sequoiadb.infrastructure.map.server;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.server.cache.MapMetaCache;
import com.sequoiadb.infrastructure.map.server.config.MapServerConfig;
import com.sequoiadb.infrastructure.map.server.service.MapServiceImpl;

@Component
class MapServer {
    private static final Logger logger = LoggerFactory.getLogger(MapServiceImpl.class);

    @Autowired
    private MapMetaCache mapMetaCache;

    @Autowired
    private MapServerConfig serverConfig;

    public void init() throws ScmMapServerException {
        startCacheClearTask();
    }

    private void startCacheClearTask() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    logger.info("Start reload mapping table meta data cache ");
                    mapMetaCache.checkAndClear(serverConfig.getMaxResidualTime());
                    logger.info("Finish reload mapping table meta data cache ");
                }
                catch (ScmMapServerException e) {
                    logger.warn("Failed to reload mapping table meta data cache ", e);
                }
                catch (Exception e) {
                    logger.error("Failed to reload mapping table meta data cache ", e);
                }

            }
        }, 10000, serverConfig.getClearJobPeriod());

    }
}
