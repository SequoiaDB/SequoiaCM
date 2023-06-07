package com.sequoiacm.config.framework.workspace.metasource;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.util.Assert;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;


public class SysWorkspaceTableDaoSdbImpl extends SequoiadbTableDao implements SysWorkspaceTableDao {

    public SysWorkspaceTableDaoSdbImpl(SequoiadbMetasource sdbMetasource) {
        super(sdbMetasource, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE);
    }

    public SysWorkspaceTableDaoSdbImpl(Transaction transaction) {
        super(transaction, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE);
    }

    @Override
    public BSONObject removeDataLocation(BSONObject oldWsRecord, int siteId,
            BSONObject extraUpdator) throws MetasourceException {
        BasicBSONObject matcher = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION + "." + SequoiadbHelper.DOLLAR0 + "."
                        + FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID,
                siteId);
        BasicBSONList matcherList = new BasicBSONList();
        matcherList.add(matcher);
        matcherList.add(oldWsRecord);
        BasicBSONObject andMatcher = new BasicBSONObject(SequoiadbHelper.DOLLAR_AND, matcherList);

        BasicBSONObject unsetValue = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION + "." + SequoiadbHelper.DOLLAR0, "");
        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_UNSET, unsetValue);

        BSONObject updatedRecord = updateAndReturnNew(andMatcher, updater);
        if (updatedRecord == null){
            return null;
        }

        updatedRecord = deleteNullDataLocation(updatedRecord);
        Assert.notNull(updatedRecord, "updatedRecord can't be null!");
        return updatedRecord;
    }

    @Override
    // TODO:控制加站点加到数组的位置
    public BSONObject addDataLocation(BSONObject oldWsRecord, BSONObject location,
            BSONObject extraUpdator) throws MetasourceException {
        BasicBSONObject locationInfo = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, location);
        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_PUSH,
                locationInfo);

        return updateAndReturnNew(oldWsRecord, updater);
    }

    private BSONObject deleteNullDataLocation(BSONObject matcher) throws MetasourceException {
        BSONObject nullInList = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION,
                null);
        BSONObject pull = new BasicBSONObject(SequoiadbHelper.DOLLAR_PULL, nullInList);

        return updateAndReturnNew(matcher, pull);
    }

    @Override
    public BSONObject updateExternalData(BSONObject matcher, BSONObject externalData,
            BSONObject extraUpdator) throws MetasourceException {
        BSONObject extDatas = new BasicBSONObject();
        for (String key : externalData.keySet()) {
            extDatas.put(FieldName.FIELD_CLWORKSPACE_EXT_DATA + "." + key, externalData.get(key));
        }

        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_SET, extDatas);

        return updateAndReturnNew(matcher, updater);
    }

    @Override
    public BSONObject updateDataLocation(BSONObject matcher, BasicBSONList updater,
            BSONObject extraUpdator) throws ScmConfigException {
        BasicBSONList matcherList = new BasicBSONList();
        matcherList.add(matcher);

        BSONObject setValue = new BasicBSONObject();

        int dollarPos = 0;
        for (Object dataLocation : updater) {
            // 以siteId做匹配条件，匹配记录中的数组位置，set的时候set具体位置的 数组内容
            // set: $set:{"data_location.$0.":{"data_sharding_type": "month", "site_id": 2},
            //            "data_location.$1.":{"domain": "domain1", "site_id": 1, "data_sharding_type": {"collection_space": "month", "collection": "day"}}}
            // match: {"data_location.$0.site_id":2, "data_location.$1.site_id":1}
            BSONObject dataLocationMatcher = new BasicBSONObject(
                    FieldName.FIELD_CLWORKSPACE_DATA_LOCATION + "." + SequoiadbHelper.DOLLAR
                            + dollarPos + "." + FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID,
                    ((BSONObject)dataLocation).get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID));
            matcherList.add(dataLocationMatcher);

            setValue.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION
                    + "." + SequoiadbHelper.DOLLAR + dollarPos, dataLocation);

            ++dollarPos;
        }

        BSONObject allUpdater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_SET, setValue);
        BSONObject andMatcher = new BasicBSONObject(SequoiadbHelper.DOLLAR_AND, matcherList);

        return updateAndReturnNew(andMatcher, allUpdater);
    }

    @Override
    public BSONObject updateDescription(BSONObject matcher, String newDesc, BSONObject extraUpdator)
            throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_DESCRIPTION,
                newDesc);
        BSONObject siteCacheStrategyUpdater = combineUpdater(extraUpdator,
                SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, siteCacheStrategyUpdater);
    }

    @Override
    public BSONObject updateSiteCacheStrategy(BSONObject matcher, String newSiteCacheStrategy,
            BSONObject extraUpdator) throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY,
                newSiteCacheStrategy);
        BSONObject siteCacheStrategyUpdater = combineUpdater(extraUpdator,
                SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, siteCacheStrategyUpdater);
    }

    @Override
    public BSONObject updatePreferred(BSONObject matcher, String newPreferred,
            BSONObject extraUpdator) throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_PREFERRED,
                newPreferred);
        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, updater);
    }

    @Override
    public BSONObject updateDirectory(BSONObject matcher, Boolean isEnableDirectory,
            BSONObject extraUpdator) throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, isEnableDirectory);
        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, updater);
    }

    @Override
    public BSONObject updateMetaDomain(BSONObject oldWsRecord, String newDomain,
            BSONObject extraUpdator) throws ScmConfigException {
        if (!isDomainExist(newDomain)) {
            throw new ScmConfigException(ScmConfError.INVALID_ARG,
                    "Domain does not exist,domainName: " + newDomain);
        }
        // { $set: { "meta_location.domain": newDomain } }
        BasicBSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_META_LOCATION
                + "." + FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN, newDomain);
        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(oldWsRecord, updater);

    }

    @Override
    public BSONObject addExtraMetaCs(BSONObject matcher, String newCs, BSONObject extraUpdator)
            throws ScmConfigException {
        BSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_EXTRA_META_CS,
                newCs);
        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_PUSH, setUpdater);
        return updateAndReturnNew(matcher, updater);
    }

    @Override
    public BSONObject updateByNewAttribute(BSONObject matcher, BSONObject newInfo,
            BSONObject extraUpdator) throws ScmConfigException {
        BSONObject updater = combineUpdater(extraUpdator, SequoiadbHelper.DOLLAR_SET, newInfo);
        return updateAndReturnNew(matcher, updater);
    }

    private BSONObject combineUpdater(BSONObject extraUpdator, String operation, BSONObject value) {
        // 样例1：
        // extraUpdator：{$set:{version:2,update_user:"admin","update_time":1686106906493}}
        // operation: $set value: {preferred:"rootsite"}
        // 合并后：{$set:{version:2,update_user:"admin","update_time":1686106906493,
        // preferred:"rootsite"}}

        // 样例2：extraUpdator：{{$inc:{version:1}},{$set:
        // {update_user:"admin","update_time":1686106906493}}}
        // operation: $set value: {preferred:"rootsite"}
        // 合并后：{$inc:{version:1}},
        // $set:{preferred:"rootsite",update_user:"admin","update_time":1686106906493}}
        BSONObject update = new BasicBSONObject();
        if (extraUpdator.get(operation) != null) {
            BSONObject bsonObject = (BSONObject) extraUpdator.get(operation);
            bsonObject.putAll(value);
            update.putAll(extraUpdator);
        }
        else {
            update.put(operation, value);
            update.putAll(extraUpdator);
        }

        return update;
    }
}
