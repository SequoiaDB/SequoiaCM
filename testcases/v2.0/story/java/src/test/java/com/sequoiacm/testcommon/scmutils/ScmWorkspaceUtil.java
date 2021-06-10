package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephSwiftDataLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHdfsDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.Ssh;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;

/**
 * @Description ScmWrokspaceUtil.java
 * @author luweikang
 * @date 2018年6月23日
 */
public class ScmWorkspaceUtil extends TestScmBase {

    private static final Logger logger = Logger
            .getLogger( ScmWorkspaceUtil.class );

    /**
     * 创建普通的工作区
     * 
     * @param session
     * @param wsName
     * @param siteNum
     * @return
     * @throws ScmException
     * @throws InterruptedException
     */
    public static ScmWorkspace createWS( ScmSession session, String wsName,
            int siteNum ) throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( getDataLocationList( siteNum ) );
        conf.setMetaLocation( getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        return createWS( session, conf );
    }

    /**
     * 创建带批次和目录开关的工作区
     * 
     * @param session
     * @param wsName
     * @param siteNum
     * @param batchShardingType
     * @param regexp
     * @param pattern
     * @param isFileNameUnique
     * @param enableDir
     * @return
     * @throws ScmException
     * @throws InterruptedException
     */
    public static ScmWorkspace createWS( ScmSession session, String wsName,
            int siteNum, ScmShardingType batchShardingType, String regexp,
            String pattern, boolean isFileNameUnique, boolean enableDir )
            throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( getDataLocationList( siteNum ) );
        conf.setMetaLocation( getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        conf.setBatchIdTimeRegex( regexp );
        conf.setBatchShardingType( batchShardingType );
        conf.setBatchIdTimePattern( pattern );
        conf.setBatchFileNameUnique( isFileNameUnique );
        conf.setEnableDirectory( enableDir );
        return createWS( session, conf );
    }

    /**
     * 创建带批次信息的工作区
     * 
     * @param session
     * @param wsName
     * @param siteNum
     * @param batchShardingType
     * @param regexp
     * @param pattern
     * @param isFileNameUnique
     * @return
     * @throws ScmException
     * @throws InterruptedException
     */
    public static ScmWorkspace createWS( ScmSession session, String wsName,
            int siteNum, ScmShardingType batchShardingType, String regexp,
            String pattern, boolean isFileNameUnique )
            throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( getDataLocationList( siteNum ) );
        conf.setMetaLocation( getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        conf.setBatchIdTimeRegex( regexp );
        conf.setBatchShardingType( batchShardingType );
        conf.setBatchIdTimePattern( pattern );
        conf.setBatchFileNameUnique( isFileNameUnique );
        return createWS( session, conf );
    }

    public static ScmWorkspace createWS( ScmSession session,
            ScmWorkspaceConf conf ) throws ScmException, InterruptedException {
        ScmFactory.Workspace.createWorkspace( session, conf );
        ScmWorkspace ws = null;
        for ( int i = 0; i < 15; i++ ) {
            try {
                ws = ScmFactory.Workspace.getWorkspace( conf.getName(),
                        session );
                break;
            } catch ( ScmException e ) {
                Thread.sleep( 1000 );
                if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                    throw e;
                }
            }
        }
        return ws;
    }

    public static ScmMetaLocation getMetaLocation(
            ScmShardingType scmShardingType )
            throws ScmInvalidArgumentException {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        return new ScmSdbMetaLocation( rootSite.getSiteName(), scmShardingType,
                TestSdbTools.getDomainNames( rootSite.getMetaDsUrl() )
                        .get( 0 ) );
    }

    public static List< ScmDataLocation > getDataLocationList( int siteNum )
            throws ScmInvalidArgumentException {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = new ArrayList<>();
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        if ( siteNum > 1 ) {
            siteList = ScmInfo.getBranchSites( siteNum - 1 );
        } else if ( siteNum < 1 ) {
            throw new IllegalArgumentException(
                    "error, create ws siteNum can't equal " + siteNum );
        }
        siteList.add( rootSite );
        for ( int i = 0; i < siteList.size(); i++ ) {
            String siteName = siteList.get( i ).getSiteName();
            String dataType = siteList.get( i ).getDataType().toString();
            switch ( dataType ) {
            case "sequoiadb":
                String domainName = TestSdbTools
                        .getDomainNames( siteList.get( i ).getDataDsUrl() )
                        .get( 0 );
                scmDataLocationList
                        .add( new ScmSdbDataLocation( siteName, domainName ) );
                break;
            case "hbase":
                scmDataLocationList.add( new ScmHbaseDataLocation( siteName ) );
                break;
            case "hdfs":
                scmDataLocationList.add( new ScmHdfsDataLocation( siteName ) );
                break;
            case "ceph_s3":
                scmDataLocationList
                        .add( new ScmCephS3DataLocation( siteName ) );
                break;
            case "ceph_swift":
                scmDataLocationList
                        .add( new ScmCephSwiftDataLocation( siteName ) );
                break;
            default:
                Assert.fail( "dataSourceType not match: " + dataType );
            }
        }
        return scmDataLocationList;
    }

    public static void wsSetPriority( ScmSession session, String wsName )
            throws ScmException, InterruptedException {
        wsSetPriority( session, wsName, ScmPrivilegeType.ALL );
    }

    public static void wsSetPriority( ScmSession session, String wsName,
            ScmPrivilegeType privilege )
            throws ScmException, InterruptedException {

        ScmUser superuser = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        ScmResource rs = ScmResourceFactory.createWorkspaceResource( wsName );
        ScmFactory.Role.grantPrivilege( session,
                superuser.getRoles().iterator().next(), rs, privilege );

        for ( int i = 0; i < 6; i++ ) {
            Thread.sleep( 10000 );
            try {
                ScmFactory.File.listInstance(
                        ScmFactory.Workspace.getWorkspace( wsName, session ),
                        ScopeType.SCOPE_ALL, new BasicBSONObject() );
                ScmFactory.Fulltext.getIndexInfo(
                        ScmFactory.Workspace.getWorkspace( wsName, session ) );
                return;
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(),
                        ScmError.OPERATION_UNAUTHORIZED, e.getMessage() );
            }
        }
        Assert.fail( "grantPrivilege is not done in 60 seconds" );
    }

    public static ScmWorkspace createWs( ScmSession session, String wsName,
            String metaStr, String dataStr ) throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( ScmInfo.getRootSite().getNode().getHost() );

            // get scm_install_dir
            String installPath = ssh.getScmInstallDir();

            // create workspace
            String cmd = installPath + "/bin/scmadmin.sh createws -n " + wsName
                    + " -m \"" + metaStr + "\" -d \"" + dataStr + "\" --url \""
                    + TestScmBase.gateWayList.get( 0 ) + "/"
                    + ScmInfo.getRootSite().getSiteName().toLowerCase()
                    + "\" --user " + TestScmBase.scmUserName + " --password "
                    + TestScmBase.scmUserName;
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

    public static void deleteWs( String wsName, ScmSession session )
            throws Exception {
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        } catch ( ScmException e ) {
            e.printStackTrace();
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                throw e;
            }
        }
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            try {
                ScmFactory.Workspace.getWorkspace( wsName, session );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                    throw e;
                }
                TestSdbTools.Workspace.checkWsCs( wsName, session );
                return;
            }
        }
        Assert.fail( "delete ws is not done in 15 seconds" );
    }

    public static void wsRemoveSite( ScmWorkspace ws, String siteName )
            throws ScmException, InterruptedException {
        ws.removeDataLocation( siteName );
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            List< ScmDataLocation > dataList = ws.getDataLocations();
            List< String > nameList = new ArrayList<>();
            for ( ScmDataLocation data : dataList ) {
                nameList.add( data.getSiteName() );
            }
            for ( int j = 0; j < dataList.size(); j++ ) {
                if ( !nameList.contains( siteName ) ) {
                    return;
                }
            }
        }
        Assert.fail( "ws remove site is not done in 15 seconds" );
    }

    /**
     * @param ws
     * @param site
     * @throws ScmException
     * @throws InterruptedException
     */
    public static void wsAddSite( ScmWorkspace ws, SiteWrapper site )
            throws ScmException, InterruptedException {

        ScmDataLocation dataLocation = null;
        String siteName = site.getSiteName();
        switch ( site.getDataType().toString() ) {
        case "sequoiadb":
            String domainName = TestSdbTools
                    .getDomainNames( site.getDataDsUrl() ).get( 0 );
            dataLocation = new ScmSdbDataLocation( siteName, domainName );
            break;
        case "hbase":
            dataLocation = new ScmHbaseDataLocation( siteName );
            break;
        case "hdfs":
            dataLocation = new ScmHdfsDataLocation( siteName );
            break;
        case "ceph_s3":
            dataLocation = new ScmCephS3DataLocation( siteName );
            break;
        case "ceph_swift":
            dataLocation = new ScmCephSwiftDataLocation( siteName );
            break;
        default:
            Assert.fail( "dataSourceType not match: "
                    + site.getDataType().toString() );
        }

        ws.addDataLocation( dataLocation );
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            List< ScmDataLocation > dataList = ws.getDataLocations();
            List< String > nameList = new ArrayList<>();
            for ( ScmDataLocation data : dataList ) {
                nameList.add( data.getSiteName() );
            }
            for ( int j = 0; j < dataList.size(); j++ ) {

                if ( nameList.contains( siteName ) ) {
                    return;
                }
            }
        }
        Assert.fail( "ws add site is not done in 15 seconds" );
    }

    public static void wsAddSite( ScmWorkspace ws,
            ScmDataLocation dataLocation )
            throws ScmException, InterruptedException {

        ws.addDataLocation( dataLocation );
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            List< ScmDataLocation > dataList = ws.getDataLocations();
            List< String > nameList = new ArrayList<>();
            for ( ScmDataLocation data : dataList ) {
                nameList.add( data.getSiteName() );
            }
            for ( int j = 0; j < dataList.size(); j++ ) {

                if ( nameList.contains( dataList.get( j ).getSiteName() ) ) {
                    return;
                }
            }
        }
        Assert.fail( "ws add site is not done in 15 seconds" );
    }
}
