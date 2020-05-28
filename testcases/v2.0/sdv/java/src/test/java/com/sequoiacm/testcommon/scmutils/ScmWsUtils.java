package com.sequoiacm.testcommon.scmutils;

import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.Ssh;
import com.sequoiacm.testcommon.TestScmBase;

public class ScmWsUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmWsUtils.class );

    /**
     * createws by /opt/sequoiacm/bin/scmadmin.sh createws ...
     *
     * @param session
     *            for reloadbizconf
     * @param wsName
     * @param metaStr
     *            e.g: "{site:'rootSite',domain:'domain1'}"
     * @param dataStr
     *            e.g: "[{site:'rootSite',domain:'domain2',
     *            data_options:{collection_space:{LobPageSize:65536}},
     *            data_sharding_type:{collection_space:'month',
     *            collection:'month'}},...]"
     * @return
     * @throws Exception
     */
    public static ScmWorkspace createWs( ScmSession session, String wsName,
            String metaStr, String dataStr ) throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( ScmInfo.getRootSite().getNode().getHost() );

            // get scm_install_dir
            String installPath = ssh.getScmInstallDir();

            // create workspace
            String cmd = installPath + "/bin/scmadmin.sh createws -n " + wsName
                    + " -m \"" + metaStr + "\" -d \"" + dataStr + "\"";
            ssh.exec( cmd );
            String resultMsg = ssh.getStdout();
            if ( !resultMsg.contains( "success" ) ) {
                throw new Exception( "Failed to create ws[" + wsName
                        + "], msg:\n" + resultMsg );
            }

            // reloadbizconf after create new workspace
            List< BSONObject > infoList = ScmSystem.Configuration.reloadBizConf(
                    ServerScope.ALL_SITE, ScmInfo.getRootSite().getSiteId(),
                    session );
            logger.info( "infoList after reloadbizconf: \n" + infoList );
            return ScmFactory.Workspace.getWorkspace( wsName, session );
        } finally {
            if ( null != ssh ) {
                ssh.disconnect();
            }
        }
    }

}
