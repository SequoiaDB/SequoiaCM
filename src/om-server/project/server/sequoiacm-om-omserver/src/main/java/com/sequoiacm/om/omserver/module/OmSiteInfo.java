package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.common.ScmType;

import java.util.List;
import java.util.Map;

public class OmSiteInfo {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("is_root_site")
    private boolean isRootSite;

    @JsonProperty("datasource_type")
    private String datasourceType;

    @JsonProperty("datasource_user")
    private String datasourceUser;

    @JsonProperty("datasource_pwd")
    private String datasourcePwd;

    @JsonProperty("metasource_user")
    private String metasourceUser;

    @JsonProperty("metasource_pwd")
    private String metasourcePwd;

    @JsonProperty("datasource_conf")
    private Map<String, String> datasourceConf;

    @JsonProperty("datasource_url")
    private List<String> datasourceUrl;

    @JsonProperty("metasource_url")
    private List<String> metasourceUrl;

    @JsonIgnore
    private ScmType.DatasourceType datasourceTypeEnum;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRootSite() {
        return isRootSite;
    }

    public void setRootSite(boolean isRootSite) {
        this.isRootSite = isRootSite;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getDatasourceUser() {
        return datasourceUser;
    }

    public void setDatasourceUser(String datasourceUser) {
        this.datasourceUser = datasourceUser;
    }


    public String getDatasourcePwd() {
        return datasourcePwd;
    }

    public void setDatasourcePwd(String datasourcePwd) {
        this.datasourcePwd = datasourcePwd;
    }



    public String getMetasourceUser() {
        return metasourceUser;
    }

    public void setMetasourceUser(String metasourceUser) {
        this.metasourceUser = metasourceUser;
    }

    public String getMetasourcePwd() {
        return metasourcePwd;
    }

    public void setMetasourcePwd(String metasourcePwd) {
        this.metasourcePwd = metasourcePwd;
    }

    public Map<String, String> getDatasourceConf() {
        return datasourceConf;
    }

    public void setDatasourceConf(Map<String, String> datasourceConf) {
        this.datasourceConf = datasourceConf;
    }

    public List<String> getDatasourceUrl() {
        return datasourceUrl;
    }

    public void setDatasourceUrl(List<String> datasourceUrl) {
        this.datasourceUrl = datasourceUrl;
    }

    public List<String> getMetasourceUrl() {
        return metasourceUrl;
    }

    public void setMetasourceUrl(List<String> metasourceUrl) {
        this.metasourceUrl = metasourceUrl;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public ScmType.DatasourceType getDatasourceTypeEnum() {
        return datasourceTypeEnum;
    }

    public void setDatasourceTypeEnum(ScmType.DatasourceType datasourceTypeEnum) {
        this.datasourceTypeEnum = datasourceTypeEnum;
    }
}
