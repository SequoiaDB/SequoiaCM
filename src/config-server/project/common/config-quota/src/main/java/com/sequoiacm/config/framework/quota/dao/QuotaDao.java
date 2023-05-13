package com.sequoiacm.config.framework.quota.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.quota.metasource.QuotaMetaService;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.IndexDef;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmModifierDefine;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaBsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaFilter;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaUpdator;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaVersionFilter;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class QuotaDao {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(QuotaDao.class);

    private static final String BUCKET_QUOTA_TYPE = "bucket";

    @Autowired
    private QuotaMetaService metaService;

    @Autowired
    private Metasource metasource;

    @Autowired
    private QuotaBsonConverter converter;

    @PostConstruct
    public void init() throws MetasourceException {
        IndexDef indexDef = new IndexDef();
        indexDef.setUnique(true);
        indexDef.setUnionKeys(Arrays.asList(FieldName.Quota.TYPE, FieldName.Quota.NAME));
        metaService.getQuotaTable().ensureTable(Collections.singletonList(indexDef));
    }

    public ScmConfOperateResult crateQuota(QuotaConfig config) throws ScmConfigException {
        logger.info("start create {} quota: name={}", config.getType(), config.getName());
        create(config);
        logger.info("create {} quota success: name={}", config.getType(), config.getName());
        ScmConfEvent event = createQuotaEvent(config);
        return new ScmConfOperateResult(config, event);
    }

    private void create(QuotaConfig config) throws ScmConfigException {
        try {
            try {
                TableDao quotaTable = metaService.getQuotaTable();
                BSONObject bsonObject = config.toBSONObject();
                bsonObject.put(FieldName.Quota.UPDATE_TIME, System.currentTimeMillis());
                bsonObject.put(FieldName.Quota.VERSION, 1);
                // 已用字节数、已用对象数由内容服务负责更新，不会解析到 QuotaConfig 中
                bsonObject.put(FieldName.Quota.USED_SIZE, 0L);
                bsonObject.put(FieldName.Quota.USED_OBJECTS, 0L);
                quotaTable.insert(bsonObject);
            }
            catch (MetasourceException e) {
                if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.QUOTA_EXIST,
                            config.getType() + " quota is already exist:name=" + config.getName(),
                            e);
                }
                throw e;
            }
        }
        catch (ScmConfigException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to create " + config.getType() + " quota:name=" + config.getName(), e);
        }

    }

    private ScmConfEvent createQuotaEvent(QuotaConfig quotaConfig) {
        QuotaNotifyOption notifyCation = new QuotaNotifyOption(quotaConfig.getType(),
                quotaConfig.getName(), 1, EventType.CREATE);
        return new ScmConfEventBase(ScmConfigNameDefine.QUOTA, notifyCation);
    }

    public ScmConfOperateResult deleteQuota(QuotaFilter filter) throws ScmConfigException {
        logger.info("start to delete quota:filter={}", filter.toBSONObject());
        QuotaConfig quotaConfig = delete(filter);
        logger.info("delete quota success:filter={}", filter.toBSONObject());
        ScmConfEvent event = deleteQuotaEvent(quotaConfig);
        return new ScmConfOperateResult(quotaConfig, event);
    }

    private ScmConfEvent deleteQuotaEvent(QuotaConfig quotaConfig) {
        QuotaNotifyOption notifyCation = new QuotaNotifyOption(quotaConfig.getType(),
                quotaConfig.getName(), 1, EventType.DELTE);
        return new ScmConfEventBase(ScmConfigNameDefine.QUOTA, notifyCation);
    }

    private QuotaConfig delete(QuotaFilter filter) throws ScmConfigException {
        MetaCursor quotaCursor = null;
        try {
            TableDao quotaTable = metaService.getQuotaTable();
            quotaCursor = quotaTable.query(filter.toBSONObject(), null, null);
            if (quotaCursor.hasNext()) {
                BSONObject quotaBson = quotaCursor.getNext();
                if (quotaCursor.hasNext()) {
                    BSONObject secondQuota = quotaCursor.getNext();
                    throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                            "have two specified quota to delete, please check quota table: firstQuota="
                                    + quotaBson + ", secondQuota=" + secondQuota);
                }

                QuotaConfig quotaConfig = (QuotaConfig) new QuotaBsonConverter()
                        .convertToConfig(quotaBson);

                quotaTable.delete(filter.toBSONObject());
                return quotaConfig;
            }
            throw new ScmConfigException(ScmConfError.QUOTA_NOT_EXIST,
                    "quota is not exist: filter=" + filter.toBSONObject());
        }
        catch (ScmConfigException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "delete quota failed:filter =" + filter.toBSONObject(), e);
        }
        finally {
            if (quotaCursor != null) {
                quotaCursor.close();
            }
        }
    }

    public ScmConfOperateResult updateQuota(QuotaUpdator updator) throws ScmConfigException {
        logger.info("start to update quota:bson={}", updator.toBSONObject());
        ScmConfOperateResult scmConfOperateResult = update(updator);
        logger.info("update quota success:bson={}", updator.toBSONObject());
        return scmConfOperateResult;
    }

    public ScmConfOperateResult update(QuotaUpdator updator) throws ScmConfigException {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        try {
            SequoiadbTableDao quotaTable = metaService.getQuotaTable();
            BSONObject match = new BasicBSONObject(FieldName.Quota.TYPE, updator.getType());
            match.put(FieldName.Quota.NAME, updator.getName());
            if (updator.getMatcher() != null) {
                match.putAll(updator.getMatcher());
            }
            BSONObject quotaBSON = quotaTable.queryOne(match, null, null);
            if (quotaBSON != null) {
                BSONObject updateBson = createUpdateBson(updator);
                BSONObject incrModifier = new BasicBSONObject(FieldName.Quota.VERSION, 1);
                updateBson.put(ScmModifierDefine.SEQUOIADB_MODIFIER_INC, incrModifier);
                BSONObject newQuotaConfig = quotaTable.updateAndReturnNew(match, updateBson);
                int newVersion = BsonUtils.getNumberChecked(newQuotaConfig, FieldName.Quota.VERSION)
                        .intValue();
                QuotaConfig newConfig = (QuotaConfig) new QuotaBsonConverter()
                        .convertToConfig(newQuotaConfig);
                ScmConfEvent event = updateQuotaEvent(newConfig, newVersion);
                opRes.setConfig(newConfig);
                opRes.addEvent(event);
                return opRes;
            }
            throw new ScmConfigException(ScmConfError.QUOTA_NOT_EXIST,
                    updator.getType() + " quota is not exist:name=" + updator.getName());
        }
        catch (ScmConfigException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to update " + updator.getType() + " quota:name=" + updator.getName(),
                    e);
        }
    }

    private BSONObject createUpdateBson(QuotaUpdator updator) {
        BSONObject updateBson = new BasicBSONObject();
        if (updator.getEnable() != null) {
            updateBson.put(FieldName.Quota.ENABLE, updator.getEnable());
        }
        if (updator.getMaxObjects() != null) {
            updateBson.put(FieldName.Quota.MAX_OBJECTS, updator.getMaxObjects());
        }
        if (updator.getMaxSize() != null) {
            updateBson.put(FieldName.Quota.MAX_SIZE, updator.getMaxSize());
        }
        if (updator.getExtraInfo() != null) {
            updateBson.put(FieldName.Quota.EXTRA_INFO, updator.getExtraInfo());
        }
        if (updator.getQuotaRoundNumber() != null) {
            updateBson.put(FieldName.Quota.QUOTA_ROUND_NUMBER, updator.getQuotaRoundNumber());
        }
        if (updator.getUsedObjects() != null) {
            updateBson.put(FieldName.Quota.USED_OBJECTS, updator.getUsedObjects());
        }
        if (updator.getUsedSize() != null) {
            updateBson.put(FieldName.Quota.USED_SIZE, updator.getUsedSize());
        }
        updateBson.put(FieldName.Quota.UPDATE_TIME, System.currentTimeMillis());
        return new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, updateBson);
    }

    private ScmConfEvent updateQuotaEvent(QuotaConfig quotaConfig, Integer newVersion) {
        QuotaNotifyOption notifyOption = new QuotaNotifyOption(quotaConfig.getType(),
                quotaConfig.getName(), newVersion, EventType.UPDATE);
        return new ScmConfEventBase(ScmConfigNameDefine.QUOTA, notifyOption);
    }

    public List<Config> getQuotas(QuotaFilter filter) throws MetasourceException {
        List<Config> res = new ArrayList<>();
        SequoiadbTableDao quotaTable = metaService.getQuotaTable();
        MetaCursor cursor = null;
        try {
            cursor = quotaTable.query(filter.toBSONObject(), null, null);
            while (cursor.hasNext()) {
                BSONObject bson = cursor.getNext();
                QuotaConfig quotaConfig = (QuotaConfig) converter.convertToConfig(bson);
                res.add(quotaConfig);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;

    }

    public void deleteBucketQuotaSilence(String bucketName) {
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.Quota.TYPE, BUCKET_QUOTA_TYPE);
            matcher.put(FieldName.Quota.NAME, bucketName);
            TableDao quotaTable = metaService.getQuotaTable();
            quotaTable.delete(matcher);
        }
        catch (Exception e) {
            logger.warn("failed to delete quota meta:bucket={}", bucketName, e);
        }
    }

    public void deleteWsBucketQuotaSilence(String wsName) {
        Transaction trans = null;
        MetaCursor cursor = null;
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.Quota.TYPE, BUCKET_QUOTA_TYPE);
            matcher.put(FieldName.Quota.EXTRA_INFO + "." + FieldName.Quota.EXTRA_INFO_WORKSPACE,
                    wsName);

            trans = metasource.createTransaction();
            TableDao quotaTable = metaService.getQuotaTable(trans);
            trans.begin();
            cursor = quotaTable.query(matcher, null, null);
            while (cursor.hasNext()) {
                BSONObject next = cursor.getNext();
                String bucketName = BsonUtils.getStringChecked(next, FieldName.Quota.NAME);
                BSONObject deleteMatcher = new BasicBSONObject();
                deleteMatcher.put(FieldName.Quota.TYPE, BUCKET_QUOTA_TYPE);
                deleteMatcher.put(FieldName.Quota.NAME, bucketName);
                quotaTable.delete(deleteMatcher);
            }
            trans.commit();
        }
        catch (Exception e) {
            if (trans != null) {
                trans.rollback();
            }
            logger.warn("failed to delete quota meta, wsName={}", wsName, e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (trans != null) {
                trans.close();
            }
        }
    }

    public List<Version> getVersions(QuotaVersionFilter versionFilter) throws ScmConfigException {
        List<Version> res = new ArrayList<>();
        SequoiadbTableDao quotaTable = metaService.getQuotaTable();
        MetaCursor cursor = null;
        try {
            cursor = quotaTable.query(versionFilter.toBSONObject(), null, null);
            while (cursor.hasNext()) {
                BSONObject bson = cursor.getNext();
                String type = BsonUtils.getStringChecked(bson, FieldName.Quota.TYPE);
                String name = BsonUtils.getStringChecked(bson, FieldName.Quota.NAME);
                int versionNum = BsonUtils.getNumberChecked(bson, FieldName.Quota.VERSION)
                        .intValue();
                DefaultVersion version = new DefaultVersion(ScmConfigNameDefine.QUOTA,
                        QuotaConfig.toBusinessName(type, name), versionNum);
                res.add(version);
            }
        }
        catch (MetasourceException e) {
            throw new ScmConfigException(ScmConfError.CONFIG_ERROR, "failed to get quota versions",
                    e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }
}
