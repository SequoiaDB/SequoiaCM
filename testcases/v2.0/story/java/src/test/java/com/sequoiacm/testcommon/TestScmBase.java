package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.dsutils.CephSwiftUtils;
import com.sequoiacm.testcommon.dsutils.HbaseUtils;
import com.sequoiacm.testcommon.dsutils.HdfsUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class TestScmBase {
    private static final Logger logger = Logger.getLogger( TestScmBase.class );
    protected static final String FULLTEXT_SERVICE_NAME = "fulltext-server";
    protected static String CEPHS3_s3SecretKey;
    protected static String CEPHSwift_s3SecretKey;
    protected static String hdfsURI;

    protected static boolean forceClear;
    protected static String dataDirectory;
    protected static String ntpServer;
    protected static String localHostName;

    protected static String sshUserName;
    protected static String sshPassword;

    protected static String mainSdbUrl;
    protected static String sdbUserName;
    protected static String sdbPassword;
    protected static String newSiteSdbUrl;

    protected static List< String > gateWayList;

    protected static String rootSiteServiceName;
    protected static String scmUserName;
    protected static String scmPassword;
    protected static String scmPasswordPath;
    protected static String s3AccessKeyID;
    protected static String s3SecretKey;
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

    protected static String omServerUrl;

    protected static boolean isfulltextExists = false;
    protected static String resourceFilePath;

    @Parameters({ "FORCECLEAR", "NTPSERVER", "DATADIR", "LOCALHOSTNAME",
            "SSHUSER", "SSHPASSWD", "MAINSDBURL", "SDBUSER", "SDBPASSWD",
            "NEWSITESDBURL", "GATEWAYS", "ROOTSITESVCNAME", "SCMUSER",
            "SCMPASSWD", "SCMPASSWDPATH", "OMSERVERURL", "LDAPUSER",
            "LDAPPASSWD", "CEPHS3SECRETKEY", "CEPHSWIFTSECRETKEY", "HDFSURL",
            "COMMONWS" })

    @BeforeSuite(alwaysRun = true)
    public static void initSuite( boolean FORCECLEAR, String NTPSERVER,
            String DATADIR, String LOCALHOSTNAME, String SSHUSER,
            String SSHPASSWD, String MAINSDBURL, String SDBUSER,
            String SDBPASSWD, String NEWSITESDBURL, String GATEWAYS,
            String ROOTSITESVCNAME, String SCMUSER, String SCMPASSWD,
            String SCMPASSWDPATH, String OMSERVERURL, String LDAPUSER,
            String LDAPPASSWD, String CEPHS3SECRETKEY,
            String CEPHSWIFTSECRETKEY, String HDFSURL, String COMMONWS )
            throws Exception {

        // 加载xml配置
        forceClear = FORCECLEAR;
        dataDirectory = DATADIR;
        localHostName = LOCALHOSTNAME;

        sshUserName = SSHUSER;
        sshPassword = SSHPASSWD;

        mainSdbUrl = MAINSDBURL;
        sdbUserName = SDBUSER;
        sdbPassword = SDBPASSWD;
        newSiteSdbUrl = NEWSITESDBURL;

        gateWayList = parseInfo( GATEWAYS );

        rootSiteServiceName = ROOTSITESVCNAME;
        scmUserName = SCMUSER;
        scmPassword = SCMPASSWD;
        scmPasswordPath = SCMPASSWDPATH;
        omServerUrl = OMSERVERURL;

        ntpServer = NTPSERVER;
        ldapUserName = LDAPUSER;
        ldapPassword = LDAPPASSWD;

        CEPHS3_s3SecretKey = CEPHS3SECRETKEY;
        CEPHSwift_s3SecretKey = CEPHSWIFTSECRETKEY;
        hdfsURI = HDFSURL;
        // s3配置
        s3AccessKeyID = "ABCDEFGHIJKLMNOPQRST";
        s3SecretKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCD";
        s3WorkSpaces = "ws_s3_default";
        // 公共桶桶名
        bucketName = "commbucket";
        enableVerBucketName = "commbucketwithversion";
        susVerBucketName = "commsuspendedbucket";
        // schedulePreferredZone目录下用例使用
        zone1 = "zone1";
        zone2 = "zone2";
        defaultRegion = "DefaultRegion";

        // SEQUOIACM-936 关闭全局工作区缓存
        ScmFactory.Workspace.setKeepAliveTime( 0 );
        ScmSession session = null;
        try {
            // 加载集群站点、节点信息
            session = ScmFactory.Session.createSession( new ScmConfigOption(
                    gateWayList.get( 0 ) + "/" + rootSiteServiceName,
                    scmUserName, scmPassword ) );
            ScmInfo.refresh( session );

            // 读取配置文件中的工作区并添加s3工作区后并发创建
            HashMap< String, String > wsConfig = loadWsConfig( COMMONWS );
            wsConfig.put( s3WorkSpaces, null );
            WsPool.init( wsConfig );
            ScmInfo.refreshWs( session, new ArrayList<>( wsConfig.keySet() ) );

            // 检查服务
            serviceList = ScmSystem.ServiceCenter.getServiceList( session );
            if ( serviceList.contains( FULLTEXT_SERVICE_NAME ) ) {
                isfulltextExists = true;
            }

            // 数据源环境检查清理
            CleanDataSource();

            // 刷新s3key、设置默认工作区、创建公共桶
            for ( String serviceName : serviceList ) {
                if ( serviceName.contains( "s3" ) ) {
                    ScmFactory.S3.setDefaultRegion( session, s3WorkSpaces );
                    ScmFactory.S3.refreshAccesskey( session, scmUserName,
                            scmPassword, s3AccessKeyID, s3SecretKey );
                    createS3CommBucket();
                }
            }

        } finally {
            if ( null != session ) {
                session.close();
            }
        }
        resourceFilePath = FileLoader.loadAndGetFilePath( dataDirectory,
                "story" );
    }

    @AfterSuite(alwaysRun = true)
    public static void finiSuite() throws Exception {
        WsPool.destroy();
        // SEQUOIACM-1316
        Sequoiadb sdb = null;
        try {
            sdb = TestSdbTools.getSdb( mainSdbUrl );
            DBCollection cl = sdb.getCollectionSpace( "SCMSYSTEM" )
                    .getCollection( "DATA_TABLE_NAME_HISTORY" );
            cl.truncate();
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }
    }

    private static void CleanDataSource() throws Exception {
        List< SiteWrapper > allSites = ScmInfo.getAllSites();
        for ( SiteWrapper site : allSites ) {
            switch ( site.getDataType() ) {
            case CEPH_S3:
                CephS3Utils.deleteAllBuckets( site );
                break;
            case CEPH_SWIFT:
                CephSwiftUtils.deleteAllContainers( site );
                break;
            case HDFS:
                List< WsWrapper > wspList = ScmInfo.getAllWorkspaces();
                for ( WsWrapper wsp : wspList ) {
                    String rootPath = HdfsUtils.getRootPath( site, wsp );
                    HdfsUtils.deletePath( site, rootPath );
                }
                break;
            case HBASE:
                HbaseUtils.deleteTableInHbase( site );
                break;
            case SEQUOIADB:
                TestSdbTools.deleteLobCS( site );
                break;
            case SFTP:
                break;
            default:
                Assert.fail(
                        "dataSourceType not match: " + site.getDataType() );
            }
        }
    }

    private static void createS3CommBucket() throws Exception {
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

    /**
     * @description 读取配置文件中的工作区名及分区规则
     * @param common_workspaces
     * @return
     */
    private static HashMap< String, String > loadWsConfig(
            String common_workspaces ) {
        HashMap< String, String > map = new HashMap<>();
        String[] wsConfigs = common_workspaces.split( "," );
        for ( String wsConfig : wsConfigs ) {
            String[] strings = wsConfig.split( ":" );
            map.put( strings[ 0 ], strings[ 1 ] );
        }
        return map;
    }
}