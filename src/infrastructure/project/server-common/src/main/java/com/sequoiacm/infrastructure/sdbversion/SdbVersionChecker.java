package com.sequoiacm.infrastructure.sdbversion;

import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.metasource.config.SequoiadbConfig;
import com.sequoiadb.base.DBVersion;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class SdbVersionChecker {
    private static final Logger logger = LoggerFactory.getLogger(SdbVersionChecker.class);
    private SequoiadbConfig config;
    private String plainPasswd;
    @Value("${scm.sdbVersionChecker.versionCacheTTL:300000}")
    private int versionCacheTTL = 10 * 60 * 1000;
    private long lastRefreshVersionTime = 0;

    private volatile SdbVersion sdbVersion = null;

    private SequoiadbDatasource sds;

    public SdbVersionChecker(SequoiadbConfig config) {
        this.config = config;
        plainPasswd = ScmFilePasswordParser.parserFile(config.getPassword()).getPassword();
    }

    public SdbVersionChecker(SequoiadbDatasource sds) {
        this.sds = sds;
    }

    public boolean isCompatible(List<SdbVersionRange> versionRanges) {
        if (System.currentTimeMillis() - lastRefreshVersionTime > versionCacheTTL) {
            sdbVersion = getSdbVersionSilence();
        }

        if (sdbVersion == null) {
            throw new IllegalArgumentException("failed to get sequoiadb version");
        }

        for (SdbVersionRange range : versionRanges) {
            if (range.isInRange(sdbVersion)) {
                return true;
            }
        }
        return false;
    }

    public SdbVersion getSdbVersionSilence() {
        Sequoiadb sdb = null;
        try {
            if (null == sds) {
                sdb = new Sequoiadb(config.getUrls(), config.getUsername(), plainPasswd, null);
            }
            else {
                sdb = sds.getConnection();
            }
            DBVersion version = sdb.getDBVersion();
            lastRefreshVersionTime = System.currentTimeMillis();
            return new SdbVersion(version.getVersion(), version.getSubVersion(),
                    version.getFixVersion());
        }
        catch (Exception e) {
            logger.warn("get sdb version failed", e);
            return null;
        }
        finally {
            releaseConnect(sdb);
        }
    }

    public SdbVersion getSdbVersion() {
        return sdbVersion;
    }

    private void releaseConnect(Sequoiadb sdb) {
        if (null == sdb) {
            return;
        }
        try {
            if (null != sds) {
                sds.releaseConnection(sdb);
            }
            else {
                IOUtils.close(sdb);
            }
        }
        catch (Exception e) {
            logger.warn("release connection failed", e);
            IOUtils.close(sdb);
        }
    }
}
