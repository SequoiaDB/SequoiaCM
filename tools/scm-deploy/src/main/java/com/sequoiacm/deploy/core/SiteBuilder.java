package com.sequoiacm.deploy.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.deploy.common.RestTools;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.module.DataSourceInfo;
import com.sequoiacm.deploy.module.SiteInfo;
import com.sequoiacm.tools.common.RestDispatcher;
import com.sequoiacm.tools.element.ScmSiteConfig;

public class SiteBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SiteBuilder.class);

    private ScmPasswordFileSender pwdFileSender = ScmPasswordFileSender.getInstance();

    private ScmDeployInfoMgr deployInfoMgr = ScmDeployInfoMgr.getInstance();

    private CommonConfig commonConfig = CommonConfig.getInstance();

    private boolean isBuild;

    private static volatile SiteBuilder instance;

    public static SiteBuilder getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SiteBuilder.class) {
            if (instance != null) {
                return instance;
            }
            instance = new SiteBuilder();
            return instance;
        }
    }

    @SuppressWarnings("unchecked")
    private void buidSite(SiteInfo site) throws Exception {
        DataSourceInfo ds = deployInfoMgr.getDatasouceInfo(site.getDatasourceName());

        List<String> datasourceUrls = getUrlFromDs(ds);

        ScmSiteConfig siteConf = null;
        if (!site.isRoot()) {
            siteConf = ScmSiteConfig.start(site.getName()).isRootSite(false)
                    .SetDataSourceType(ds.getType()).setDataSource(datasourceUrls, ds.getUser(),
                            pwdFileSender.getDsPasswdFilePath(ds), ds.getConConf().toMap())
                    .build();
        }
        else {
            siteConf = ScmSiteConfig.start(site.getName()).isRootSite(true)
                    .SetDataSourceType(ds.getType())
                    .setDataSource(datasourceUrls, ds.getUser(),
                            pwdFileSender.getDsPasswdFilePath(ds), ds.getConConf().toMap())
                    .setMetaSource(
                            Arrays.asList(deployInfoMgr.getMetasourceInfo().getUrl().split(",")),
                            deployInfoMgr.getMetasourceInfo().getUser(),
                            pwdFileSender.getMetasourcePasswdFilePath())
                    .build();
        }

        String gatewayUrl = deployInfoMgr.getFirstGatewayUrl();

        ScmSession ss = ScmFactory.Session
                .createSession(new ScmConfigOption(gatewayUrl, "admin", "admin"));
        try {
            RestDispatcher.getInstance().createSite(ss, siteConf);
        }
        finally {
            ss.close();
        }
    }

    private List<String> getUrlFromDs(DataSourceInfo ds) {
        if (ds.getType() == ScmType.DatasourceType.CEPH_S3 && ds.getStandbyDatasource() != null) {
            List<String> ret = new ArrayList<>();
            ret.add(ds.getUrl());
            DataSourceInfo standbyDatasource = ds.getStandbyDatasource();
            ret.add(standbyDatasource.getUser() + ":"
                    + pwdFileSender.getDsPasswdFilePath(standbyDatasource) + "@"
                    + standbyDatasource.getUrl());
            return ret;
        }
        return Arrays.asList(ds.getUrl().split(","));
    }

    public void buidAllSite() throws Exception {
        if (isBuild) {
            return;
        }

        RestTools.waitDependentServiceReady(deployInfoMgr.getFirstGatewayUrl(),
                commonConfig.getWaitServiceReadyTimeout(), "auth-server", "config-server");

        List<SiteInfo> sites = deployInfoMgr.getSites();
        for (SiteInfo site : sites) {
            logger.info("Creating site:{}", site.getName());
            try {
                buidSite(site);
            }
            catch (Exception e) {
                throw new Exception("failed to create site:" + site.getName(), e);
            }
        }
        isBuild = true;
    }
}
