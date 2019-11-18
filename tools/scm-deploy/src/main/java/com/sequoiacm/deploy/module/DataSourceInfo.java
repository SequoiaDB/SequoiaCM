package com.sequoiacm.deploy.module;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataSourceInfo other = (DataSourceInfo) obj;
        if (type != other.type)
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        }
        else if (!url.equals(other.url))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        }
        else if (!user.equals(other.user))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DataSourceInfo [url=" + url + ", user=" + user + ", passwordFile=" + passwordFile
                + ", password=" + password + ", type=" + type + ", connectionConf=" + connectionConf
                + ", name=" + name + "]";
    }
}
