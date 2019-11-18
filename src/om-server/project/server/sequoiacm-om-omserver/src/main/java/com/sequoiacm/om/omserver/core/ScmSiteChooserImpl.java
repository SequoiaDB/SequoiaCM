package com.sequoiacm.om.omserver.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmServiceInstanceInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDataLocation;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import com.sequoiacm.om.omserver.session.ScmOmSessionFactory;

@Component
public class ScmSiteChooserImpl implements ScmSiteChooser {
    private static final Logger logger = LoggerFactory.getLogger(ScmSiteChooserImpl.class);

    private ScmOmServerConfig omserverConfig;

    // cache
    private List<OmServiceInstanceInfo> contentserverInstances;
    private PreferSites preferedSites;
    private String rootSite;
    // concurrent hash map
    private Map<String, PreferSites> preferedWorksapceSites;

    private ScmOmSessionFactory sessionFactory;

    private ScmTimer timer;

    private Random random = new Random();
    private boolean isInitialized;

    @Autowired
    ScmSiteChooserImpl(ScmOmSessionFactory sessionFactory, ScmOmServerConfig omserverConfig)
            throws ScmInternalException, ScmOmServerException {
        this.sessionFactory = sessionFactory;
        this.omserverConfig = omserverConfig;
        refreshContentServerInstanceInfo();
        int period = omserverConfig.getCacheRefreshIntreval() * 1000;
        this.timer = ScmTimerFactory.createScmTimer();
        this.timer.schedule(new ScmTimerTask() {

            @Override
            public void run() {
                try {
                    refreshContentServerInstanceInfo();
                }
                catch (Exception e) {
                    logger.error("failed to refesh contentserver instances info cache", e);
                }
            }
        }, period, period);
    }

    private void refreshContentServerInstanceInfo()
            throws ScmInternalException, ScmOmServerException {
        if (!sessionFactory.isDocked()) {
            return;
        }

        ScmOmSession session = sessionFactory.createSession();
        try {
            contentserverInstances = session.getMonitorDao().getContentServerInstance();
        }
        finally {
            session.close();
        }

        List<String> sameRegionSites = new ArrayList<>();
        List<String> sameZoneSites = new ArrayList<>();
        String rootSite = null;
        for (OmServiceInstanceInfo instance : contentserverInstances) {
            String instanceZone = instance.getZone();
            String instanceRegion = instance.getRegion();
            String siteName = instance.getServiceName().toLowerCase();

            if (instance.isRootSite()) {
                rootSite = siteName;
            }

            if (instanceRegion.equalsIgnoreCase(omserverConfig.getRegion())) {
                if (!sameZoneSites.contains(siteName)
                        && instanceZone.equalsIgnoreCase(omserverConfig.getZone())) {
                    sameZoneSites.add(siteName);
                }
                else if (!sameRegionSites.contains(siteName)) {
                    sameRegionSites.add(siteName);
                }
            }

        }
        if (rootSite == null) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "root site is not exist!");
        }
        this.rootSite = rootSite;
        this.preferedSites = new PreferSites(sameZoneSites, sameRegionSites);
        this.preferedWorksapceSites = new ConcurrentHashMap<>();

        isInitialized = true;
    }

    @Override
    public String chooseSiteFromWorkspace(OmWorkspaceDetail ws)
            throws ScmInternalException, ScmOmServerException {
        Assert.isTrue(isInitialized, "site chooser is not initialized yet");

        PreferSites wsPreferedSites = preferedWorksapceSites.get(ws.getName());
        if (wsPreferedSites == null) {
            wsPreferedSites = getPreferedSitesFromWs(ws);
            preferedWorksapceSites.put(ws.getName(), wsPreferedSites);
        }

        if (wsPreferedSites.hasPreferedSite()) {
            return wsPreferedSites.getPreferedSite();
        }

        if (omserverConfig.isOnlyConnectLocalRegionServer()) {
            refreshContentServerInstanceInfo();
            wsPreferedSites = getPreferedSitesFromWs(ws);
            preferedWorksapceSites.put(ws.getName(), wsPreferedSites);
            if (wsPreferedSites.hasPreferedSite()) {
                return wsPreferedSites.getPreferedSite();
            }
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "workspace site not in my region:ws=" + ws.getName() + ",myRegion="
                            + omserverConfig.getRegion());
        }

        List<OmWorkspaceDataLocation> sites = ws.getDataLocations();
        logger.debug("choose site,randomsite", sites.toString());
        return sites.get(new Random().nextInt(sites.size())).getSiteName();
    }

    private PreferSites getPreferedSitesFromWs(OmWorkspaceDetail ws) {
        List<String> wsSameZoneSites = new ArrayList<>();
        List<String> wsSameRegionSites = new ArrayList<>();
        for (OmWorkspaceDataLocation site : ws.getDataLocations()) {
            if (preferedSites.isSameRegionSite(site.getSiteName())) {
                wsSameRegionSites.add(site.getSiteName().toLowerCase());
            }

            if (preferedSites.isSameZoneSite(site.getSiteName())) {
                wsSameZoneSites.add(site.getSiteName().toLowerCase());
            }
        }

        return new PreferSites(wsSameZoneSites, wsSameRegionSites);
    }

    @Override
    public String chooseFromAllSite() throws ScmInternalException, ScmOmServerException {
        Assert.isTrue(isInitialized, "site chooser is not initialized yet");

        String preferSite = preferedSites.getPreferedSite();
        if (preferSite != null) {
            return preferSite;
        }

        if (omserverConfig.isOnlyConnectLocalRegionServer()) {
            refreshContentServerInstanceInfo();
            preferSite = preferedSites.getPreferedSite();
            if (preferSite != null) {
                logger.debug("chooseFromAllSite:{}", preferSite);
                return preferSite;
            }
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "all site not in my region:myRegion=" + omserverConfig.getRegion());
        }

        logger.debug("chooseFromAllSite,randomSite:", contentserverInstances.toString());
        return contentserverInstances.get(random.nextInt(contentserverInstances.size()))
                .getServiceName();
    }

    @EventListener
    public void onDockedEvent(ScmDockedEvent event)
            throws ScmInternalException, ScmOmServerException {
        Assert.isTrue(sessionFactory.isDocked(),
                "get dock event, but session factory is not initialized");
        refreshContentServerInstanceInfo();
    }

    @Override
    public String getRootSite() throws ScmInternalException, ScmOmServerException {
        Assert.isTrue(isInitialized, "site chooser is not initialized yet!");
        if (rootSite == null) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "root site is not exist!!");
        }
        return rootSite;
    }

    @Override
    public void refreshCacheSilence() {
        try {
            refreshContentServerInstanceInfo();
        }
        catch (Exception e) {
            logger.warn("failed to fresh site chooser cache", e);
        }
    }

    class PreferSites {
        private List<String> sameZoneSites = new ArrayList<>();
        private List<String> sameRegionSites = new ArrayList<>();

        public PreferSites(List<String> sameZoneSites, List<String> sameRegionSites) {
            this.sameZoneSites = sameZoneSites;
            this.sameRegionSites = sameRegionSites;
        }

        public boolean isSameZoneSite(String site) {
            return sameZoneSites.contains(site.toLowerCase());
        }

        public boolean isSameRegionSite(String site) {
            return sameRegionSites.contains(site.toLowerCase());
        }

        public boolean hasPreferedSite() {
            return sameZoneSites.size() > 0 || sameRegionSites.size() > 0;
        }

        public String getPreferedSite() {
            if (sameZoneSites.size() > 0) {
                return sameZoneSites.get(random.nextInt(sameZoneSites.size()));
            }
            if (sameRegionSites.size() > 0) {
                return sameRegionSites.get(random.nextInt(sameRegionSites.size()));
            }
            return null;
        }
    }

    @Override
    public void onException(ScmInternalException e) {
        // site not exist
        // site not in workspace
        if (e.getErrorCode() == ScmError.HTTP_NOT_FOUND.getErrorCode()
                || e.getErrorCode() == ScmError.SERVER_NOT_IN_WORKSPACE.getErrorCode()) {
            refreshCacheSilence();
        }
    }
}
