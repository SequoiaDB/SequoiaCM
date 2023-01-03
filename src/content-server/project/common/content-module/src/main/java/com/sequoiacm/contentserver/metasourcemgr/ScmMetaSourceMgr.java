package com.sequoiacm.contentserver.metasourcemgr;

import java.util.List;

import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.net.ConfigOptions;

public class ScmMetaSourceMgr {
    private static ScmMetaSourceMgr instance = new ScmMetaSourceMgr();

    private ScmMetaSourceMgr() {

    }

    public static ScmMetaSourceMgr getInstance() {
        return instance;
    }

    public ContentModuleMetaSource createInitPhaseMetaSource(List<String> urlList, String user, String passwd)
            throws ScmServerException {
        ConfigOptions connConf = new ConfigOptions();
        DatasourceOptions datasourceConf = new DatasourceOptions();
        datasourceConf.setConnectStrategy(PropertiesUtils.getConnectStrategy());
        datasourceConf.setMaxIdleCount(1);
        try {
            return new SdbMetaSource(urlList, user, passwd, connConf, datasourceConf);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to create SdbMetaSource", e);
        }

        // TODO: add other
    }
}
