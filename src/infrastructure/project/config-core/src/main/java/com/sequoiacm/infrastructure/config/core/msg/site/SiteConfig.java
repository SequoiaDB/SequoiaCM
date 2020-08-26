package com.sequoiacm.infrastructure.config.core.msg.site;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class SiteConfig implements Config {
    private int id;
    private String name;
    private boolean isRootSite;
    private BSONObject dataSource;
    private BSONObject metaSource;

    public boolean isRootSite() {
        return isRootSite;
    }

    public BSONObject getDataSource() {
        return dataSource;
    }

    public BSONObject getMetaSource() {
        return metaSource;
    }

    public void setRootSite(boolean isRootSite) {
        this.isRootSite = isRootSite;
    }

    public void setDataSource(BSONObject dataSource) {
        this.dataSource = dataSource;
    }

    public void setMetaSource(BSONObject metaSource) {
        this.metaSource = metaSource;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject siteConfigObj = new BasicBSONObject();
        siteConfigObj.put(FieldName.FIELD_CLSITE_SITE_ID, id);
        siteConfigObj.put(FieldName.FIELD_CLSITE_SITE_NAME, name);
        siteConfigObj.put(FieldName.FIELD_CLSITE_SITE_ROOT_SITE_FLAG, isRootSite);
        siteConfigObj.put(FieldName.FIELD_CLSITE_SITE_DATA, dataSource);
        if (metaSource != null) {
            siteConfigObj.put(FieldName.FIELD_CLSITE_SITE_META, metaSource);
        }
        return siteConfigObj;
    }

    @Override
    public String toString() {
        return "SiteConfig [id=" + id + ", name=" + name + ", isRootSite=" + isRootSite
                + ", dataSource=" + dataSource + ", metaSource=" + metaSource + "]";
    }

}
