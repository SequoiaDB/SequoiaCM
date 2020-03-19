package com.sequoiacm.s3.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.config.ContextConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.remote.ScmContentServerClient;

@Component
public class S3ListObjContextMgr {

    @Autowired
    private ScmClientFactory clientFactory;

    // TODO:
    private Map<String, S3ListObjContextMeta> contextMetas = new HashMap<>();

    private ScmTimer timer;

    private ContextConfig contextConfig;

    @Autowired
    public S3ListObjContextMgr(ContextConfig contextConfig) {
        timer = ScmTimerFactory.createScmTimer();
        this.contextConfig = contextConfig;
        timer.schedule(new ContextCleaner(this), contextConfig.getCleanPeriod(),
                contextConfig.getCleanPeriod());
        
    }

    @PreDestroy
    public void destory() {
        timer.cancel();
    }

    public S3ListObjContext createContext(ScmSession ss, String bucketName, String ws, String bucketDir,
            String prefix, String startAfter, String delimiter) throws S3ServerException {
        S3ListObjContextMeta meta = new S3ListObjContextMeta(UUID.randomUUID().toString(),
                bucketName, ws, bucketDir, prefix, startAfter, delimiter, startAfter);
        ScmContentServerClient client = clientFactory.getContentServerClient(ss, meta.getWs());
        S3ListObjContext ret = new S3ListObjContext(client, this, meta);
        ret.setNewContext(true);
        return ret;
    }

    public S3ListObjContext getContext(ScmSession session, String id) throws S3ServerException {
        S3ListObjContextMeta meta = contextMetas.get(id);
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
        contextMetas.put(meta.getId(), meta);
    }

    public void remove(String meta) {
        contextMetas.remove(meta);
    }

    void cleanExpireContext() {
        long now = System.currentTimeMillis();
        Set<String> keyset = contextMetas.keySet();
        for (String key : keyset) {
            S3ListObjContextMeta meta = contextMetas.get(key);
            if (meta != null && now - meta.getUpdateTime() > contextConfig.getKeepaliveTime()) {
                contextMetas.remove(key);
            }
        }
    }

}

class ContextCleaner extends ScmTimerTask {
    private S3ListObjContextMgr mgr;

    public ContextCleaner(S3ListObjContextMgr mgr) {
        this.mgr = mgr;
    }

    @Override
    public void run() {
        mgr.cleanExpireContext();
    }

}
