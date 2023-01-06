package com.sequoiacm.config.framework.workspace.metasource;

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

import java.util.List;

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
    public BSONObject removeDataLocation(BSONObject oldWsRecord, int siteId, BSONObject versionSet)
            throws MetasourceException {
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
        BSONObject updater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_UNSET, unsetValue);

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
    public BSONObject addDataLocation(BSONObject oldWsRecord, BSONObject location, BSONObject versionSet)
            throws MetasourceException {
        BasicBSONObject locationInfo = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, location);
        BSONObject updater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_PUSH, locationInfo);

        return updateAndReturnNew(oldWsRecord, updater);
    }

    private BSONObject deleteNullDataLocation(BSONObject matcher) throws MetasourceException {
        BSONObject nullInList = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION,
                null);
        BSONObject pull = new BasicBSONObject(SequoiadbHelper.DOLLAR_PULL, nullInList);

        return updateAndReturnNew(matcher, pull);
    }

    @Override
    public BSONObject updateExternalData(BSONObject matcher, BSONObject externalData, BSONObject versionSet)
            throws MetasourceException {
        BSONObject extDatas = new BasicBSONObject();
        for (String key : externalData.keySet()) {
            extDatas.put(FieldName.FIELD_CLWORKSPACE_EXT_DATA + "." + key, externalData.get(key));
        }

        BSONObject updater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_SET, extDatas);

        return updateAndReturnNew(matcher, updater);
    }

    @Override
    public BSONObject updateDataLocation(BSONObject matcher, BasicBSONList updater,
            BSONObject versionSet) throws ScmConfigException {
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

        BSONObject allUpdater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_SET, setValue);
        BSONObject andMatcher = new BasicBSONObject(SequoiadbHelper.DOLLAR_AND, matcherList);

        return updateAndReturnNew(andMatcher, allUpdater);
    }

    @Override
    public BSONObject updateDescription(BSONObject matcher, String newDesc, BSONObject versionSet) throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_DESCRIPTION,
                newDesc);
        BSONObject siteCacheStrategyUpdater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, siteCacheStrategyUpdater);
    }

    @Override
    public BSONObject updateSiteCacheStrategy(BSONObject matcher, String newSiteCacheStrategy, BSONObject versionSet) throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY,
                newSiteCacheStrategy);
        BSONObject siteCacheStrategyUpdater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, siteCacheStrategyUpdater);
    }

    @Override
    public BSONObject updatePreferred(BSONObject matcher, String newPreferred, BSONObject versionSet) throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_PREFERRED,
                newPreferred);
        BSONObject updater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, updater);
    }

    @Override
    public BSONObject updateDirectory(BSONObject matcher, Boolean isEnableDirectory, BSONObject versionSet)
            throws ScmConfigException {
        BasicBSONObject setUpdater = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, isEnableDirectory);
        BSONObject updater = combineUpdater(versionSet, SequoiadbHelper.DOLLAR_SET, setUpdater);

        return updateAndReturnNew(matcher, updater);
    }

    private BSONObject combineUpdater(BSONObject versionUpdate, String operation, BSONObject value){
        // 样例1：versionUpdate：{$set:{version:2}}   operation: $set   value: {preferred:"rootsite"}
        // 合并后：{$set:{version:2, preferred:"rootsite"}}
        // 样例2：versionUpdate：{$inc:{version:1}}   operation: $set   value: {preferred:"rootsite"}
        // 合并后：{$inc:{version:1}}, $set:{preferred:"rootsite"}}
        BSONObject update = new BasicBSONObject();
        if (versionUpdate.get(operation) != null) {
            value.putAll((BSONObject) versionUpdate.get(operation));
            update.put(operation, value);
        }
        else {
            update.put(operation, value);
            update.putAll(versionUpdate);
        }

        return update;
    }
}
