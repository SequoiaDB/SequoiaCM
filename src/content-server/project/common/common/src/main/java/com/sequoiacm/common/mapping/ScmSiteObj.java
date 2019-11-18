package com.sequoiacm.common.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonDefine.DataSourceType;
import com.sequoiacm.common.FieldName;

public class ScmSiteObj {
    private static final Logger logger = LoggerFactory.getLogger(ScmSiteObj.class);
    private String name;
    private int id;
    private boolean isRootSite;

    // data
    private String dataType;
    private List<String> dataUrlList = new ArrayList<String>();
    private String dataUser;
    private String dataPasswd;
    private Map<String, String> dataConf = new HashMap<String, String>();

    // meta
    private List<String> metaUrlList = new ArrayList<String>();
    private String metaType;
    private String metaUser;
    private String metaPasswd;

    public ScmSiteObj(BSONObject record) throws ScmMappingException {
        try {
            BSONObject metaObj = null;
            BSONObject dataObj = null;

            name = (String) getValueCheckNotNull(record, FieldName.FIELD_CLSITE_NAME);
            id = (Integer) getValueCheckNotNull(record, FieldName.FIELD_CLSITE_ID);
            isRootSite = (Boolean) getValueCheckNotNull(record, FieldName.FIELD_CLSITE_MAINFLAG);

            Object tmp = record.get(FieldName.FIELD_CLSITE_DATA);
            if (null != tmp) {
                dataObj = (BSONObject) tmp;
                metaObj = (BSONObject) record.get(FieldName.FIELD_CLSITE_META);
            }
            else {
                dataObj = generateOldMeta(record);
                if (isRootSite) {
                    metaObj = dataObj;
                }
            }

            // data
            dataType = (String) getValueWithDefault(dataObj, FieldName.FIELD_CLSITE_DATA_TYPE,
                    CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR);
            dataUser = (String) getValueWithDefault(dataObj, FieldName.FIELD_CLSITE_USER, "");
            dataPasswd = (String) getValueWithDefault(dataObj, FieldName.FIELD_CLSITE_PASSWD, "");
            BSONObject urlBSON = (BSONObject) getValueCheckNotNull(dataObj,
                    FieldName.FIELD_CLSITE_URL);

            if (dataType.equals(DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR)
                    || dataType.equals(DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR)) {

                BSONObject confBSON = (BSONObject) getValueCheckNotNull(dataObj,
                        FieldName.FIELD_CLSITE_CONF);
                logger.info("get confBSON : " + confBSON.toString());
                parseHadoopConf(confBSON);
            }
            else {
                parseUrl(urlBSON, dataUrlList);
            }
            // meta
            if (isRootSite) {
                if (null == metaObj) {
                    throw new ScmMappingException("root site must specify meta info");
                }
                metaType = (String) getValueWithDefault(metaObj, FieldName.FIELD_CLSITE_DATA_TYPE,
                        CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR);
                metaUser = (String) getValueWithDefault(metaObj, FieldName.FIELD_CLSITE_USER, "");
                metaPasswd = (String) getValueWithDefault(metaObj, FieldName.FIELD_CLSITE_PASSWD,
                        "");
                urlBSON = (BSONObject) getValueCheckNotNull(metaObj, FieldName.FIELD_CLSITE_URL);
                parseUrl(urlBSON, metaUrlList);
            }
        }
        catch (ScmMappingException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmMappingException("parse siteMap info failed:record=" + record.toString(),
                    e);
        }
    }

    private BSONObject generateOldMeta(BSONObject record) throws ScmMappingException {
        BSONObject url = (BSONObject) getValueCheckNotNull(record, FieldName.FIELD_CLSITE_URL);
        String user = "";
        Object tmp = record.get(FieldName.FIELD_CLSITE_USER);
        if (null == tmp) {
            user = "";
        }
        else {
            user = (String) tmp;
        }

        int password_type = 0;
        tmp = record.get(FieldName.FIELD_CLSITE_PASSWD_TYPE);
        if (null == tmp) {
            password_type = CommonDefine.CryptType.SCM_CRYPT_TYPE_NONE;
        }
        else {
            password_type = (Integer) tmp;
        }

        String password = "";
        tmp = record.get(FieldName.FIELD_CLSITE_PASSWD);
        if (null == tmp) {
            password = "";
        }
        else {
            password = (String) tmp;
        }

        BSONObject obj = new BasicBSONObject();
        obj.put(FieldName.FIELD_CLSITE_DATA_TYPE,
                CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR);
        obj.put(FieldName.FIELD_CLSITE_URL, url);
        obj.put(FieldName.FIELD_CLSITE_USER, user);
        obj.put(FieldName.FIELD_CLSITE_PASSWD_TYPE, password_type);
        obj.put(FieldName.FIELD_CLSITE_PASSWD, password);

        return obj;
    }

    private Object getValueCheckNotNull(BSONObject obj, String key) throws ScmMappingException {
        Object value = obj.get(key);
        if (value == null) {
            throw new ScmMappingException("field is not exist:fieldName=" + key);
        }
        return value;
    }

    private Object getValueWithDefault(BSONObject obj, String key, Object defaultValue) {
        Object ret = obj.get(key);
        if (ret == null) {
            return defaultValue;
        }
        else {
            return ret;
        }
    }

    private void parseUrl(BSONObject urlBSON, List<String> urlList) throws ScmMappingException {
        BasicBSONList bsonList = (BasicBSONList) urlBSON;
        for (int i = 0; i < bsonList.size(); i++) {
            urlList.add((String) bsonList.get(i));
        }

        if (urlList.size() == 0) {
            throw new ScmMappingException("url count is 0");
        }
    }

    @SuppressWarnings("unchecked")
    private void parseHadoopConf(BSONObject confBSON) throws ScmMappingException {
        dataConf = confBSON.toMap();
        logger.info("parseHadoopConf : " + dataConf.toString());
        if (dataConf.size() == 0) {
            throw new ScmMappingException("hdfs|hbase configuration is null");
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

    public String getDataType() {
        return dataType;
    }

    public List<String> getDataUrlList() {
        return dataUrlList;
    }

    public String getDataUser() {
        return dataUser;
    }

    public String getDataPasswd() {
        return dataPasswd;
    }

    public Map<String, String> getDataConf() {
        return dataConf;
    }

    public List<String> getMetaUrlList() {
        return metaUrlList;
    }

    public String getMetaUser() {
        return metaUser;
    }

    public String getMetaPasswd() {
        return metaPasswd;
    }

    public String getMetaType() {
        return metaType;
    }
}
