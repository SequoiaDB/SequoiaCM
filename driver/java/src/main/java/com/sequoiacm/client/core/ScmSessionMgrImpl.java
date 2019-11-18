package com.sequoiacm.client.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmUrlConfig.Builder;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

class ScmSessionMgrImpl implements ScmSessionMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmSessionMgrImpl.class);
    private ScmConfigOption config;
    private ScmTimer timer;
    private boolean isClosed = false;

    ScmSessionMgrImpl(ScmConfigOption config, long syncGatewayAddrInterval)
            throws ScmInvalidArgumentException {
        this.config = config;
        if (syncGatewayAddrInterval < 1000) {
            logger.warn(
                    "invalid syncGatewayAddrInterval:{}, reset syncGatewayAddrInterval to 1000ms",
                    syncGatewayAddrInterval);
            syncGatewayAddrInterval = 1000;
        }
        timer = ScmTimerFactory.createScmTimer();
        timer.schedule(new SyncGatewayAddrTask(this), syncGatewayAddrInterval,
                syncGatewayAddrInterval);
    }


    @Override
    public ScmSession getSession(SessionType sessionType) throws ScmException {
        if(isClosed) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED, "sessionMgr is closed");
        }
        return ScmFactory.Session.createSession(sessionType, config);
    }

    ScmConfigOption getConfigOption() {
        return config;
    }

    @Override
    public void close() {
        isClosed = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}

class SyncGatewayAddrTask extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(SyncGatewayAddrTask.class);
    private ScmSessionMgrImpl sessionMgr;
    private ScmUrlConfig basicUrlConfig;
    private List<String> basicIpUrls;

    public SyncGatewayAddrTask(ScmSessionMgrImpl sessionMgr) throws ScmInvalidArgumentException {
        this.sessionMgr = sessionMgr;
        this.basicUrlConfig = sessionMgr.getConfigOption().getUrlConfig();
        this.basicIpUrls = getIpUrls(basicUrlConfig.getUrl());
    }

    @Override
    public void run() {
        ScmSession ss = null;
        try {
            ss = sessionMgr.getSession(SessionType.NOT_AUTH_SESSION);
            List<ScmServiceInstance> gatewayInstances = ScmSystem.ServiceCenter
                    .getServiceInstanceList(ss, "gateway");
            if (gatewayInstances.size() <= 0) {
                logger.warn("latest gateway addr is empty, ignore to reset local addr list");
                return;
            }

            ScmConfigOption configOption = sessionMgr.getConfigOption();
            String targetSite = configOption.getUrlConfig().getTargetSite();
            String urlTail = targetSite == null ? "" : "/" + targetSite;

            // copy from basicUrlConfig, do not modify basicUrlConfig
            Builder urlConfigBuilder = ScmUrlConfig.custom(basicUrlConfig);

            for (ScmServiceInstance instance : gatewayInstances) {
                if (!"UP".equals(instance.getStatus())) {
                    continue;
                }
                String instanceUrl = instance.getIp() + ":" + instance.getPort() + urlTail;
                if (!basicIpUrls.contains(instanceUrl)) {
                    ArrayList<String> instanceUrls = new ArrayList<String>();
                    instanceUrls.add(instanceUrl);
                    urlConfigBuilder.addUrl(instance.getRegion(), instance.getZone(), instanceUrls);
                }
            }

            ScmUrlConfig newUrlConfig = urlConfigBuilder.build();

            configOption.setUrlConfig(newUrlConfig);
            logger.debug("sync gateway addr:" + newUrlConfig.getUrl());
        }
        catch (Exception e) {
            logger.warn("failed to sync gateway addr list", e);
        }
        finally {
            if (ss != null) {
                ss.close();
            }
        }
    }


    private List<String> getIpUrls(List<String> urls) throws ScmInvalidArgumentException {
        ArrayList<String> ipUrls = new ArrayList<String>();
        for (String basicUrl : urls) {
            int index = basicUrl.indexOf(":");
            if (index <= -1) {
                throw new ScmInvalidArgumentException("invalid url:" + basicUrl);
            }
            String host = basicUrl.substring(0, index);
            String ip;
            try {
                ip = InetAddress.getByName(host).getHostAddress();
            }
            catch (UnknownHostException e) {
                throw new ScmInvalidArgumentException("invalid url:" + basicUrl, e);
            }
            ipUrls.add(ip + basicUrl.substring(index));
        }
        return ipUrls;
    }
}
