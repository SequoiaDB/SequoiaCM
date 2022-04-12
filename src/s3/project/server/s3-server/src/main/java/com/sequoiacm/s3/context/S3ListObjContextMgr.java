package com.sequoiacm.s3.context;

import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.config.ContextConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class S3ListObjContextMgr {
    private ScmTimer timer;
    private ContextConfig contextConfig;

    @Autowired
    private MetaSourceService metaSourceService;

    @Autowired
    public S3ListObjContextMgr(ContextConfig contextConfig) {
        timer = ScmTimerFactory.createScmTimer();
        this.contextConfig = contextConfig;
        timer.schedule(new ContextCleaner(this), contextConfig.getCleanPeriod(),
                contextConfig.getCleanPeriod());
    }

    @PreDestroy
    public void destroy() {
        timer.cancel();
    }

    public S3ListObjectContext createContext(String prefix, String startAfter, String delimiter,
            String bucketName) throws S3ServerException {
        S3ListObjectContext context = new S3ListObjectContext(prefix, startAfter, delimiter,
                bucketName, false, this);
        return context;
    }

    public S3ListObjectContext getContext(String token) throws S3ServerException {
        try {
            MetaAccessor accessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.LIST_OBJECT_CONTEXT_TABLE_NAME);
            BSONObject record = accessor.queryOne(
                    new BasicBSONObject(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_TOKEN, token),
                    null, null);
            if (record == null) {
                throw new S3ServerException(S3Error.OBJECT_INVALID_TOKEN,
                        "The continuation token provided is incorrect.token:" + token);
            }
            return new S3ListObjectContext(record, true, this);
        }
        catch (ScmMetasourceException | ScmServerException e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR,
                    "failed to get context: token=" + token, e);
        }
    }

    void save(S3ListObjectContext context) throws S3ServerException {
        try {
            MetaAccessor accessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.LIST_OBJECT_CONTEXT_TABLE_NAME);
            if (context.isPersistence()) {
                BasicBSONObject matcher = new BasicBSONObject(
                        S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_TOKEN, context.getToken());

                BasicBSONObject newValue = new BasicBSONObject();
                newValue.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_LAST_MARKER,
                        context.getLastMarker());
                newValue.put(S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_LAST_ACCESS_TIME,
                        context.getLastAccessTime());

                BasicBSONObject updater = new BasicBSONObject("$set", newValue);
                accessor.update(matcher, updater);
                return;
            }
            accessor.insert(context.toBSON());
        }
        catch (ScmMetasourceException | ScmServerException e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR,
                    "failed to save context:" + context, e);
        }

    }

    public void remove(String token) throws S3ServerException {
        try {
            MetaAccessor accessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.LIST_OBJECT_CONTEXT_TABLE_NAME);
            BasicBSONObject matcher = new BasicBSONObject(
                    S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_TOKEN, token);
            accessor.delete(matcher);
        }
        catch (ScmMetasourceException | ScmServerException e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR,
                    "failed to remove context: token=" + token, e);
        }
    }

    void cleanExpireContext() throws Exception {
        long now = System.currentTimeMillis();
        long timeForClean = now - contextConfig.getKeepaliveTime();
        MetaAccessor accessor = metaSourceService.getMetaSource()
                .createMetaAccessor(S3CommonDefine.LIST_OBJECT_CONTEXT_TABLE_NAME);
        BasicBSONObject matcher = new BasicBSONObject(
                S3CommonDefine.LIST_OBJECT_CONTEXT_FIELD_LAST_ACCESS_TIME,
                new BasicBSONObject("$lt", timeForClean));
        accessor.delete(matcher);
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
        catch (Exception e) {
            logger.warn("failed to clean context", e);
        }
    }

}
