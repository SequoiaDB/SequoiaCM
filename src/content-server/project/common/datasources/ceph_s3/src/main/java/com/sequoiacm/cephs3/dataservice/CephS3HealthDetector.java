package com.sequoiacm.cephs3.dataservice;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.common.CephS3UrlInfo;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.springframework.util.StringUtils;

interface RestoreCallback {
    void onRestore() throws Exception;
}

public class CephS3HealthDetector {
    private static final Logger logger = LoggerFactory.getLogger(CephS3HealthDetector.class);
    private final ScmTimer timer;
    private final CephS3ConnectionConf detectConnConf;
    private static final String CONF_KEY_DETECT_CONN_TIMEOUT = "detectClient."
            + CephS3ConnectionConf.CONF_KEY_CONN_TIMEOUT;
    private Map<CephS3UrlInfo, RestoreCallback> url2Callback = new ConcurrentHashMap<>();

    public CephS3HealthDetector(Map<String, String> cephS3Conf) throws CephS3Exception {
        long interval = Long.parseLong(cephS3Conf.getOrDefault("detectInterval", "5000"));
        if (interval <= 0) {
            throw new CephS3Exception(
                    "ceph s3 detectInterval must be greater than 0: " + interval);
        }
        this.detectConnConf = new CephS3ConnectionConf(cephS3Conf);
        String connTimeout = cephS3Conf.get(CONF_KEY_DETECT_CONN_TIMEOUT);
        if (!StringUtils.isEmpty(connTimeout)) {
            int connTimeoutInMills = Integer.parseInt(connTimeout);
            if (connTimeoutInMills < 0) {
                throw new IllegalArgumentException("ceph s3 " + CONF_KEY_DETECT_CONN_TIMEOUT
                        + " must be greater than or equals 0: " + connTimeoutInMills);
            }
            this.detectConnConf.setConnectionTimeout(connTimeoutInMills);
        }
        logger.info("cephs3 detect client conf:{}", detectConnConf);
        timer = ScmTimerFactory.createScmTimer();
        timer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    detect();
                }
                catch (Throwable e) {
                    logger.error("unexpected exception when detecting cephs3", e);
                }
            }
        }, interval, interval);
    }

    public void addUrlToDetect(CephS3UrlInfo url, RestoreCallback c) {
        url2Callback.put(url, c);
    }

    private void detect() {
        Iterator<Map.Entry<CephS3UrlInfo, RestoreCallback>> it = url2Callback.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<CephS3UrlInfo, RestoreCallback> entry = it.next();
            if (isRestore(entry.getKey())) {
                try {
                    entry.getValue().onRestore();
                    it.remove();
                }
                catch (Exception e) {
                    logger.error("failed to restore: url={}", entry.getKey().getUrl(), e);
                }
            }
        }
    }

    private boolean isRestore(CephS3UrlInfo key) {
        try {
            new CephS3Conn(-1, key.getUserInfo().getAccessKey(), key.getUserInfo().getSecretKey(),
                    key.getUrl(), detectConnConf).shutdownSilence();
            logger.info("detect cephs3 success: url={}", key.getUrl());
            return true;
        }
        catch (Throwable e) {
            logger.debug("detect cephs3 failed: url={}", key.getUrl(), e);
            return false;
        }
    }

    public void shutdown() {
        timer.cancel();
    }
}
