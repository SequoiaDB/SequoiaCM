package com.sequoiacm.cephs3.dataservice;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.common.CephS3UrlInfo;
import com.sequoiacm.common.CephS3UserInfo;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrlWithConf;

public class CephS3DataService extends ScmService {
    private final String CONF_KEY_DECIDER_MODE = "connectionDecider.mode";
    private final String CONF_KEY_QUOTA_EXCEEDED_AUTO_CREATE_BUCKET_OBJECT_THRESHOLD = "quotaExceeded.autoCreateBucket.objectCountThreshold";

    private CephS3ConnectionManager cephS3ConnectionManager;
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataService.class);

    // 桶发生 quotaExceeded 需要自动建新桶时，旧桶的对象数需要达到该数量以上方能建立新桶
    private int quotaExceededObjectThreshold;

    public CephS3DataService(int siteId, ScmSiteUrl siteUrl) throws CephS3Exception {
        super(siteId, siteUrl);
        ScmSiteUrlWithConf siteUrlWithConf = (ScmSiteUrlWithConf) siteUrl;
        this.quotaExceededObjectThreshold = Integer.parseInt(siteUrlWithConf.getDataConf()
                .getOrDefault(CONF_KEY_QUOTA_EXCEEDED_AUTO_CREATE_BUCKET_OBJECT_THRESHOLD, "1000"));
        if (quotaExceededObjectThreshold > 10000 || quotaExceededObjectThreshold <= 0) {
            throw new CephS3Exception(CONF_KEY_QUOTA_EXCEEDED_AUTO_CREATE_BUCKET_OBJECT_THRESHOLD
                    + " must be greater then 0 and less than 10000: "
                    + quotaExceededObjectThreshold);
        }
        logger.info("quotaExceeded.autoCreateBucket.objectCountThreshold={}",
                quotaExceededObjectThreshold);
        initConnectionMgr(siteId, siteUrlWithConf);
    }

    private void initConnectionMgr(int siteId, ScmSiteUrlWithConf siteUrlWithConf)
            throws CephS3Exception {
        Map<String, String> cephS3Conf = siteUrlWithConf.getDataConf();
        DeciderMode mode = DeciderMode
                .valueOf(cephS3Conf.getOrDefault(CONF_KEY_DECIDER_MODE, DeciderMode.auto.name()));

        CephS3UrlInfo primaryUrlInfo = getCephS3UrlInfoByIndex(0, siteUrlWithConf);
        if (primaryUrlInfo == null) {
            throw new CephS3Exception("site url is empty:siteId=" + siteId + ", siteUrl="
                    + siteUrlWithConf.getUrls());
        }
        CephS3UrlInfo standbyUrlInfo = getCephS3UrlInfoByIndex(1, siteUrlWithConf);

        if (mode == DeciderMode.primary_only) {
            logger.info("init connection mgr: " + DeciderMode.primary_only);
            cephS3ConnectionManager = new CephS3ConnMgrStandaloneIml(primaryUrlInfo,
                    CephS3ConnType.primary, siteId, cephS3Conf);
        }
        else if (mode == DeciderMode.standby_only) {
            logger.info("init connection mgr: " + DeciderMode.standby_only);
            if (standbyUrlInfo == null) {
                throw new CephS3Exception(
                        "fail to init connection:siteId" + siteId + ", standby url info is null");
            }
            cephS3ConnectionManager = new CephS3ConnMgrStandaloneIml(standbyUrlInfo,
                    CephS3ConnType.standby, siteId, cephS3Conf);
        }
        else if (mode == DeciderMode.auto) {
            // 单个库
            if (siteUrlWithConf.getUrls().size() == 1) {
                logger.info("init connection mgr: " + DeciderMode.primary_only);
                cephS3ConnectionManager = new CephS3ConnMgrStandaloneIml(primaryUrlInfo,
                        CephS3ConnType.primary, siteId, cephS3Conf);
            }
            else {
                // 主备库
                logger.info("init connection mgr: " + DeciderMode.auto);
                cephS3ConnectionManager = new CephS3ConnMgrFallbackImpl(primaryUrlInfo,
                        standbyUrlInfo, siteId, cephS3Conf);
            }
        }
        else {
            throw new CephS3Exception("unknown mode:" + mode);
        }
    }

    public CephS3ConnWrapper getConn(CephS3UserInfo primaryConfig, CephS3UserInfo standbyConfig)
            throws CephS3Exception {
        return cephS3ConnectionManager.getConn(primaryConfig, standbyConfig);
    }

    public void releaseConn(CephS3ConnWrapper conn) {
        cephS3ConnectionManager.release(conn);
    }

    public CephS3ConnWrapper releaseAndTryGetAnotherConn(CephS3ConnWrapper conn,
            CephS3UserInfo primaryInfo, CephS3UserInfo standbyInfo) throws CephS3Exception {
        return cephS3ConnectionManager.releaseAndTryGetAnotherConn(conn, primaryInfo, standbyInfo);
    }

    @Override
    public void clear() {
        cephS3ConnectionManager.shutdown();
    }

    @Override
    public String getType() {
        return "ceph_s3";
    }

    @Override
    public boolean supportsBreakpointUpload() {
        return true;
    }

    private CephS3UrlInfo getCephS3UrlInfoByIndex(int urlIndex,
            ScmSiteUrlWithConf siteUrlWithConf) {
        if (siteUrlWithConf.getUrls().size() <= urlIndex) {
            return null;
        }
        CephS3UrlInfo urlInfo = new CephS3UrlInfo(siteUrlWithConf.getUrls().get(urlIndex));
        if (!urlInfo.hasAccesskeyAndSecretkey()) {
            urlInfo.setUserInfo(
                    new CephS3UserInfo(siteUrlWithConf.getUser(), siteUrlWithConf.getPassword()));
        }
        return urlInfo;
    }

    public int getQuotaExceededObjectThreshold() {
        return quotaExceededObjectThreshold;
    }
}

enum DeciderMode {
    auto,
    primary_only,
    standby_only;
}
