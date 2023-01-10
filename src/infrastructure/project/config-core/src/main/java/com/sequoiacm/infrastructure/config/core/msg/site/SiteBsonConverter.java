package com.sequoiacm.infrastructure.config.core.msg.site;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
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
public class SiteBsonConverter implements BsonConverter {

    @Override
    public Config convertToConfig(BSONObject config) {
        SiteConfig siteConfig = new SiteConfig();
        Integer siteId = BsonUtils.getInteger(config, FieldName.FIELD_CLSITE_ID);
        if (siteId != null) {
            siteConfig.setId(siteId);
        }

        String name = BsonUtils.getStringChecked(config, FieldName.FIELD_CLSITE_NAME);
        siteConfig.setName(name);

        String stageTag = BsonUtils.getString(config,FieldName.FIELD_CLSITE_STAGE_TAG);
        siteConfig.setStageTag(stageTag);

        boolean isRootSite = BsonUtils.getBooleanChecked(config,
                FieldName.FIELD_CLSITE_MAINFLAG);
        siteConfig.setRootSite(isRootSite);

        BSONObject dataBSON = BsonUtils.getBSON(config, FieldName.FIELD_CLSITE_DATA);
        siteConfig.setDataSource(dataBSON);

        if (isRootSite) {
            BSONObject metaBSON = BsonUtils.getBSON(config, FieldName.FIELD_CLSITE_META);
            siteConfig.setMetaSource(metaBSON);
        }

        return siteConfig;
        // data source
        // String dataType = BsonUtils.getStringChecked(config,
        // FieldName.FIELD_CLSITE_SITE_DATA_TYPE);
        // String dataUser = BsonUtils.getStringChecked(config,
        // FieldName.FIELD_CLSITE_SITE_DATA_USER);
        // String dataPassword = BsonUtils.getStringChecked(config,
        // FieldName.FIELD_CLSITE_SITE_DATA_PASSWORD);
        // BSONObject dataUrl = BsonUtils.getBSONChecked(config,
        // FieldName.FIELD_CLSITE_SITE_DATA_URL);
        // BSONObject dataConfig = BsonUtils.getBSON(config,
        // FieldName.FIELD_CLSITE_SITE_DATA_CONF);

        // dataBSON.put(FieldName.FIELD_CLSITE_SITE_DATA_TYPE, dataType);
        // dataBSON.put(FieldName.FIELD_CLSITE_SITE_DATA_USER, dataUser);
        // dataBSON.put(FieldName.FIELD_CLSITE_SITE_DATA_PASSWORD,
        // dataPassword);
        // dataBSON.put(FieldName.FIELD_CLSITE_SITE_DATA_URL, dataUrl);
        // if (dataConfig != null) {
        // dataBSON.put(FieldName.FIELD_CLSITE_SITE_DATA_CONF, dataConfig);
        // }

        // meta source
        // if (isRootSite) {
        // String metaUser = BsonUtils.getStringChecked(config,
        // FieldName.FIELD_CLSITE_SITE_META_USER);
        // String metaPassword = BsonUtils.getStringChecked(config,
        // FieldName.FIELD_CLSITE_SITE_META_PASSWORD);
        // String metaUrl = BsonUtils.getStringChecked(config,
        // FieldName.FIELD_CLSITE_SITE_META_URL);
        //
        // BSONObject metaBSON = new BasicBSONObject();
        // metaBSON.put(FieldName.FIELD_CLSITE_SITE_META_USER, metaUser);
        // metaBSON.put(FieldName.FIELD_CLSITE_SITE_META_PASSWORD,
        // metaPassword);
        // metaBSON.put(FieldName.FIELD_CLSITE_SITE_META_URL, metaUrl);
        // siteConfig.setMetaSource(metaBSON);
        // }

    }

    @Override
    public ConfigFilter convertToConfigFilter(BSONObject configFilter) {
        String siteName = BsonUtils.getString(configFilter, ScmRestArgDefine.SITE_CONF_SITENAME);
        return new SiteFilter(siteName);
    }

    @Override
    public NotifyOption convertToNotifyOption(EventType type, BSONObject configOption) {
        Integer version = BsonUtils.getInteger(configOption,
                ScmRestArgDefine.SITE_CONF_SITEVERSION);
        String siteName = BsonUtils.getStringChecked(configOption,
                ScmRestArgDefine.SITE_CONF_SITENAME);
        return new SiteNotifyOption(siteName, version, type);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject configUpdatorObj) {
        String siteName = BsonUtils.getStringChecked(configUpdatorObj,ScmRestArgDefine.SITE_CONF_SITENAME);
        BSONObject updator = BsonUtils.getBSONChecked(configUpdatorObj,ScmRestArgDefine.SITE_CONF_UPDATOR);
        String updatorStageTag = BsonUtils.getStringChecked(updator,ScmRestArgDefine.SITE_CONF_STAGETAG);
        SiteUpdator siteUpdator = new SiteUpdator(siteName,updatorStageTag);
        return siteUpdator;
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
        return ScmConfigNameDefine.SITE;
    }

}
