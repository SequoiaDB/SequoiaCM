package com.sequoiacm.infrastructure.config.core.msg.workspace;

import com.sequoiacm.common.ScmSiteCacheStrategy;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.Converter;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Converter
public class WorkspaceBsonConverter implements BsonConverter {

    @Override
    public Config convertToConfig(BSONObject config) {
        WorkspaceConfig wsConfig = new WorkspaceConfig();

        String wsName = BsonUtils.getStringChecked(config, FieldName.FIELD_CLWORKSPACE_NAME);
        wsConfig.setWsName(wsName);

        int wsId = BsonUtils.getIntegerChecked(config, FieldName.FIELD_CLWORKSPACE_ID);
        wsConfig.setWsId(wsId);

        Number num = BsonUtils.getNumber(config, FieldName.FIELD_CLWORKSPACE_CREATETIME);
        if (num != null) {
            wsConfig.setCreateTime(num.longValue());
        }

        num = BsonUtils.getNumber(config, FieldName.FIELD_CLWORKSPACE_UPDATETIME);
        if (num != null) {
            wsConfig.setUpdateTime(num.longValue());
        }

        String desc = BsonUtils.getString(config, FieldName.FIELD_CLWORKSPACE_DESCRIPTION);
        wsConfig.setDesc(desc);

        String createUser = BsonUtils.getString(config, FieldName.FIELD_CLWORKSPACE_CREATEUSER);
        wsConfig.setCreateUser(createUser);

        String updateUser = BsonUtils.getString(config, FieldName.FIELD_CLWORKSPACE_UPDATEUSER);
        wsConfig.setUpdateUser(updateUser);

        BSONObject metaLocation = BsonUtils.getBSON(config,
                FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        wsConfig.setMetalocation(metaLocation);

        BasicBSONList dataLocations = BsonUtils.getArray(config,
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        wsConfig.setDataLocations(dataLocations);

        wsConfig.setExternalData(BsonUtils.getBSON(config, FieldName.FIELD_CLWORKSPACE_EXT_DATA));

        wsConfig.setBatchFileNameUnique(BsonUtils.getBooleanOrElse(config,
                FieldName.FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE, false));
        wsConfig.setBatchIdTimePattern(
                BsonUtils.getString(config, FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN));
        wsConfig.setBatchIdTimeRegex(
                BsonUtils.getString(config, FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX));
        wsConfig.setBatchShardingType(BsonUtils.getStringOrElse(config,
                FieldName.FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE, ScmShardingType.NONE.getName()));

        wsConfig.setEnableDirectory(BsonUtils.getBooleanOrElse(config,
                FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, false));

        wsConfig.setPreferred(BsonUtils.getString(config, FieldName.FIELD_CLWORKSPACE_PREFERRED));
        wsConfig.setSiteCacheStrategy(
                BsonUtils.getStringOrElse(config, FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY,
                        ScmSiteCacheStrategy.ALWAYS.name()));
        return wsConfig;
    }

    @Override
    public ConfigFilter convertToConfigFilter(BSONObject configFilter) {
        String wsName = BsonUtils.getString(configFilter,
                ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME);
        return new WorkspaceFilter(wsName);
    }

    @Override
    public NotifyOption convertToNotifyOption(EventType type, BSONObject configOption) {
        Integer version = BsonUtils.getInteger(configOption,
                ScmRestArgDefine.WORKSPACE_CONF_WORKSPACEVERSION);
        String wsName = BsonUtils.getStringChecked(configOption,
                ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME);
        return new WorkspaceNotifyOption(wsName, version, type);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject configUpdatorObj) {
        String wsName = BsonUtils.getStringChecked(configUpdatorObj,
                ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME);
        BSONObject oldWsRecord = BsonUtils.getBSON(configUpdatorObj,
                ScmRestArgDefine.WORKSPACE_CONF_OLD_WS);

        WorkspaceUpdator configUpdator = new WorkspaceUpdator(wsName, oldWsRecord);

        BSONObject wsUpdatorObj = BsonUtils.getBSONChecked(configUpdatorObj,
                ScmRestArgDefine.WORKSPACE_CONF_UPDATOR);
        configUpdator.setAddDataLocation(
                BsonUtils.getBSON(wsUpdatorObj, ScmRestArgDefine.WORKSPACE_CONF_ADD_DATALOCATION));
        configUpdator.setRemoveDataLocationId(BsonUtils.getInteger(wsUpdatorObj,
                ScmRestArgDefine.WORKSPACE_CONF_REMOVE_DATALOCATION));
        configUpdator.setNewDesc(
                BsonUtils.getString(wsUpdatorObj, ScmRestArgDefine.WORKSPACE_CONF_DESCRIPTION));
        configUpdator.setNewSiteCacheStrategy(BsonUtils.getString(wsUpdatorObj,
                ScmRestArgDefine.WORKSPACE_CONF_SITE_CACHE_STRATEGY));
        configUpdator.setExternalData(
                BsonUtils.getBSON(wsUpdatorObj, ScmRestArgDefine.WORKSPACE_CONF_EXTERNAL_DATA));
        configUpdator.setPreferred(
                BsonUtils.getString(wsUpdatorObj, ScmRestArgDefine.WORKSPACE_CONF_PREFERRED));
        return configUpdator;
    }

    @Override
    public VersionFilter convertToVersionFilter(BSONObject versionFilter) {
        return new DefaultVersionFilter(versionFilter);
    }

    @Override
    public Version convertToVersion(BSONObject version) {
        return new DefaultVersion(version);
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.WORKSPACE;
    }

}
