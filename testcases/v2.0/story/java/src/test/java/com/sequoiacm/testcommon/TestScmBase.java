package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
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
                        "SSHUSER", "SSHPASSWD", "MAINSDBURL", "SDBUSER",
                        "SDBPASSWD",
                        "GATEWAYS", "ROOTSITESVCNAME", "SCMUSER", "SCMPASSWD",
                        "LDAPUSER", "LDAPPASSWD",
                        "SCMPASSWDPATH" })

    @BeforeSuite(alwaysRun = true)
    public static void initSuite( boolean FORCECLEAR, String DATADIR,
            String NTPSERVER, String LOCALHOSTNAME, String SSHUSER,
            String SSHPASSWD, String MAINSDBURL, String SDBUSER,
            String SDBPASSWD, String GATEWAYS, String ROOTSITESVCNAME,
            String SCMUSER, String SCMPASSWD,
            String LDAPUSER, String LDAPPASSWD, String SCMPASSWDPATH )
            throws ScmException {

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
        } catch ( ScmException e ) {
            e.printStackTrace();
            throw e;
        } finally {
            if ( null != session ) {
                session.close();
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
}