package com.sequoiacm.cephs3.dataservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrlWithConf;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;

import java.util.Objects;

public class CephS3DataService extends ScmService {
    private final String CONF_KEY_DECIDER_MODE = "connectionDecider.mode";

    private CephS3UrlInfo primaryUrlInfo;
    private CephS3UrlInfo standbyUrlInfo;
    private ConnectionDecider connectionDecider;
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataService.class);

    public CephS3DataService(int siteId, ScmSiteUrl siteUrl) throws CephS3Exception {
        super(siteId, siteUrl);
        ScmSiteUrlWithConf siteUrlWithConf = (ScmSiteUrlWithConf) siteUrl;
        initConnectionDecider(siteId, siteUrlWithConf);
    }

    private void initConnectionDecider(int siteId, ScmSiteUrlWithConf siteUrlWithConf)
            throws CephS3Exception {
        DeciderMode mode = DeciderMode.valueOf(siteUrlWithConf.getDataConf()
                .getOrDefault(CONF_KEY_DECIDER_MODE, DeciderMode.auto.name()));
        primaryUrlInfo = getUrlInfoByIndex(0, siteUrlWithConf);
        if (primaryUrlInfo == null) {
            throw new CephS3Exception("site url is empty:siteId=" + siteId + ", siteUrl="
                    + siteUrlWithConf.getUrls());
        }
        standbyUrlInfo = getUrlInfoByIndex(1, siteUrlWithConf);

        if (mode == DeciderMode.primary_only || standbyUrlInfo == null) {
            logger.info("init connection decider: " + DeciderMode.primary_only);
            connectionDecider = new ConnectionDeciderImpl(siteId, siteUrlWithConf.getDataConf(),
                    primaryUrlInfo);
            return;
        }

        if (mode == DeciderMode.standby_only) {
            logger.info("init connection decider: " + DeciderMode.standby_only);
            connectionDecider = new ConnectionDeciderImpl(siteId, siteUrlWithConf.getDataConf(),
                    standbyUrlInfo);
            return;
        }

        if (mode == DeciderMode.auto) {
            logger.info("init connection decider: " + DeciderMode.auto);
            connectionDecider = new ConnectionDeciderImpl(siteId, siteUrlWithConf.getDataConf(),
                    primaryUrlInfo, standbyUrlInfo);
            return;
        }
        throw new CephS3Exception("unknown decider mode: siteId=" + siteId + ", mode=" + mode);
    }

    private CephS3UrlInfo getUrlInfoByIndex(int urlIndex, ScmSiteUrlWithConf siteUrlWithConf)
            throws CephS3Exception {
        if (siteUrlWithConf.getUrls().size() <= urlIndex) {
            return null;
        }
        CephS3UrlInfo urlInfo = new CephS3UrlInfo(siteUrlWithConf.getUrls().get(urlIndex));
        if (!urlInfo.hasAccesskeyAndSecretkey()) {
            urlInfo.setAccesskey(siteUrlWithConf.getUser());
            AuthInfo auth = ScmFilePasswordParser.parserFile(siteUrlWithConf.getPassword());
            urlInfo.setSecretkey(auth.getPassword());
        }
        return urlInfo;
    }

    public CephS3ConnWrapper getConn() throws CephS3Exception {
        return connectionDecider.getConn();
    }

    public void releaseConn(CephS3ConnWrapper conn) {
        connectionDecider.release(conn);
    }

    public CephS3ConnWrapper releaseAndTryGetAnotherConn(CephS3ConnWrapper conn) {
        return connectionDecider.releaseAndTryGetAnotherConn(conn);
    }

    @Override
    public void clear() {
        connectionDecider.shutdown();
    }

    @Override
    public String getType() {
        return "ceph_s3";
    }

}

enum DeciderMode {
    auto,
    primary_only,
    standby_only;
}

class CephS3UrlInfo {
    private final String url;
    private String accesskey;
    private String secretkey;

    public CephS3UrlInfo(String url) throws CephS3Exception {
        String[] elements = url.split("@");
        if (elements.length == 1) {
            this.url = url;
            return;
        }

        if (elements.length > 2) {
            throw new CephS3Exception("cephs3 data url syntax is invalid: " + url
                    + ", expected: accesskey:secretkeyFilePath@http://cephs3");
        }

        this.url = elements[1];

        String[] accesskeyAndSecretkeyFilePath = elements[0].split(":");
        if (accesskeyAndSecretkeyFilePath.length != 2) {
            throw new CephS3Exception("cephs3 data url syntax is invalid: " + url
                    + ", expected: accesskey:secretkeyFilePath@http://cephs3");
        }
        accesskey = accesskeyAndSecretkeyFilePath[0];
        String secretkeyFilePath = accesskeyAndSecretkeyFilePath[1];
        AuthInfo auth = ScmFilePasswordParser.parserFile(secretkeyFilePath);
        secretkey = auth.getPassword();
    }

    public String getUrl() {
        return url;
    }

    public boolean hasAccesskeyAndSecretkey() {
        return accesskey != null && secretkey != null;
    }

    public String getAccesskey() {
        return accesskey;
    }

    public String getSecretkey() {
        return secretkey;
    }

    public void setAccesskey(String accesskey) {
        this.accesskey = accesskey;
    }

    public void setSecretkey(String secretkey) {
        this.secretkey = secretkey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CephS3UrlInfo that = (CephS3UrlInfo) o;
        return Objects.equals(url, that.url) && Objects.equals(accesskey, that.accesskey)
                && Objects.equals(secretkey, that.secretkey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, accesskey, secretkey);
    }

    @Override
    public String toString() {
        return "CephS3UrlInfo{" + "url='" + url + '\'' + ", accesskey='" + accesskey + '\'' + '}';
    }
}
