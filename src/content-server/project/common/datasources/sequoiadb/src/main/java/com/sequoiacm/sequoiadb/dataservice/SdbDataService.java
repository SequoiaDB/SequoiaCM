package com.sequoiacm.sequoiadb.dataservice;

import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.SDBError;

public class SdbDataService extends SdbService {
    public SdbDataService(int siteId, ScmSiteUrl siteUrl) throws SequoiadbException {
        super(siteId, siteUrl);
        //try get a connection
        releaseSequoiadb(getSequoiadb());
    }

    public void removeLob(String csName, String clName, String id) throws SequoiadbException {
        Sequoiadb sdb = null;
        try {
            sdb = sd.getConnection();
            SequoiadbHelper.removeLob(sdb, csName, clName, id);
        }
        catch (SequoiadbException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SequoiadbException (SDBError.SDB_SYS.getErrorCode(),
                    "removeLob failed:siteId=" + siteId + ",csName=" + csName + ",clName=" + clName
                    + ",id=" + id, e);
        }
        finally {
            if (null != sdb) {
                sd.releaseConnection(sdb);
            }
        }
    }

    @Override
    public void _clear() {
        // no need to clear
    }

    @Override
    public boolean supportsBreakpointUpload() {
        return true;
    }
}