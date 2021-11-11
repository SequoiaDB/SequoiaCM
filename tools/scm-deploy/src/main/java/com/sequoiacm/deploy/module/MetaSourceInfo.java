package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class MetaSourceInfo {
    private String url;
    private String user;
    private String password;
    private String domain;
    private String type;

    public static final ConfCoverter<MetaSourceInfo> CONVERTER = new ConfCoverter<MetaSourceInfo>() {
        @Override
        public MetaSourceInfo convert(BSONObject bson) {
            return new MetaSourceInfo(bson);
        }
    };

    protected MetaSourceInfo() {
    }

    public MetaSourceInfo(BSONObject bson) {
        url = BsonUtils.getStringChecked(bson, ConfFileDefine.METASOURCE_URL);
        user = BsonUtils.getStringChecked(bson, ConfFileDefine.METASOURCE_USER);

        String passwd = BsonUtils.getStringOrElse(bson, ConfFileDefine.METASOURCE_PASSWORD, "");
        String passwdFile = BsonUtils.getString(bson, ConfFileDefine.METASOURCE_PASSWORD_FILE);
        PasswordInfo passwordInfo = new PasswordInfo(DatasourceType.SEQUOIADB, user, passwd,
                passwdFile);
        password = passwordInfo.getPlaintext();
        domain = BsonUtils.getStringChecked(bson, ConfFileDefine.METASOURCE_DOMAIN);
        type = ConfFileDefine.SEACTION_METASOURCE;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDomain() {
        return domain;
    }

    protected void setUrl(String url) {
        this.url = url;
    }

    protected void setUser(String user) {
        this.user = user;
    }

    protected void setPassword(String password) {
        this.password = password;
    }

    protected void setDomain(String domain) {
        this.domain = domain;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "MetaSourceInfo [url=" + url + ", user=" + user + ", domain=" + domain + "]";
    }
}
