package com.sequoiacm.tools.element;

import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.FieldName;

/**
 * Site config class.
 */
public class ScmSiteConfig {
    private String name;
    private boolean isRootSite;

    private DatasourceType dataType;
    private String dataUser;
    // data password is path
    private String dataPassword;
    private List<String> dataUrl;
    private Map<String, String> dataConfig;

    // root site property
    private String metaUser;
    // meta password is path
    private String metaPassword;
    private List<String> metaUrl;

    private ScmSiteConfig(Builder builder) {
        this.name = builder.name;
        this.isRootSite = builder.isRootSite;
        this.dataType = builder.dataType;
        this.dataUser = builder.dataUser;
        this.dataPassword = builder.dataPassword;
        this.dataUrl = builder.dataUrl;
        this.dataConfig = builder.dataConfig;
        if (isRootSite) {
            this.metaUser = builder.metaUser;
            this.metaPassword = builder.metaPassword;
            this.metaUrl = builder.metaUrl;
        }
    }

    /**
     * start build site config instance.
     *
     * @param siteName
     *            site name.
     * @return Site config builder instance.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public static Builder start(String siteName) throws ScmInvalidArgumentException {
        return new Builder(siteName);
    }

    /**
     * Site config builder class.
     */
    public static class Builder {
        private String name;
        private boolean isRootSite = false;
        private DatasourceType dataType = DatasourceType.SEQUOIADB;
        private String dataUser;
        // password is path
        private String dataPassword;
        private List<String> dataUrl;
        private Map<String, String> dataConfig;
        private String metaUser;
        // password is path
        private String metaPassword;
        private List<String> metaUrl;

        /**
         * construct site builder instance.
         * 
         * @param siteName
         *            site name.
         * @throws ScmInvalidArgumentException
         *             if error happens.
         */
        private Builder(String siteName) throws ScmInvalidArgumentException {
            if (siteName == null || siteName.trim().isEmpty()) {
                throw new ScmInvalidArgumentException("site name is null or empty");
            }
            this.name = siteName;
        }

        /**
         * Setter the root site.
         *
         * @param isRootSite
         *            is root site, default value false.
         * @return Site config builder.
         */
        public Builder isRootSite(boolean isRootSite) {
            this.isRootSite = isRootSite;
            return this;
        }

        /**
         * Setter data source type
         *
         * @param dataType
         *            data source type,default value SEQUOIADB.
         * @return Site config builder instance.
         * @throws ScmInvalidArgumentException
         *             if error happens.
         */
        public Builder SetDataSourceType(DatasourceType dataType)
                throws ScmInvalidArgumentException {
            if (dataType == null) {
                throw new ScmInvalidArgumentException("data source type is null");
            }
            this.dataType = dataType;
            return this;

        }

        /**
         * Setter data source.
         *
         * @param dataUrl
         *            data source urls.
         * @param dataUser
         *            data source username.
         * @param dataPassword
         *            data source password, password is path.
         * @param dataConfig
         *            data source config options.
         * @return Site config builder instance.
         * @throws ScmInvalidArgumentException
         *             if error happens.
         */
        public Builder setDataSource(List<String> dataUrl, String dataUser, String dataPassword,
                Map<String, String> dataConfig) throws ScmInvalidArgumentException {
            if (dataUrl == null || dataUrl.size() == 0) {
                throw new ScmInvalidArgumentException(
                        "data source urls is null or dataUrl size is 0");
            }
            if (dataUser == null) {
                throw new ScmInvalidArgumentException("data source username is null");
            }
            if (dataPassword == null) {
                throw new ScmInvalidArgumentException("data source password is null");
            }

            this.dataUrl = dataUrl;
            this.dataUser = dataUser;
            this.dataPassword = dataPassword;
            if (dataConfig != null) {
                this.dataConfig = dataConfig;
            }
            return this;
        }

        /**
         * Setter meta source.
         *
         * @param metaUrl
         *            meta source urls.
         * @param metaUser
         *            meta source username.
         * @param metaPassword
         *            meta source password, data password is path.
         * @return Site config builder instance.
         * @throws ScmInvalidArgumentException
         *             if error happens.
         */
        public Builder setMetaSource(List<String> metaUrl, String metaUser, String metaPassword)
                throws ScmInvalidArgumentException {
            if (metaUrl == null || metaUrl.size() == 0) {
                throw new ScmInvalidArgumentException(
                        "meta source urls is null or dataUrl size is 0");
            }
            if (metaUser == null) {
                throw new ScmInvalidArgumentException("meta source username is null");
            }
            if (metaPassword == null) {
                throw new ScmInvalidArgumentException("meta source password is null");
            }

            this.metaUser = metaUser;
            this.metaPassword = metaPassword;
            this.metaUrl = metaUrl;
            return this;
        }

        /**
         * finish build site config.
         *
         * @return site config instance.
         */
        public ScmSiteConfig build() {
            return new ScmSiteConfig(this);
        }

    }

    /**
     * site config transform bson.
     * 
     * @return bson instance.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public BSONObject toBsonObject() throws ScmInvalidArgumentException {
        BasicBSONObject confBson = new BasicBSONObject();

        confBson.put(FieldName.FIELD_CLSITE_NAME, name);
        confBson.put(FieldName.FIELD_CLSITE_MAINFLAG, isRootSite);

        BasicBSONObject dataBson = new BasicBSONObject();
        dataBson.put(FieldName.FIELD_CLSITE_DATA_TYPE, dataType.toString());
        dataBson.put(FieldName.FIELD_CLSITE_USER, dataUser);
        dataBson.put(FieldName.FIELD_CLSITE_PASSWD, dataPassword);
        dataBson.put(FieldName.FIELD_CLSITE_URL, dataUrl);
        if (dataConfig != null) {
            dataBson.put(FieldName.FIELD_CLSITE_CONF, dataConfig);
        }
        confBson.put(FieldName.FIELD_CLSITE_DATA, dataBson);
        if (!isRootSite) {
            return confBson;
        }

        BasicBSONObject metaBson = new BasicBSONObject();
        metaBson.put(FieldName.FIELD_CLSITE_USER, metaUser);
        metaBson.put(FieldName.FIELD_CLSITE_PASSWD, metaPassword);
        metaBson.put(FieldName.FIELD_CLSITE_URL, metaUrl);
        confBson.put(FieldName.FIELD_CLSITE_META, metaBson);
        return confBson;
    }

    /**
     * Gets the site name.
     *
     * @return Site name.
     */
    public String getName() {
        return name;
    }

    /**
     * Is root site.
     *
     * @return true or false.
     */
    public boolean isRootSite() {
        return isRootSite;
    }

    /**
     * Gets the data source type.
     *
     * @return data source type.
     */
    public DatasourceType getDataType() {
        return dataType;
    }

    /**
     * Gets the data source user name.
     *
     * @return user name.
     */
    public String getDataUser() {
        return dataUser;
    }

    /**
     * Gets the data source password.
     *
     * @return password.
     */
    public String getDataPassword() {
        return dataPassword;
    }

    /**
     * Gets the data source urls.
     *
     * @return urls.
     */
    public List<String> getDataUrl() {
        return dataUrl;
    }

    /**
     * Gets the data source configuration.
     *
     * @return data source configuration.
     */
    public Map<String, String> getDataConfig() {
        return dataConfig;
    }

    /**
     * Gets the meta source urls.
     *
     * @return urls.
     */
    public List<String> getMetaUrl() {
        return metaUrl;
    }

    /**
     * Gets the meta source user name.
     *
     * @return user name.
     */
    public String getMetaUser() {
        return metaUser;
    }

    /**
     * Gets the meta source password.
     *
     * @return password.
     */
    public String getMetaPassword() {
        return metaPassword;
    }

}
