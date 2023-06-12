package com.sequoiacm.infrastructure.config.core.msg.site;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.SITE)
public class SiteConfig implements Config {

    @JsonProperty(FieldName.FIELD_CLSITE_ID)
    private int id;

    @JsonProperty(FieldName.FIELD_CLSITE_NAME)
    private String name;

    @JsonProperty(FieldName.FIELD_CLSITE_MAINFLAG)
    private boolean isRootSite;

    @JsonProperty(FieldName.FIELD_CLSITE_STAGE_TAG)
    private String stageTag;

    @JsonProperty(FieldName.FIELD_CLSITE_DATA)
    private BSONObject dataSource;

    @JsonProperty(FieldName.FIELD_CLSITE_META)
    private BSONObject metaSource;

    public SiteConfig() {
    }

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

    public String getStageTag() {
        return stageTag;
    }

    public void setStageTag(String stageTag) {
        this.stageTag = stageTag;
    }


    @Override
    public String toString() {
        return "SiteConfig [id=" + id + ", name=" + name + ", isRootSite=" + isRootSite
                + ", stageTag=" + stageTag
                + ", dataSource=" + dataSource + ", metaSource=" + metaSource + "]";
    }

    @JsonIgnore
    @Override
    public String getBusinessName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteConfig that = (SiteConfig) o;
        return id == that.id && isRootSite == that.isRootSite && Objects.equals(name, that.name) && Objects.equals(stageTag, that.stageTag) && Objects.equals(dataSource, that.dataSource) && Objects.equals(metaSource, that.metaSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, isRootSite, stageTag, dataSource, metaSource);
    }
}
