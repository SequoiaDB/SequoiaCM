package com.sequoiacm.s3.context;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.config.ContextConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.remote.ScmContentServerClient;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.core.ScmMapFactory;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClient;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClientFactory;

@Component
public class S3ListObjContextMgr {
    @Autowired
    private ScmClientFactory clientFactory;

    @Autowired
    private MapFeignClientFactory mapClientFactory;

    private Map<String, S3ListObjContextMeta> contextMetas;

    private ScmTimer timer;

    private ContextConfig contextConfig;

    @Autowired
    public S3ListObjContextMgr(ContextConfig contextConfig) {
        timer = ScmTimerFactory.createScmTimer();
        this.contextConfig = contextConfig;
        timer.schedule(new ContextCleaner(this), contextConfig.getCleanPeriod(),
                contextConfig.getCleanPeriod());

    }

    private Map<String, S3ListObjContextMeta> getContextMetas() throws S3ServerException {
        initMap();
        return contextMetas;
    }

    private void initMap() throws S3ServerException {
        if (contextMetas != null) {
            return;
        }
        MapFeignClient client = mapClientFactory
                .getFeignClientByServiceName(clientFactory.getRootSite());
        try {
            contextMetas = ScmMapFactory.getGroupMap(client, S3CommonDefine.S3_MAP_GROUP_NAME)
                    .createMap(S3CommonDefine.S3_MAP_LIST_CONTEXT_META_NAME, String.class,
                            S3ListObjContextMeta.class);
        }
        catch (ScmMapServerException e) {
            if (e.getError().equals(ScmMapError.MAP_TABLE_ALREADY_EXIST)) {
                try {
                    contextMetas = ScmMapFactory
                            .getGroupMap(client, S3CommonDefine.S3_MAP_GROUP_NAME)
                            .getMap(S3CommonDefine.S3_MAP_LIST_CONTEXT_META_NAME);
                    return;
                }
                catch (ScmMapServerException e1) {
                    throw new S3ServerException(S3Error.SYSTEM_ERROR,
                            "failed to init list_context_meta map", e1);
                }
            }
            throw new S3ServerException(S3Error.SYSTEM_ERROR,
                    "failed to init list_context_meta map", e);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.SYSTEM_ERROR,
                    "failed to init list_context_meta map", e);
        }
    }

    @PreDestroy
    public void destory() {
        timer.cancel();
    }

    public S3ListObjContext createContext(ScmSession ss, String bucketName, String ws,
            String bucketDir, String prefix, String startAfter, String delimiter)
            throws S3ServerException {
        S3ListObjContextMeta meta = new S3ListObjContextMeta(UUID.randomUUID().toString(),
                bucketName, ws, bucketDir, prefix, startAfter, delimiter, startAfter);
        ScmContentServerClient client = clientFactory.getContentServerClient(ss, meta.getWs());
        S3ListObjContext ret = new S3ListObjContext(client, this, meta);
        ret.setNewContext(true);
        return ret;
    }

    public S3ListObjContext getContext(ScmSession session, String id) throws S3ServerException {
        S3ListObjContextMeta meta = getContextMetas().get(id);
        if (meta == null) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_TOKEN,
                    "The continuation token provided is incorrect.token:" + id);
        }
        ScmContentServerClient client = clientFactory.getContentServerClient(session, meta.getWs());
        S3ListObjContext ret = new S3ListObjContext(client, this, meta);
        ret.setNewContext(false);
        return ret;
    }

    void updateContextMeta(S3ListObjContextMeta meta) throws S3ServerException {
        getContextMetas().put(meta.getId(), meta);
    }

    public void remove(String meta) throws S3ServerException {
        getContextMetas().remove(meta);
    }

    void cleanExpireContext() throws S3ServerException {
        long now = System.currentTimeMillis();
        Set<String> keyset = getContextMetas().keySet();
        for (String key : keyset) {
            S3ListObjContextMeta meta = getContextMetas().get(key);
            if (meta != null && now - meta.getUpdateTime() > contextConfig.getKeepaliveTime()) {
                getContextMetas().remove(key);
            }
        }
    }

}

class ContextCleaner extends ScmTimerTask {
    private Logger logger = LoggerFactory.getLogger(ContextCleaner.class);
    private S3ListObjContextMgr mgr;

    public ContextCleaner(S3ListObjContextMgr mgr) {
        this.mgr = mgr;
    }

    @Override
    public void run() {
        try {
            mgr.cleanExpireContext();
        }
        catch (S3ServerException e) {
            logger.warn("failed to clean context", e);
        }
    }

}
