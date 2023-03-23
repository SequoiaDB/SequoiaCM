package com.sequoiacm.testcommon;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.sequoiacm.testresource.CheckResource;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;

public class TestScmBase {
    private static final Logger logger = Logger.getLogger( TestScmBase.class );
    protected static final String FULLTEXT_WS_PREFIX = "fulltext_";

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

    protected static String ldapUserName;
    protected static String ldapPassword;

    @Parameters({ "FORCECLEAR", "DATADIR", "NTPSERVER", "LOCALHOSTNAME",
            "SSHUSER", "SSHPASSWD", "MAINSDBURL", "SDBUSER", "SDBPASSWD",
            "GATEWAYS", "ROOTSITESVCNAME", "SCMUSER", "SCMPASSWD",
            "SCMPASSWDPATH", "LDAPUSER", "LDAPPASSWD", "COMMONWS" })

    @BeforeSuite(alwaysRun = true)
    public static void initSuite( boolean FORCECLEAR, String DATADIR,
            String NTPSERVER, String LOCALHOSTNAME, String SSHUSER,
            String SSHPASSWD, String MAINSDBURL, String SDBUSER,
            String SDBPASSWD, String GATEWAYS, String ROOTSITESVCNAME,
            String SCMUSER, String SCMPASSWD, String SCMPASSWDPATH,
            String LDAPUSER, String LDAPPASSWD, String COMMONWS )
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

        gateWayList = parseInfo( GATEWAYS );

        rootSiteServiceName = ROOTSITESVCNAME;
        scmUserName = SCMUSER;
        scmPassword = SCMPASSWD;
        scmPasswordPath = SCMPASSWDPATH;

        ntpServer = NTPSERVER;

        ldapUserName = LDAPUSER;
        ldapPassword = LDAPPASSWD;

        // initialize scmInfo
        ScmSession session = null;
        try {
            // 加载集群站点、节点信息
            session = ScmFactory.Session.createSession( new ScmConfigOption(
                    gateWayList.get( 0 ) + "/" + rootSiteServiceName,
                    scmUserName, scmPassword ) );
            ScmInfo.refresh( session );

            // 读取配置文件中的工作区并添加s3工作区后并发创建
            HashMap< String, String > wsConfig = loadWsConfig( COMMONWS );
            WsPool.init( wsConfig );
            ScmInfo.refreshWs( session, new ArrayList<>( wsConfig.keySet() ) );

        } finally {
            if ( null != session ) {
                session.close();
            }
        }
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

    private static Properties loadConfig() throws IOException {
        String configPath = "src/test/resources/testcase_conf.properties";
        Properties config = new Properties();
        FileInputStream in = new FileInputStream( configPath );
        try {
            config.load( in );
        } finally {
            in.close();
        }
        return config;
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