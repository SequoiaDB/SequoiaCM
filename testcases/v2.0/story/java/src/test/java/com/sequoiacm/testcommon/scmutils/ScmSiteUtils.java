/**
 *
 */
package com.sequoiacm.testcommon.scmutils;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.Ssh;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class ScmSiteUtils extends TestScmBase {
    private static final Logger logger = Logger.getLogger( ScmSiteUtils.class );

    /**
     * @descreption 创建站点
     * @param session
     *            for reloadbizconf
     * @param siteName
     * @param gatewayurl
     * @param dstype
     * @param dsurl
     *            e.g:ZB-7:11810
     * @param user
     *            e.g:--dsuser sdbadmin
     * @param passwd
     *            e.g:--dspsswd sequoiadb
     * @return
     * @throws Exception
     */
    public static void createSite( ScmSession session, String siteName,
            String gatewayurl, int dstype, String dsurl, String user,
            String passwd ) throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( ScmInfo.getRootSite().getNode().getHost() );
            // get scm_install_dir
            String installPath = ssh.getScmInstallDir();
            // create workspace
            String cmd = installPath + "/bin/scmadmin.sh createsite -n "
                    + siteName + " --dstype " + dstype + " --dsurl " + dsurl
                    + " --dsuser " + user + " --dspasswd " + passwd
                    + " --gateway " + gatewayurl + " --user "
                    + TestScmBase.scmUserName + " --passwd "
                    + TestScmBase.scmPassword;
            ssh.exec( cmd );
            String resultMsg = ssh.getStdout();
            if ( !resultMsg.contains( "success" ) ) {
                throw new Exception( "Failed to createsite[" + siteName
                        + "], msg:\n" + resultMsg );
            }
        } finally {
            if ( null != ssh ) {
                ssh.disconnect();
            }
        }
    }

    /**
     * @descreption 删除站点
     * @param session
     * @param siteName
     * @return
     * @throws ScmException
     */
    public static void deleteSite( ScmSession session, String siteName ) {
        Sequoiadb db = null;
        try {
            db = new Sequoiadb( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
                    TestScmBase.sdbPassword );
            DBCollection cl = db.getCollectionSpace( "SCMSYSTEM" )
                    .getCollection( "SITE" );
            if ( null != siteName ) {
                BSONObject obj = new BasicBSONObject();
                obj.put( "name", siteName );
                cl.delete( obj );
            }
            DBCollection cl1 = db.getCollectionSpace( "SCMSYSTEM" )
                    .getCollection( "CONF_VERSION" );
            if ( null != siteName ) {
                BSONObject obj = new BasicBSONObject();
                obj.put( "business_name", siteName );
                cl1.delete( obj );
            }
        } finally {
            if ( db != null ) {
                db.close();
            }
        }
    }
}
