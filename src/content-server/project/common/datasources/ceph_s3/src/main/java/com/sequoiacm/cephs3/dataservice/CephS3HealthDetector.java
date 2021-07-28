package com.sequoiacm.cephs3.dataservice;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.common.CephS3UrlInfo;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

interface RestoreCallback {
    void onRestore() throws Exception;
}

public class CephS3HealthDetector {
    private static final Logger logger = LoggerFactory.getLogger(CephS3HealthDetector.class);
    private final ScmTimer timer;
    private final CephS3ConnectionConf detectConnConf;
    private Map<CephS3UrlInfo, RestoreCallback> url2Callback = new ConcurrentHashMap<>();

    public CephS3HealthDetector(Map<String, String> detectConnectionConf) throws CephS3Exception {
        long interval = Long.parseLong(detectConnectionConf.getOrDefault("detectInterval", "5000"));
        if (interval <= 0) {
            throw new CephS3Exception(
                    "ceph s3 detectInterval must be greater than 0: " + interval);
        }
        this.detectConnConf = new CephS3ConnectionConf(detectConnectionConf, "detectClient.");
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
            new CephS3Conn(-1, key.getAccesskey(), key.getSecretkey(), key.getUrl(), detectConnConf)
                    .shutdown();
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
