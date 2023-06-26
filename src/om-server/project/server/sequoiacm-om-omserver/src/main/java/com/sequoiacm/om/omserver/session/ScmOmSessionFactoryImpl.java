package com.sequoiacm.om.omserver.session;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRequestConfig;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUrlConfig;
import com.sequoiacm.client.core.ScmUrlConfig.Builder;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.exception.ScmInternalException;

import javax.annotation.PreDestroy;

@Component
public class ScmOmSessionFactoryImpl implements ScmOmSessionFactory {

    private static final Logger logger = LoggerFactory.getLogger(ScmOmSessionFactoryImpl.class);
    private ScmTimer asyncInitSessionTimer;
    private ScmDriverConnectionFactory connectionFactory;
    private volatile boolean isDocked = false;

    @Autowired
    public ScmOmSessionFactoryImpl(ScmOmServerConfig omServerConfig) {
        if (omServerConfig.getGateway() != null && omServerConfig.getGateway().size() > 0) {
            try {
                reinit(omServerConfig.getGateway(), omServerConfig.getReadTimeout(),
                        omServerConfig.getRegion(), omServerConfig.getZone());
            }
            catch (ScmInternalException e) {
                logger.warn("failed to initialize ScmOmSessionFactory, retry later: ", e);
                asyncReInit(this, omServerConfig, 5000);
            }
        }
    }

    private void asyncReInit(final ScmOmSessionFactoryImpl omSessionFactory,
            final ScmOmServerConfig omServerConfig, int interval) {
        if (asyncInitSessionTimer == null) {
            asyncInitSessionTimer = ScmTimerFactory.createScmTimer();
        }
        asyncInitSessionTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    omSessionFactory.reinit(omServerConfig.getGateway(),
                            omServerConfig.getReadTimeout(), omServerConfig.getRegion(),
                            omServerConfig.getZone());
                    cancel();
                }
                catch (Exception e) {
                    logger.warn("failed to initialize om session factory: ", e);
                }
            }
        }, interval, interval);
    }

    @Override
    public ScmOmSession createSession(String username, String password)
            throws ScmInternalException {
        Assert.isTrue(isDocked, "Session factory is not docked yet");
        ScmSession connection = null;
        if (username == null || password == null) {
            connection = connectionFactory.createConnection();
        }
        else {
            connection = connectionFactory.createConnection(username, password);
        }
        return new ScmOmSessionImpl(connection);
    }

    @Override
    public ScmOmSession createSession() throws ScmInternalException {
        return createSession(null, null);
    }

    @Override
    public void reinit(List<String> gatewayAddr, int readTimeout, String region, String zone)
            throws ScmInternalException {
        this.connectionFactory = new ScmDriverConnectionFactory(gatewayAddr, readTimeout, region,
                zone);
        isDocked = true;
    }

    @Override
    public boolean isDocked() {
        return isDocked;
    }

    class ScmDriverConnectionFactory {
        private List<String> addrs;
        private int readTimeout;
        private String region;
        private String zone;

        private ScmUrlConfig getwayUrlConfig;

        public ScmDriverConnectionFactory(List<String> gatewayAddr, int readTimeout, String region,
                String zone) throws ScmInternalException {
            this.addrs = gatewayAddr;
            this.readTimeout = readTimeout;
            this.region = region;
            this.zone = zone;
            this.getwayUrlConfig = createUrlConfig(addrs);
        }

        private ScmSession createConnection(SessionType connectionType, String username,
                String password) throws ScmInternalException {
            try {
                ScmSession connection = ScmFactory.Session.createSession(connectionType,
                        new ScmConfigOption(getwayUrlConfig, region, zone, username, password,
                                ScmRequestConfig.custom().setSocketTimeout(readTimeout).build()));
                return connection;
            }
            catch (ScmException e) {
                throw new ScmInternalException(e.getError(),
                        "failed to connect to scm, " + e.getMessage(), e);
            }
        }

        public ScmSession createConnection() throws ScmInternalException {
            return createConnection(SessionType.NOT_AUTH_SESSION, null, null);
        }

        public ScmSession createConnection(String username, String password)
                throws ScmInternalException {
            return createConnection(SessionType.AUTH_SESSION, username, password);
        }

        public ScmSession reinit(List<String> gatewayList, String region, String zone,
                String username, String password) throws ScmInternalException {
            try {
                ScmUrlConfig urlConfig = createUrlConfig(gatewayList);
                ScmSession session = ScmFactory.Session.createSession(
                        new ScmConfigOption(urlConfig, region, zone, username, password,
                                ScmRequestConfig.custom().setSocketTimeout(readTimeout).build()));
                this.getwayUrlConfig = urlConfig;
                this.region = region;
                this.zone = zone;
                return session;
            }
            catch (ScmException e) {
                throw new ScmInternalException(e.getError(),
                        "failed to reinit connection factory, " + e.getMessage(), e);
            }
        }

        public ScmUrlConfig createUrlConfig(List<String> gatewayAddrs) throws ScmInternalException {
            ScmSession s = null;
            try {
                ScmConfigOption tmpScmConfigOption = new ScmConfigOption(gatewayAddrs);
                s = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION,
                        tmpScmConfigOption);
                List<ScmServiceInstance> gatewayList = ScmSystem.ServiceCenter
                        .getServiceInstanceList(s, "gateway");
                Builder urlBuilder = ScmUrlConfig.custom();
                for (ScmServiceInstance gateway : gatewayList) {
                    ArrayList<String> url = new ArrayList<>();
                    url.add(gateway.getIp() + ":" + gateway.getPort());
                    urlBuilder.addUrl(gateway.getRegion(), gateway.getZone(), url);
                }
                return urlBuilder.build();
            }
            catch (ScmException e) {
                throw new ScmInternalException(e.getError(), "initialized ulr config failed", e);
            }
            finally {
                if (s != null) {
                    s.close();
                }
            }
        }
    }

    @PreDestroy
    private void destroy() {
        if (asyncInitSessionTimer != null) {
            asyncInitSessionTimer.cancel();
        }
    }
}
