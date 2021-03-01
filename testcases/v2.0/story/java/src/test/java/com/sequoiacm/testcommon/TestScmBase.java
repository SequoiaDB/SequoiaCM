package com.sequoiacm.testcommon;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

public class TestScmBase {
    private static final Logger logger = Logger.getLogger( TestScmBase.class );
    protected static final String FULLTEXT_WS_PREFIX = "fulltext_";
    private static final int WS_NUM_PER_POOL = 1;

    protected static boolean forceClear;
    protected static String dataDirectory;
    protected static String ntpServer;
    protected static String localHostName;

    protected static String sshUserName;
    protected static String sshPassword;

    protected static String mainSdbUrl;
    protected static String sdbUserName;
    protected static String sdbPassword;

    protected static List< String > gateWayList;

    protected static String rootSiteServiceName;
    protected static String scmUserName;
    protected static String scmPassword;
    protected static String scmPasswordPath;
    protected static String cloudDiskUserName;

    protected static String ldapUserName;
    protected static String ldapPassword;

    @Parameters({ "FORCECLEAR", "DATADIR", "NTPSERVER", "LOCALHOSTNAME",
            "SSHUSER", "SSHPASSWD", "MAINSDBURL", "SDBUSER", "SDBPASSWD",
            "GATEWAYS", "ROOTSITESVCNAME", "SCMUSER", "SCMPASSWD",
            "CLOUDDISKUSERNAME", "LDAPUSER", "LDAPPASSWD", "SCMPASSWDPATH" })

    @BeforeSuite(alwaysRun = true)
    public static void initSuite( boolean FORCECLEAR, String DATADIR,
            String NTPSERVER, String LOCALHOSTNAME, String SSHUSER,
            String SSHPASSWD, String MAINSDBURL, String SDBUSER,
            String SDBPASSWD, String GATEWAYS, String ROOTSITESVCNAME,
            String SCMUSER, String SCMPASSWD, String CLOUDDISKUSERNAME,
            String LDAPUSER, String LDAPPASSWD, String SCMPASSWDPATH )
            throws Exception {

        forceClear = FORCECLEAR;
        dataDirectory = DATADIR;
        ntpServer = NTPSERVER;
        localHostName = LOCALHOSTNAME;

        sshUserName = SSHUSER;
        sshPassword = SSHPASSWD;

        mainSdbUrl = MAINSDBURL;
        sdbUserName = SDBUSER;
        sdbPassword = SDBPASSWD;

        gateWayList = parseInfo( GATEWAYS );

        rootSiteServiceName = ROOTSITESVCNAME;
        scmUserName = SCMUSER;
        scmPassword = SCMPASSWD;
        scmPasswordPath = SCMPASSWDPATH;
        cloudDiskUserName = CLOUDDISKUSERNAME;

        ldapUserName = LDAPUSER;
        ldapPassword = LDAPPASSWD;

        // initialize scmInfo
        ScmSession session = null;
        try {
            List< String > urlList = new ArrayList< String >();
            for ( String gateWay : gateWayList ) {
                urlList.add( gateWay + "/" + ROOTSITESVCNAME );
            }
            logger.info( "gateWay info \n" + gateWayList );
            try {
                for ( String url : urlList ) {
                    checkSiteIsOk( url );
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            ScmConfigOption scOpt = new ScmConfigOption( urlList,
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            session = ScmFactory.Session
                    .createSession( SessionType.AUTH_SESSION, scOpt );
            ScmInfo.refresh( session );
            WsPool.init( prepareWs( session ) );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    @AfterSuite(alwaysRun = true)
    public static void finiSuite() throws Exception {
        WsPool.destroy();
    }

    private static List< String > parseInfo( String infos ) {
        List< String > infoList = new ArrayList< String >();
        if ( infos.contains( "," ) ) {
            String[] infoArr = infos.split( "," );
            for ( String info : infoArr ) {
                infoList.add( info );
            }
        } else {
            infoList.add( infos );
        }
        return infoList;
    }

    private static void checkSiteIsOk( String url ) throws Exception {
        int i = 0;
        int tryNum = 6;
        int interval = 10 * 1000;
        ScmConfigOption scOpt = new ScmConfigOption( url,
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        for ( ; i < tryNum; i++ ) {
            ScmSession session = null;
            try {
                session = ScmFactory.Session
                        .createSession( SessionType.AUTH_SESSION, scOpt );
                break;
            } catch ( ScmException e ) {
                if ( ScmError.HTTP_NOT_FOUND != e.getError()
                        || i == tryNum - 1 ) {
                    e.printStackTrace();
                    throw e;
                }
                Thread.sleep( interval );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private static List< String > prepareWs( ScmSession session )
            throws Exception {
        List< String > wsNames = new ArrayList<>();
        ScmCursor< ScmWorkspaceInfo > wsInfo = ScmFactory.Workspace
                .listWorkspace( session );
        String wsNamePrefix = getWsNamePrefix();
        while ( wsInfo.hasNext() ) {
            ScmWorkspaceInfo info = wsInfo.getNext();
            if ( info.getName().startsWith( wsNamePrefix ) ) {
                ScmWorkspaceUtil.deleteWs( info.getName(), session );
            }
        }
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < WS_NUM_PER_POOL; i++ ) {
            String wsName =  wsNamePrefix + "_test_" + i;
            threadExec.addWorker(
                    new TestScmBase().new CreateWS( session, wsName ) );
            wsNames.add( wsName );
        }
        threadExec.run();
        return wsNames;
    }

    private static String getWsNamePrefix() throws UnknownHostException {
        return FULLTEXT_WS_PREFIX
                + InetAddress.getLocalHost().getHostName().replace( "-", "_" );
    }

    private class CreateWS {
        private ScmSession session;
        private String wsName;

        public CreateWS( ScmSession session, String wsName ) {
            this.session = session;
            this.wsName = wsName;
        }

        @ExecuteOrder(step = 1)
        private void created() throws ScmException, InterruptedException {
            ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
            ScmWorkspaceUtil.wsSetPriority( session, wsName );
        }
    }
}