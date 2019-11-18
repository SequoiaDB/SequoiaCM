package com.sequoiacm.client.element;

import java.util.List;
import java.util.Map;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.common.mapping.ScmSiteObj;

/**
 * Class of ScmSiteInfo.
 *
 * @since 2.1
 */
public class ScmSiteInfo {
    private int id;
    private String name;
    private boolean isRootSite;

    private List<String> dataUrl;
    private String dataUser;
    private String dataPasswd;
    private DatasourceType dataTypeEnum;
    private String dataTypeStr;
    private Map<String, String> dataConf;

    private List<String> metaUrl;
    private String metaUser;
    private String metaPasswd;
    private String metaTypeStr;
    private DatasourceType metaTypeEnum;

    /**
     * Create a site info.
     *
     * @param obj
     *            a bson containing information about site info.
     * @throws ScmException
     *             if error happens.
     */
    public ScmSiteInfo(BSONObject obj) throws ScmException {
        try {
            ScmSiteObj siteObj = new ScmSiteObj(obj);
            this.id = siteObj.getId();
            this.name = siteObj.getName();
            this.isRootSite = siteObj.isRootSite();

            this.dataUrl = siteObj.getDataUrlList();
            this.dataTypeStr = siteObj.getDataType();
            this.dataTypeEnum = DatasourceType.getDatasourceType(dataTypeStr);

            this.dataUser = siteObj.getDataUser();
            this.dataPasswd = siteObj.getDataPasswd();
            this.dataConf = siteObj.getDataConf();

            if (isRootSite) {
                this.metaUrl = siteObj.getMetaUrlList();
                this.metaUser = siteObj.getMetaUser();
                this.metaPasswd = siteObj.getMetaPasswd();
                this.metaTypeStr = siteObj.getMetaType();
                this.metaTypeEnum = DatasourceType.getDatasourceType(metaTypeStr);
            }
        }
        catch (ScmMappingException e) {
            throw new ScmInvalidArgumentException(e.getMessage(), e);
        }
    }

    private DatasourceType convert2Enum(String dataType) {
        if (dataType == null) {
            return null;
        }
        if (CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR.equals(dataType)) {
            return DatasourceType.SEQUOIADB;
        }
        else if (CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR.equals(dataType)) {
            return DatasourceType.HBASE;
        }
        else if (CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR.equals(dataType)) {
            return DatasourceType.CEPH_S3;
        }
        else if (CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR.equals(dataType)) {
            return DatasourceType.CEPH_SWIFT;
        }
        else if (CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR.equals(dataType)) {
            return DatasourceType.HDFS;
        }
        else {
            return DatasourceType.UNKNOWN;
        }
    }

    /**
     * Get site id.
     *
     * @return Site id.
     * @since 2.1
     */
    public int getId() {
        return id;
    }

    /**
     * Get site name.
     *
     * @return Site name.
     * @since 2.1
     */
    public String getName() {
        return name;
    }

    /**
     * Whether the root site.
     *
     * @return True or false.
     */
    public boolean isRootSite() {
        return isRootSite;
    }

    /**
     * Get data service url list.
     *
     * @return Url list.
     * @since 2.1
     */
    public List<String> getDataUrl() {
        return dataUrl;
    }

    /**
     * Get data service user name.
     *
     * @return user name
     * @since 2.1
     */
    public String getDataUser() {
        return dataUser;
    }

    /**
     * Get data service password.
     *
     * @return password
     * @since 2.1
     */
    public String getDataPasswd() {
        return dataPasswd;
    }

    /**
     * Get data service Configuration for hdfs|hbase
     *
     * @return encrypt type
     * @since 2.1
     */
    public Map<String, String> getDataConf() {
        return dataConf;
    }

    /**
     * Get data service type enum.
     *
     * @return Data service type enum.
     * @since 2.1
     * @see com.sequoiacm.client.common.ScmType.DatasourceType
     */
    public DatasourceType getDataType() {
        return dataTypeEnum;
    }

    /**
     * Get data service type string.
     *
     * @return data service type string.
     * @since 2.1
     */
    public String getDataTypeStr() {
        return dataTypeStr;
    }

    /**
     * Get meta service url list.
     *
     * @return meta service url list
     * @since 2.1
     */
    public List<String> getMetaUrl() {
        return metaUrl;
    }

    /**
     * Get meta service user name.
     *
     * @return user name
     * @since 2.1
     */
    public String getMetaUser() {
        return metaUser;
    }

    /**
     * Get meta service password.
     *
     * @return password
     * @since 2.1
     */
    public String getMetaPasswd() {
        return metaPasswd;
    }

    public DatasourceType getMetaType() {
        return metaTypeEnum;
    }

    public String getMetaTypeStr() {
        return metaTypeStr;
    }

    @Override
    public String toString() {
        return "ScmSiteInfo [id=" + id + ", name=" + name + ", isRootSite=" + isRootSite
                + ", dataUrl=" + dataUrl + ", dataUser=" + dataUser + ", dataPasswd=" + dataPasswd
                + ", dataTypeEnum=" + dataTypeEnum + ", dataTypeStr=" + dataTypeStr + ", metaUrl="
                + metaUrl + ", metaUser=" + metaUser + ", metaPasswd=" + metaPasswd + "]";
    }

}
