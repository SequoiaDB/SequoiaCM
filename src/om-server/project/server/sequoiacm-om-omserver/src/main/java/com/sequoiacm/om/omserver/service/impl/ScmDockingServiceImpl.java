package com.sequoiacm.om.omserver.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmDockedEvent;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.ScmDockingService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import com.sequoiacm.om.omserver.session.ScmOmSessionFactory;
import com.sequoiacm.om.omserver.session.ScmOmSessionMgr;

@Component
public class ScmDockingServiceImpl implements ScmDockingService {
    private static final Logger logger = LoggerFactory.getLogger(ScmDockingServiceImpl.class);

    private ScmOmSessionFactory sessionFactory;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ScmOmSessionMgr sessionMgr;

    @Autowired
    private ScmOmServerConfig omServerConfig;

    @Autowired
    private ApplicationArguments appArgs;

    private boolean isDockedToScm = false;

    @Autowired
    public ScmDockingServiceImpl(ScmOmSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        isDockedToScm = sessionFactory.isDocked();
    }

    @Override
    public synchronized ScmOmSession dock(List<String> gatewayList, String region, String zone,
            String username, String password) throws ScmInternalException, ScmOmServerException {
        if (isDockedToScm) {
            throw new ScmOmServerException(ScmOmServerError.UNSUPPORT_OPERATION,
                    "this om already bind to a scm cluster");
        }
        sessionFactory.reinit(gatewayList, ScmOmServerConfig.DEFAULT_READ_TIMEOUT, region, zone);
        ScmOmSession session = sessionFactory.createSession(username, password);
        try {
            sessionMgr.saveSession(session);
            saveConfig(gatewayList, ScmOmServerConfig.DEFAULT_READ_TIMEOUT, region, zone);
            isDockedToScm = true;
            appContext.publishEvent(new ScmDockedEvent());
        }
        catch (Exception e) {
            sessionMgr.deleteSession(session);
            throw e;
        }
        return session;
    }

    private void saveConfig(List<String> gatewayList, int defaultReadTiemout, String region,
            String zone) throws ScmOmServerException {
        omServerConfig.setRegion(region);
        omServerConfig.setSessionKeepAliveTime(defaultReadTiemout);
        omServerConfig.setZone(zone);
        omServerConfig.setGateway(gatewayList);

        List<String> confLocations = appArgs.getOptionValues("spring.config.location");
        if (confLocations == null || confLocations.size() != 1) {
            // Developer env
            logger.warn(
                    "--spring.config.location is not specified correctly, ignore to save config to conf file:{}",
                    confLocations);
            return;
        }

        Map<String, String> newProps = new HashMap<>();
        newProps.put("scm.omserver.region", region);
        newProps.put("scm.omserver.zone", zone);
        newProps.put("scm.omserver.gateway",
                StringUtils.collectionToCommaDelimitedString(gatewayList));
        ScmConfigPropsModifier confModifier = new ScmConfigPropsModifier(confLocations.get(0));
        confModifier.modifyPropsFile(newProps);

    }

    @Override
    public boolean isDockedToScm() {
        return isDockedToScm;
    }

}
