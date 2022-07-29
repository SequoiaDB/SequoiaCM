package com.sequoiacm.testcommon;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TestScmBase {
    private static final Logger logger = Logger.getLogger( TestScmBase.class );
    protected static final String FULLTEXT_SERVICE_NAME = "fulltext-server";
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
    protected static String s3AccessKeyID;
    protected static String s3SecretKey;
    protected static String s3ClientUrl;
    protected static String s3WorkSpaces;
    protected static List< String > serviceList;

    protected static String bucketName;
    protected static String enableVerBucketName;
    protected static String susVerBucketName;

    protected static String ldapUserName;
    protected static String ldapPassword;

    protected static String zone1;
    protected static String zone2;
    protected static String defaultRegion;

    @Parameters({ "FORCECLEAR", "DATADIR", "NTPSERVER", "LOCALHOSTNAME",
            "SSHUSER", "SSHPASSWD", "MAINSDBURL", "SDBUSER", "SDBPASSWD",
            "GATEWAYS", "ROOTSITESVCNAME", "SCMUSER", "SCMPASSWD", "LDAPUSER",
            "LDAPPASSWD", "SCMPASSWDPATH", "S3ACCESSKEYID", "S3SECRETKEY",
            "S3WOKERSPACES" })

    @BeforeSuite(alwaysRun = true)
    public static void initSuite( boolean FORCECLEAR, String DATADIR,
            String NTPSERVER, String LOCALHOSTNAME, String SSHUSER,
            String SSHPASSWD, String MAINSDBURL, String SDBUSER,
            String SDBPASSWD, String GATEWAYS, String ROOTSITESVCNAME,
            String SCMUSER, String SCMPASSWD, String LDAPUSER,
            String LDAPPASSWD, String SCMPASSWDPATH, String S3ACCESSKEYID,
            String S3SECRETKEY, String S3WOKERSPACES ) throws Exception {

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
        s3ClientUrl = "http://" + gateWayList.get( 0 ) + "/s3";
        s3AccessKeyID = S3ACCESSKEYID;
        s3SecretKey = S3SECRETKEY;
        s3WorkSpaces = S3WOKERSPACES;
        ldapUserName = LDAPUSER;
        ldapPassword = LDAPPASSWD;

        bucketName = "commbucket";
        enableVerBucketName = "commbucketwithversion";
        susVerBucketName = "commsuspendedbucket";

        // TODO schedulePreferredZone特性需要，暂时写死，后续替换为xml传参形式
        zone1 = "zone1";
        zone2 = "zone2";
        defaultRegion = "DefaultRegion";

        // TODO:SEQUOIACM-936 暂时关闭缓存
        ScmFactory.Workspace.setKeepAliveTime( 0 );
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
            serviceList = ScmSystem.ServiceCenter.getServiceList( session );
            if ( serviceList.contains( FULLTEXT_SERVICE_NAME ) ) {
                List< String > wsNames = prepareWs( session );
                List< WsWrapper > wsps = ScmInfo.getWsList( session );
                for ( WsWrapper wsp : wsps ) {
                    wsNames.add( wsp.getName() );
                }
                WsPool.init( wsNames );
            }

            // 创建S3公共桶
            createS3CommBucket( session );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    @AfterSuite(alwaysRun = true)
    public static void finiSuite() throws Exception {
        if ( serviceList.contains( FULLTEXT_SERVICE_NAME ) ) {
            WsPool.destroy();
        }
    }

    private static void createS3CommBucket( ScmSession session )
            throws Exception {
        AmazonS3 s3Client = null;
        try {
            // clean up existing buckets
            s3Client = S3Utils.buildS3Client();

            if ( s3Client.doesBucketExistV2( bucketName ) ) {
                S3Utils.deleteAllObjects( s3Client, bucketName );
                s3Client.deleteBucket( bucketName );
            }
            if ( s3Client.doesBucketExistV2( enableVerBucketName ) ) {
                S3Utils.deleteAllObjectVersions( s3Client,
                        enableVerBucketName );
                s3Client.deleteBucket( enableVerBucketName );
            }

            if ( s3Client.doesBucketExistV2( susVerBucketName ) ) {
                S3Utils.deleteAllObjectVersions( s3Client, susVerBucketName );
                s3Client.deleteBucket( susVerBucketName );
            }

            // create bucket
            s3Client.createBucket( bucketName );
            // create bucket by enable versioning
            s3Client.createBucket( enableVerBucketName );
            S3Utils.setBucketVersioning( s3Client, enableVerBucketName,
                    "Enabled" );
            // create bucket by enable versioning
            s3Client.createBucket( susVerBucketName );
            S3Utils.setBucketVersioning( s3Client, susVerBucketName,
                    "Suspended" );
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private static List< String > parseInfo( String infos ) {
        List< String > infoList = new ArrayList<>();
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
            String wsName = wsNamePrefix + "_test_" + i;
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