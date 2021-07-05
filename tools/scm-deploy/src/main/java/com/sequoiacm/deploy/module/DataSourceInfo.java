package com.sequoiacm.deploy.module;

import java.util.Objects;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class DataSourceInfo {
    private String url;
    private String user;
    private String passwordFile;
    private String password;
    private DatasourceType type;
    private BSONObject connectionConf;
    private String name;
    private DataSourceInfo standbyDatasource;

    public static final ConfCoverter<DataSourceInfo> CONVERTER = new ConfCoverter<DataSourceInfo>() {
        @Override
        public DataSourceInfo convert(BSONObject bson) {
            return new DataSourceInfo(bson);
        }
    };

    public DataSourceInfo(BSONObject bson) {
        url = BsonUtils.getStringChecked(bson, ConfFileDefine.DATASOURCE_URL);
        user = BsonUtils.getStringChecked(bson, ConfFileDefine.DATASOURCE_USER);
        type = DatasourceType.getDatasourceType(
                BsonUtils.getStringChecked(bson, ConfFileDefine.DATASOURCE_TYPE));
        if (type == DatasourceType.UNKNOWN) {
            throw new IllegalArgumentException("unknown datasource type:" + bson);
        }
        String passwd = BsonUtils.getStringOrElse(bson, ConfFileDefine.DATASOURCE_PASSWORD, "");
        String passwdFile = BsonUtils.getString(bson, ConfFileDefine.DATASOURCE_PASSWORD_FILE);
        PasswordInfo passwordInf = new PasswordInfo(type, user, passwd, passwdFile);
        password = passwordInf.getPlaintext();
        passwordFile = passwordInf.getFilePath();
        String connectionConfStr = BsonUtils.getString(bson, ConfFileDefine.DATASOURCE_CONF);
        connectionConf = (BSONObject) JSON.parse(connectionConfStr);
        if (connectionConf == null) {
            connectionConf = new BasicBSONObject();
        }
        name = BsonUtils.getStringChecked(bson, ConfFileDefine.DATASOURCE_NAME);
    }

    public void setStandbyDatasource(DataSourceInfo standbyDatasource) {
        this.standbyDatasource = standbyDatasource;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPasswordFile() {
        return passwordFile;
    }

    public String getPassword() {
        return password;
    }

    public DatasourceType getType() {
        return type;
    }

    public BSONObject getConConf() {
        return connectionConf;
    }

    public void resetName(String name) {
        this.name = name;
    }

    public DataSourceInfo getStandbyDatasource() {
        return standbyDatasource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DataSourceInfo that = (DataSourceInfo) o;
        return Objects.equals(url, that.url) && Objects.equals(user, that.user)
                && Objects.equals(passwordFile, that.passwordFile)
                && Objects.equals(password, that.password) && type == that.type
                && Objects.equals(connectionConf, that.connectionConf)
                && Objects.equals(name, that.name)
                && Objects.equals(standbyDatasource, that.standbyDatasource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, user, passwordFile, password, type, connectionConf, name,
                standbyDatasource);
    }

    @Override
    public String toString() {
        return "DataSourceInfo{" + "url='" + url + '\'' + ", user='" + user + '\''
                + ", passwordFile='" + passwordFile + '\'' + ", password='" + password + '\''
                + ", type=" + type + ", connectionConf=" + connectionConf + ", name='" + name + '\''
                + ", standbyDatasource=" + standbyDatasource + ", conConf=" + getConConf() + '}';
    }
}
