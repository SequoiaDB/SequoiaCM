package com.sequoiacm.tools.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class ScmSiteCreator {
    private Sequoiadb db;
    private String mainSiteUrl;
    private static final Logger logger = LoggerFactory.getLogger(ScmSiteCreator.class);

    public ScmSiteCreator(String mainSiteDbUrl, String user, String passwd)
            throws ScmToolsException {
        this.mainSiteUrl = mainSiteDbUrl;
        db = SdbHelper.connectUrls(mainSiteDbUrl, user, passwd);
    }

    public void createSite(ScmSiteInfo site, boolean isContinue) throws ScmToolsException {
        CollectionSpace sysCS = SdbHelper.getCSWithCheck(db, SdbHelper.CS_SYS);
        site.setId(SdbHelper.generateCLId(db, SdbHelper.CS_SYS, SdbHelper.CL_SITE));
        if (site.isRootSite()) {
            // create main site
            System.out.println("Start to create root site:" + site.getName());
            logger.info("Start to create root site:" + site.getName());

            ScmMetaMgr mg = new ScmMetaMgr(db);
            ScmSiteInfo mainSite = mg.getMainSite();
            if (mainSite != null) {
                logger.error("This sdb already have a root site,site name:" + mainSite.getName());
                throw new ScmToolsException(
                        "This sdb already have a root site,site name:" + mainSite.getName(),
                        ScmExitCode.SCM_DUPLICATE_SITE);
            }
        }
        else {
            // create normal site
            System.out.println("Start to create branch site:" + site.getName());
            logger.info("Start to create branch site:" + site.getName());
        }

        DBCollection siteCL = SdbHelper.getCLWithCheck(sysCS, SdbHelper.CL_SITE);
        System.out.println("Inserting site to collection:" + SdbHelper.CL_SITE);
        logger.info("Inserting site to collection:" + SdbHelper.CL_SITE);
        SdbHelper.insert(siteCL, site.toBSON());

        System.out.println("Create site success:" + site.getName());
        logger.info("Create site success:" + site.getName());
    }

    public void close() {
        SdbHelper.closeCursorAndDb(db);

    }
}
