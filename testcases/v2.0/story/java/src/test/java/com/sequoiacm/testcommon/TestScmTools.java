package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionMgr;
import com.sequoiacm.client.exception.ScmException;

public class TestScmTools extends TestScmBase {

    /**
     * create session, by specified site
     */
    public static ScmSession createSession( SiteWrapper site )
            throws ScmException {
        return createSession( site.getSiteServiceName() );
    }

    /**
     * create session, by specified site and user
     */
    public static ScmSession createSession( SiteWrapper site, String username,
            String password ) throws ScmException {
        return createSession( site.getSiteServiceName(), username, password );
    }

    /**
     * create session by specified serviceName
     */
    public static ScmSession createSession( String serviceName )
            throws ScmException {
        return createSession( serviceName, TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    /**
     * create session by specified serviceName and user
     */
    public static ScmSession createSession( String serviceName, String username,
            String password ) throws ScmException {
        List< String > urlList = new ArrayList< String >();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + serviceName );
        }
        ScmConfigOption scOpt = new ScmConfigOption( urlList, username,
                password );
        ScmSession session = ScmFactory.Session
                .createSession( SessionType.AUTH_SESSION, scOpt );
        System.out.println( "tmpSession = " + session.toString() );
        return session;
    }

    /**
     * create session by random serviceName
     */
    public static ScmSession createSession() throws ScmException {
        List< String > urlList = new ArrayList< String >();
        SiteWrapper site = ScmInfo.getSite();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + site.getSiteServiceName() );
        }
        ScmConfigOption scOpt = new ScmConfigOption( urlList,
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        ScmSession session = ScmFactory.Session
                .createSession( SessionType.AUTH_SESSION, scOpt );
        return session;
    }

    /**
     * create no auth session
     */
    public static ScmSession createNoAuthSession( SiteWrapper site )
            throws ScmException {
        List< String > urlList = new ArrayList< String >();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + site.getSiteServiceName() );
        }
        ScmConfigOption scOpt = new ScmConfigOption( urlList );
        return ScmFactory.Session.createSession( SessionType.NOT_AUTH_SESSION,
                scOpt );
    }

    /**
     * create sessionMgr, by specified site and define user
     */
    public static ScmSessionMgr createSessionMgr( SiteWrapper site,
            String username, String password ) throws ScmException {
        return createSessionMgr( site.getSiteServiceName(), username, password,
                Long.MAX_VALUE );
    }

    /**
     * create sessionMgr, by specified site and default scm user
     */
    public static ScmSessionMgr createSessionMgr( SiteWrapper site )
            throws ScmException {
        return createSessionMgr( site.getSiteServiceName(),
                TestScmTools.scmUserName, TestScmTools.scmPassword,
                Long.MAX_VALUE );
    }

    /**
     * create sessionMgr, by specified serviceName and user and
     * syncGateWayInterval
     */
    public static ScmSessionMgr createSessionMgr( String serviceName,
            String username, String password, long syncGateWayInterval )
            throws ScmException {
        List< String > urlList = new ArrayList< String >();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + serviceName );
        }
        ScmSessionMgr sessionMgr = null;
        ScmConfigOption scOpt = new ScmConfigOption( urlList, username,
                password );
        return ScmFactory.Session.createSessionMgr( scOpt,
                syncGateWayInterval );
    }

    /**
     * @descreption 获取一个包含所有网关的ScmConfigOption
     * @param siteName
     * @return
     * @throws ScmException
     */
    public static ScmConfigOption getScmConfigOption( String siteName )
            throws ScmException {
        List< String > urlList = new ArrayList< String >();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + siteName );
        }
        return new ScmConfigOption( urlList, TestScmTools.scmUserName,
                TestScmTools.scmPassword );
    }
}
