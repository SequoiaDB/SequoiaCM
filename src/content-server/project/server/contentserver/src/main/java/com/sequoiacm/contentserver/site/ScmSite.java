package com.sequoiacm.contentserver.site;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.mapping.ScmSiteObj;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.HadoopSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiacm.exception.ScmError;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.net.ConfigOptions;

public class ScmSite {
    private static final Logger logger = LoggerFactory.getLogger(ScmSite.class);
    private String name;
    private int id;
    private boolean isRootSite;
    private ScmSiteUrl metaUrl = null;
    private ScmSiteUrl dataUrl = null;

    public ScmSite(BSONObject record) throws ScmServerException {
        try {
            logger.debug("create ScmSite , get record :" + record.toString());
            ScmSiteObj siteObj = new ScmSiteObj(record);
            this.name = siteObj.getName();
            this.id = siteObj.getId();
            this.isRootSite = siteObj.isRootSite();
            this.dataUrl = createScmSiteUrl(siteObj);
            //            if (siteObj.getDataType().equals(ScmDataSourceType.SEQUOIADB.getName())) {
            //                this.dataUrl = createSdbSiteUrl(siteObj, false);
            //            }
            //            else if (siteObj.getDataType().equals(ScmDataSourceType.HDFS.getName())
            //                    || siteObj.getDataType().equals(ScmDataSourceType.HBASE.getName())) {
            //                logger.debug("create ScmSite , siteObj.getDataConf() : " + siteObj.getDataConf().toString());
            //                this.dataUrl = new HadoopSiteUrl(siteObj.getDataType(), siteObj.getDataUrlList(),
            //                        siteObj.getDataUser(), siteObj.getDataPasswd(), siteObj.getDataPasswdType(), siteObj.getDataConf());
            //            }
            //            else {
            //                this.dataUrl = new ScmSiteUrl(siteObj.getDataType(), siteObj.getDataUrlList(),
            //                        siteObj.getDataUser(), siteObj.getDataPasswd(), siteObj.getDataPasswdType());
            //            }

            if (siteObj.isRootSite()) {
                //TODO:
                if (siteObj.getMetaType().equals(ScmDataSourceType.SEQUOIADB.getName())) {
                    this.metaUrl = createSdbSiteUrl(siteObj, true);
                }
                else {
                    throw new ScmInvalidArgumentException(
                            "parse site info failed:metaType=" + siteObj.getMetaType());
                }
            }
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException(
                    "parse siteMap info failed:record=" + record.toString(), e);
        }
    }

    private ScmSiteUrl createScmSiteUrl(ScmSiteObj siteObj)
            throws ScmServerException, ScmDatasourceException {
        switch (siteObj.getDataType()) {
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                return createSdbSiteUrl(siteObj, false);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
                return new HadoopSiteUrl(siteObj.getDataType(), siteObj.getDataUrlList(),
                        siteObj.getDataUser(), siteObj.getDataPasswd(), siteObj.getDataConf());
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                return new HadoopSiteUrl(siteObj.getDataType(), siteObj.getDataUrlList(),
                        siteObj.getDataUser(), siteObj.getDataPasswd(), siteObj.getDataConf());
            default:
                return new ScmSiteUrl(siteObj.getDataType(), siteObj.getDataUrlList(),
                        siteObj.getDataUser(), siteObj.getDataPasswd());
        }
    }

    private ScmSiteUrl createSdbSiteUrl(ScmSiteObj siteObj, boolean isMeta)
            throws ScmServerException {
        ConfigOptions connConf = new ConfigOptions();
        connConf.setConnectTimeout(PropertiesUtils.getConnectTimeout());
        connConf.setMaxAutoConnectRetryTime(PropertiesUtils.getAutoConnectRetryTime());
        connConf.setSocketTimeout(PropertiesUtils.getSocketTimeout());
        connConf.setUseNagle(PropertiesUtils.getUseNagleFlag());
        connConf.setUseSSL(PropertiesUtils.getUseSSLFlag());
        connConf.setSocketKeepAlive(true);

        DatasourceOptions datasourceConf = new DatasourceOptions();
        datasourceConf.setMaxCount(PropertiesUtils.getMaxConnectionNum());
        datasourceConf.setDeltaIncCount(PropertiesUtils.getDeltaIncCount());
        datasourceConf.setMaxIdleCount(PropertiesUtils.getMaxIdleNum());
        datasourceConf.setKeepAliveTimeout(PropertiesUtils.getSdbKeepAliveTime());
        datasourceConf.setCheckInterval(PropertiesUtils.getRecheckCyclePeriod());
        datasourceConf.setValidateConnection(PropertiesUtils.getValidateConnection());
        List<String> preferedInstance = new ArrayList<>();
        preferedInstance.add("M");
        datasourceConf.setPreferedInstance(preferedInstance);
        try {
            if (isMeta) {
                return new SdbSiteUrl(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR,
                        siteObj.getMetaUrlList(), siteObj.getMetaUser(), siteObj.getMetaPasswd(),
                        connConf, datasourceConf);
            }
            else {
                return new SdbSiteUrl(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR,
                        siteObj.getDataUrlList(), siteObj.getDataUser(), siteObj.getDataPasswd(),
                        connConf, datasourceConf);
            }
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to create SdbSiteUrl", e);
        }
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean isRootSite() {
        return isRootSite;
    }

    public ScmSiteUrl getMetaUrl() {
        return metaUrl;
    }

    public ScmSiteUrl getDataUrl() {
        return dataUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ScmSite)) {
            return false;
        }

        ScmSite right = (ScmSite) o;
        if (this.id == right.id && this.name.equals(right.name)
                && this.isRootSite == right.isRootSite && this.dataUrl.equals(right.dataUrl)) {
            if (null != metaUrl) {
                return this.metaUrl.equals(right.metaUrl);
            }
            else if (null == right.metaUrl) {
                return true;
            }

            return false;
        }

        return false;
    }

    private void generateUrlInfo(ScmSiteUrl siteUrl, BSONObject result) throws ScmServerException {
        List<String> urlList = siteUrl.getUrls();
        BasicBSONList bList = new BasicBSONList();
        bList.addAll(urlList);

        result.put(FieldName.FIELD_CLSITE_URL, bList);
        result.put(FieldName.FIELD_CLSITE_USER, siteUrl.getUser());
        result.put(FieldName.FIELD_CLSITE_PASSWD, siteUrl.getPassword());
    }

    public BSONObject generateRecord() throws ScmServerException {
        BSONObject result = new BasicBSONObject();
        result.put(FieldName.FIELD_CLSITE_ID, id);
        result.put(FieldName.FIELD_CLSITE_NAME, name);
        result.put(FieldName.FIELD_CLSITE_MAINFLAG, isRootSite);
        if (null != metaUrl) {
            BSONObject metaObj = new BasicBSONObject();
            generateUrlInfo(metaUrl, metaObj);
            result.put(FieldName.FIELD_CLSITE_META, metaObj);
        }

        BSONObject dataObj = new BasicBSONObject();
        dataObj.put(FieldName.FIELD_CLSITE_DATA_TYPE, dataUrl.getType());
        generateUrlInfo(dataUrl, dataObj);
        result.put(FieldName.FIELD_CLSITE_DATA, dataObj);

        return result;
    }
}
