package com.sequoiacm.tools.common;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.element.ScmSiteConfig;
import com.sequoiacm.tools.element.ScmSiteInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class ScmSiteHelper {

    private static final Logger logger = LoggerFactory.getLogger(ScmSiteHelper.class.getName());

    private ScmSiteHelper() {
    }

    /**
     *
     * @param siteConf
     *            site config instance.
     * @param gatewayUrls
     *            List's element has this formate:host:port.
     * @param username
     *            sequoiaCM user name.
     * @param password
     *            sequoiaCM user password.
     * @param checkDs
     *            check data url is connectable.
     * @throws ScmToolsException
     */
    public static void createSite(ScmSiteConfig siteConf, List<String> gatewayUrls, String username,
            String password, boolean checkDs) throws ScmToolsException {
        ScmSession ss = null;
        try {
            // check data url is connectable
            if (checkDs) {
                // ScmSiteConf transform ScmSiteInfo
                ScmSiteInfo siteInfo = transformSiteInfo(siteConf);
                // id is not practical meaning
                siteInfo.setId(1);
                ScmDatasourceUtil.validateDatasourceUrl(siteInfo);
            }
            ss = ScmFactory.Session
                    .createSession(new ScmConfigOption(gatewayUrls, username, password));
            RestDispatcher.getInstance().createSite(ss, siteConf);
        }
        catch (ScmException e) {
            logger.error("create site failed:siteName={}, error=", siteConf.getName(), e.getError(),
                    e);
            ScmCommon.throwToolException("create site failed", e);
        }
        finally {
            if (ss != null) {
                ss.close();
            }
        }
    }

    /**
     *
     * @param siteConf
     *            site config instance.
     * @param gatewayUrl
     *            sequoiaCM node address, format is host:port.
     * @param username
     *            sequoiaCM user name.
     * @param password
     *            sequoiaCM user password.
     * @param checkDs
     *            check data url is connectable.
     * @throws ScmToolsException
     */
    public static void createSite(ScmSiteConfig siteConf, String gatewayUrl, String username,
            String password, boolean checkDs) throws ScmToolsException {
        createSite(siteConf, Collections.singletonList(gatewayUrl), username, password, checkDs);
    }

    private static ScmSiteInfo transformSiteInfo(ScmSiteConfig siteConf)
            throws ScmInvalidArgumentException, ScmToolsException {
        ScmSiteInfo siteInfo = new ScmSiteInfo();
        siteInfo.setName(siteConf.getName());
        siteInfo.setRootSite(siteConf.isRootSite());
        siteInfo.setDataType(siteConf.getDataType().getType());
        siteInfo.setDataUrl(siteConf.getDataUrl());
        siteInfo.setDataUser(siteConf.getDataUser());
        siteInfo.setDataPasswd(siteConf.getDataPassword());
        siteInfo.setDataConf(siteConf.getDataConfig());
        siteInfo.setMetaUrl(siteConf.getMetaUrl());
        siteInfo.setMetaUser(siteConf.getMetaUser());
        siteInfo.setMetaPasswd(siteConf.getMetaPassword());
        return siteInfo;
    }

}
