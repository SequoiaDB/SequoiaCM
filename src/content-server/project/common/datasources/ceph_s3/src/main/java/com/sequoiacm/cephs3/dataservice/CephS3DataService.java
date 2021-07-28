package com.sequoiacm.cephs3.dataservice;

import com.sequoiacm.common.CephS3UrlInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrlWithConf;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;

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
