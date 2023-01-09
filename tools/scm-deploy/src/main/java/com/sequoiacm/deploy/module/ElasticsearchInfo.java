package com.sequoiacm.deploy.module;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;
import org.bson.BSONObject;

import java.io.File;

public class ElasticsearchInfo {
    private String url;
    private String user;
    private String password;
    private String certPath;
    public static final ConfCoverter<ElasticsearchInfo> CONVERTER = new ConfCoverter<ElasticsearchInfo>() {
        @Override
        public ElasticsearchInfo convert(BSONObject bson) {
            return new ElasticsearchInfo(bson);
        }
    };

    public ElasticsearchInfo(BSONObject bson) {
        url = BsonUtils.getStringChecked(bson, ConfFileDefine.ES_URL);
        user = BsonUtils.getString(bson, ConfFileDefine.ES_USER);
        password = BsonUtils.getString(bson, ConfFileDefine.ES_PASSWORD);
        certPath = BsonUtils.getString(bson, ConfFileDefine.ES_CERT_PATH);
        if (certPath != null && certPath.trim().length() > 0) {
            if (!new File(certPath).exists()) {
                throw new IllegalArgumentException("cert file not found:" + certPath);
            }
        }
        else {
            certPath = null;
        }

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    @Override
    public String toString() {
        return "ElasticsearchInfo{" + "url='" + url + '\'' + ", user='" + user + '\''
                + ", certPath='" + certPath + '\'' + '}';
    }
}
