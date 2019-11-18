package com.sequoiacm.sequoiadb.dataservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.exception.SDBError;
import com.sequoiadb.net.ConfigOptions;

public abstract class SdbService extends ScmService {
    private static final Logger logger = LoggerFactory.getLogger(SdbService.class);

    protected DataSourceWrapper sd = null;
    protected ConfigOptions connConf = null;
    protected DatasourceOptions datasourceConf = null;

    public SdbService(int siteId, ScmSiteUrl siteUrl) throws SequoiadbException {
        super(siteId, siteUrl);

        SdbSiteUrl sdbSiteUrl = (SdbSiteUrl) siteUrl;
        connConf = sdbSiteUrl.getConfig();
        datasourceConf = sdbSiteUrl.getDatasourceOption();
        AuthInfo auth = ScmFilePasswordParser.parserFile(siteUrl.getPassword());
        sd = new DataSourceWrapper(siteId, siteUrl.getUrls(), siteUrl.getUser(), auth.getPassword(),
                connConf, datasourceConf);
    }

    public abstract void _clear();

    public Sequoiadb getSequoiadb() throws SequoiadbException {
        return getSequoiadb(DataSourceWrapper.SESSION_TARGET_PRIMARY);
    }

    private Sequoiadb getSequoiadb(int preferedInstanceType) throws SequoiadbException {
        Sequoiadb sdb = null;
        try {
            sdb = sd.getConnection(preferedInstanceType);
            return sdb;
        }
        catch (SequoiadbException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "get Sequoiadb failed:siteId=" + siteId, e);
        }
    }

    public void releaseSequoiadb(Sequoiadb sdb) {
        try {
            if (null != sdb) {
                sd.releaseConnection(sdb);
            }
        }
        catch (Exception e) {
            logger.warn("release sequoiadb failed:siteId=" + siteId + ",sdb=" + sdb, e);
            if (sdb != null) {
                try {
                    sdb.disconnect();
                }
                catch (Exception e1) {
                    logger.warn("disconnect sequoiadb failed:siteId=" + siteId + ",sdb=" + sdb, e1);
                }
            }
        }
    }

    @Override
    public String getType() {
        return "sequoiadb";
    }

    @Override
    public final void clear() {
        _clear();

        if (null != sd) {
            sd.clear();
        }
        sd = null;

        connConf = null;
        datasourceConf = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("@").append(Integer.toHexString(hashCode()))
                .append(",");
        sb.append("siteId=").append(siteId).append(",");
        sb.append(sd.toString());
        return sb.toString();
    }
}
