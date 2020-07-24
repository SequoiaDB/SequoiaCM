package com.sequoiacm.tools.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine.DataSourceType;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.common.mapping.ScmSiteObj;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.tools.common.SdbHelper;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmSiteInfo {
    private int id;
    private String name;
    private boolean isRootSite;

    private List<String> dataUrl;
    private String dataUser;
    private String dataPasswd;
    private String dataType;
    private Map<String, String> dataConf;

    private List<String> metaUrl;
    private String metaUser;
    private String metaPasswd;

    private final Logger logger = LoggerFactory.getLogger(ScmSiteInfo.class.getName());

    public ScmSiteInfo(BSONObject obj) throws ScmToolsException {
        ScmSiteObj inner;
        try {
            inner = new ScmSiteObj(obj);
        }
        catch (ScmMappingException e) {
            throw new ScmToolsException("parse site info failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        this.dataPasswd = inner.getDataPasswd();
        this.dataType = inner.getDataType();
        this.dataUrl = inner.getDataUrlList();
        this.dataUser = inner.getDataUser();
        this.dataConf = inner.getDataConf();
        this.id = inner.getId();
        this.isRootSite = inner.isRootSite();
        this.metaPasswd = inner.getMetaPasswd();
        this.metaUrl = inner.getMetaUrlList();
        this.metaUser = inner.getMetaUser();
        this.name = inner.getName();
    }

    public ScmSiteInfo() {
    }

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

    public List<String> getDataUrl() {
        return dataUrl;
    }

    public void setDataUrl(List<String> dataUrl) {
        this.dataUrl = dataUrl;
    }

    public String getDataUser() {
        return dataUser;
    }

    public void setDataUser(String dataUser) {
        this.dataUser = dataUser;
    }

    public String getDataPasswd() {
        return dataPasswd;
    }

    public String getDecryptDataPasswd() throws ScmToolsException {
        return ScmFilePasswordParser.parserFile(dataPasswd).getPassword();
    }

    public void setDataPasswd(String dataPasswd) {
        this.dataPasswd = dataPasswd;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Map<String, String> getDataConf() {
        return dataConf;
    }

    public void setDataConf(Map<String, String> dataConf) {
        this.dataConf = dataConf;
    }

    public List<String> getMetaUrl() {
        return metaUrl;
    }

    public void setMetaUrl(List<String> metaUrl) {
        this.metaUrl = metaUrl;
    }

    public String getMetaUrlStr() {
        return listToStr(metaUrl);
    }

    public String getDataUrlStr() {
        return listToStr(dataUrl);
    }

    private String listToStr(List<String> list) {
        if (list == null) {
            return null;
        }
        String ret = new String();
        for (String url : list) {
            ret = ret + url + ",";
        }
        if (ret.length() > 0) {
            ret = ret.substring(0, ret.length() - 1);
        }
        return ret;
    }

    public String getMetaUser() {
        return metaUser;
    }

    public void setMetaUser(String metaUser) {
        this.metaUser = metaUser;
    }

    public String getMetaPasswd() {
        return metaPasswd;
    }

    public void setMetaPasswd(String metaPasswd) {
        this.metaPasswd = metaPasswd;
    }

    public String getMetaDecryptPasswd() throws ScmToolsException {
        return ScmFilePasswordParser.parserFile(metaPasswd).getPassword();
    }

    public BSONObject toBSON() throws ScmToolsException {
        BSONObject retBSON = new BasicBSONObject();
        retBSON.put(FieldName.FIELD_CLSITE_ID, id);
        retBSON.put(FieldName.FIELD_CLSITE_NAME, name);
        retBSON.put(FieldName.FIELD_CLSITE_MAINFLAG, isRootSite);

        BSONObject dataBSON = new BasicBSONObject();
        dataBSON.put(FieldName.FIELD_CLSITE_DATA_TYPE, dataType);
        dataBSON.put(FieldName.FIELD_CLSITE_USER, dataUser);
        dataBSON.put(FieldName.FIELD_CLSITE_PASSWD, dataPasswd);
        dataBSON.put(FieldName.FIELD_CLSITE_URL, dataUrl);

        if (dataType.equals(DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR)
                || dataType.equals(DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR)) {
            dataBSON.put(FieldName.FIELD_CLSITE_CONF, dataConf);
        }
        retBSON.put(FieldName.FIELD_CLSITE_DATA, dataBSON);

        if (isRootSite) {
            BSONObject metaBSON = new BasicBSONObject();
            metaBSON.put(FieldName.FIELD_CLSITE_URL, metaUrl);
            metaBSON.put(FieldName.FIELD_CLSITE_USER, metaUser);
            metaBSON.put(FieldName.FIELD_CLSITE_PASSWD, metaPasswd);
            retBSON.put(FieldName.FIELD_CLSITE_META, metaBSON);
        }

        return retBSON;

    }

    public static void main(String[] args) {
        BSONObject retBSON = new BasicBSONObject();
        List<String> list = new ArrayList<>();
        list.add("asdas");
        list.add("asda");
        retBSON.put("list", list);
        System.out.println(retBSON.toString());
    }

    @Override
    public String toString() {
        return "ScmSiteInfo [id=" + id + ", name=" + name + ", isRootSite=" + isRootSite
                + ", dataUrl=" + dataUrl + ", dataUser=" + dataUser + ", dataPasswd=" + dataPasswd
                + ", dataType=" + dataType + ", metaUrl=" + metaUrl + ", metaUser=" + metaUser
                + ", metaPasswd=" + metaPasswd + "]";
    }



    public void validateDataLocation(BSONObject bson) throws ScmToolsException {
        switch (dataType) {
            case DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                String domainName = (String) SdbHelper.getValueWithCheck(bson,
                        FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);

                SdbHelper.checkSdbDomainExist(getDataUrlStr(), getDataUser(),
                        getDecryptDataPasswd(), domainName);
                break;
            case DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
                // noting
                break;
            case DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR:
                break;
            case DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR:
                break;
            case DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                break;
            default:
                logger.error("Unknow DataType=" + dataType + ",siteName=" + getName());
                throw new ScmToolsException(
                        "Unknow DataType=" + dataType + ",siteName=" + getName(),
                        ScmExitCode.INVALID_ARG);
        }
    }
}
