package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.SequoiadbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonDataOperation {
    private static final Logger logger = LoggerFactory.getLogger(CommonDataOperation.class);

    public static void deleteResidueFileContent(int siteId, String wsName, String lobId,
            ScmLocation location, String csName, String clName, ScmService service,
            MetaSource metaSource, ScmLockManager lockManager) throws SequoiadbException {
        logger.warn("local site exist residue file content:localSiteId={},wsName={},dataId={}",
                siteId, wsName, lobId);
        try {
            SdbDataDeletorImpl deletor = new SdbDataDeletorImpl(siteId, location.getSiteName(),
                    csName, clName, wsName, lobId, service, metaSource, lockManager);
            deletor.delete();
            logger.warn("delete residue file content success:localSiteId={},wsName={},dataId={}",
                    siteId, wsName, lobId);
        }
        catch (Exception e) {
            logger.error("delete residue file content failed:localSiteId={},wsName={},dataId={}",
                    siteId, wsName, lobId, e);
            throw e;
        }
    }
}
