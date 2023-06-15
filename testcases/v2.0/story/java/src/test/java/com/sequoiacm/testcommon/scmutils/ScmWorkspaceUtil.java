package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @Description ScmWrokspaceUtil.java
 * @author luweikang
 * @date 2018年6月23日
 */
public class ScmWorkspaceUtil extends TestScmBase {

    private static final Logger logger = Logger
            .getLogger( ScmWorkspaceUtil.class );

    /**
     * @descreption 创建普通的工作区
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
        conf.setEnableDirectory( true );
        return createWS( session, conf );
    }

    /**
     * @descreption 创建工作区禁用目录
     * @param session
     * @param wsName
     * @param siteNum
     * @return
     * @throws ScmException
     * @throws InterruptedException
     */
    public static ScmWorkspace createDisEnableDirectoryWS( ScmSession session,
            String wsName, int siteNum )
            throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( getDataLocationList( siteNum ) );
        conf.setMetaLocation( getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        conf.setEnableDirectory( false );
        return createWS( session, conf );
    }

    /**
     * @descreption 指定分区方式创建工作区
     * @param session
     * @param wsName
     * @param siteNum
     * @param dataLocationShardingType
     * @return
     * @throws ScmException
     * @throws InterruptedException
     */
    public static ScmWorkspace createWS( ScmSession session, String wsName,
            int siteNum, ScmShardingType dataLocationShardingType )
            throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations(
                getDataLocationList( siteNum, dataLocationShardingType ) );
        conf.setMetaLocation( getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        return createWS( session, conf );
    }

    /**
     * @descreption 创建带批次和目录开关的工作区
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
     * @descreption 创建带批次信息的工作区
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
        conf.setEnableDirectory( true );
        return createWS( session, conf );
    }

    /**
     * @descreption 创建工作区
     * @param session
     * @param conf
     * @return ScmWorkspace
     * @throws ScmException
     * @throws InterruptedException
     */
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

    /**
     * @descreption 获取工作区元数据Location @param scmShardingType @return
     *              ScmMetaLocation @throws
     */
    public static ScmMetaLocation getMetaLocation(
            ScmShardingType scmShardingType )
            throws ScmInvalidArgumentException {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        return new ScmSdbMetaLocation( rootSite.getSiteName(), scmShardingType,
                TestSdbTools.getDomainNames( rootSite.getMetaDsUrl() )
                        .get( 0 ) );
    }

    /**
     * @descreption 获取工作区站点数据Location
     * @param siteNum
     * @return List< ScmDataLocation >
     * @throws ScmInvalidArgumentException
     */
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
            case "sftp":
                scmDataLocationList.add( new ScmSftpDataLocation( siteName ) );
                break;
            default:
                Assert.fail( "dataSourceType not match: " + dataType );
            }
        }
        return scmDataLocationList;
    }

    /**
     * @descreption 准备工作区的DataLocation信息
     * @param siteList
     * @param ScmShardingType
     * @return List< ScmDataLocation >
     * @throws ScmInvalidArgumentException
     */
    public static List< ScmDataLocation > prepareWsDataLocation(
            List< SiteWrapper > siteList, ScmShardingType ScmShardingType )
            throws ScmInvalidArgumentException {
        if ( siteList.size() < 1 ) {
            throw new IllegalArgumentException(
                    "error, site num can't less than 1 ！" );
        }
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        for ( SiteWrapper site : siteList ) {
            String dataType = site.getDataType().toString();
            String siteName = site.getSiteName();
            switch ( dataType ) {
            case "sequoiadb":
                String domainName = TestSdbTools
                        .getDomainNames( site.getDataDsUrl() ).get( 0 );
                ScmSdbDataLocation scmSdbDataLocation = new ScmSdbDataLocation(
                        siteName, domainName );
                scmSdbDataLocation.setCsShardingType( ScmShardingType );
                if ( ScmShardingType != ScmShardingType.NONE ) {
                    scmSdbDataLocation.setClShardingType( ScmShardingType );
                }
                scmDataLocationList.add( scmSdbDataLocation );
                break;
            case "hbase":
                ScmHbaseDataLocation scmHbaseDataLocation = new ScmHbaseDataLocation(
                        siteName );
                scmHbaseDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmHbaseDataLocation );
                break;
            case "hdfs":
                ScmHdfsDataLocation scmHdfsDataLocation = new ScmHdfsDataLocation(
                        siteName );
                scmHdfsDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmHdfsDataLocation );
                break;
            case "ceph_s3":
                ScmCephS3DataLocation scmCephS3DataLocation = new ScmCephS3DataLocation(
                        siteName );
                scmCephS3DataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmCephS3DataLocation );
                break;
            case "ceph_swift":
                ScmCephSwiftDataLocation scmCephSwiftDataLocation = new ScmCephSwiftDataLocation(
                        siteName );
                scmCephSwiftDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmCephSwiftDataLocation );
                break;
            case "sftp":
                ScmSftpDataLocation scmSftpDataLocation = new ScmSftpDataLocation(
                        siteName );
                scmSftpDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmSftpDataLocation );
                break;
            default:
                Assert.fail( "dataSourceType not match: " + dataType );
            }
        }
        return scmDataLocationList;
    }

    /**
     * @descreption 校验工作区修改
     * @param session
     * @param wsName
     * @param expDataLocations
     * @return
     * @throws ScmException
     */
    public static void checkWsUpdate( ScmSession session, String wsName,
            List< ScmDataLocation > expDataLocations ) throws ScmException {
        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace( wsName,
                session );
        List< ScmDataLocation > actDataLocations = workspace.getDataLocations();
        for ( ScmDataLocation actDataLocation : actDataLocations ) {
            for ( ScmDataLocation expDataLocation : expDataLocations ) {
                if ( actDataLocation.getSiteName()
                        .equals( expDataLocation.getSiteName() ) ) {
                    Assert.assertEquals( actDataLocation, expDataLocation );
                }
            }
        }
    }

    /**
     * @descreption 工作区赋权
     * @param session
     * @param wsName
     * @return
     * @throws ScmException
     */
    public static void wsSetPriority( ScmSession session, String wsName )
            throws ScmException, InterruptedException {
        wsSetPriority( session, wsName, ScmPrivilegeType.ALL );
    }

    /**
     * @descreption 工作区赋权
     * @param session
     * @param wsName
     * @param privilege
     * @return
     * @throws ScmException
     */
    public static void wsSetPriority( ScmSession session, String wsName,
            ScmPrivilegeType privilege )
            throws ScmException, InterruptedException {

        ScmUser superuser = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        ScmResource rs = ScmResourceFactory.createWorkspaceResource( wsName );
        ScmFactory.Role.grantPrivilege( session,
                superuser.getRoles().iterator().next(), rs, privilege );

    }

    /**
     * @descreption 创建工作区
     * @param session
     * @param wsName
     * @param metaStr
     * @param dataStr
     * @return
     * @throws ScmException
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
                    + " -m \"" + metaStr + "\" -d \"" + dataStr + "\" --url \""
                    + TestScmBase.gateWayList.get( 0 ) + "/"
                    + ScmInfo.getRootSite().getSiteName().toLowerCase()
                    + "\" --user " + TestScmBase.scmUserName + " --password "
                    + TestScmBase.scmUserName + " --enable-directory";
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

    /**
     * @descreption 删除工作区
     * @param session
     * @param wsName
     * @return
     * @throws ScmException
     */
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
                checkWsCs( wsName, session );
                return;
            }
        }
        Assert.fail( "delete ws is not done in 15 seconds" );
    }

    /**
     * @descreption 工作区删除站点
     * @param ws
     * @param siteName
     * @return
     * @throws ScmException
     */
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
     * @descreption 工作区新增站点
     * @param ws
     * @param site
     * @return
     * @throws ScmException
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

    /**
     * @descreption 工作区新增站点
     * @param ws
     * @param dataLocation
     * @return
     * @throws ScmException
     */
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

    /**
     * @descreption 创建关闭目录的ws供s3功能使用
     * @param session
     * @param wsName
     * @return
     * @throws Exception
     */
    public static ScmWorkspace createS3WS( ScmSession session, String wsName )
            throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations(
                ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setEnableDirectory( false );
        conf.setName( wsName );
        return ScmWorkspaceUtil.createWS( session, conf );
    }

    // /**
    // * @descreption 创建工作区，指定开启标签检索
    // * @param session
    // * @param wsName
    // * @return
    // * @throws Exception
    // */
    // public static ScmWorkspace createWS( ScmSession session, String wsName,
    // boolean enableTagRetrieval ) throws Exception {
    // ScmWorkspaceConf conf = new ScmWorkspaceConf();
    // conf.setDataLocations(
    // ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
    // conf.setMetaLocation(
    // ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
    // conf.setEnableTagRetrieval( enableTagRetrieval );
    // conf.setName( wsName );
    // ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, conf );
    // ScmWorkspaceTagRetrievalStatus status = null;
    // long begin = System.currentTimeMillis();
    // while ( status != ScmWorkspaceTagRetrievalStatus.ENABLED ) {
    // Thread.sleep( 1000 );
    // status = ws.getTagRetrievalStatus();
    // // 1分钟超时
    // if ( System.currentTimeMillis() - begin > 60000 ) {
    // Assert.fail( "wait ws TagRetrievalStatus timeout" );
    // }
    // }
    // return ws;
    // }

    /**
     * @descreption 根据站点获取DataLocationList
     * @param siteNum
     * @param dataLocationShardingType
     * @return
     * @throws Exception
     */
    public static List< ScmDataLocation > getDataLocationList( int siteNum,
            ScmShardingType ScmShardingType )
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
        for ( SiteWrapper site : siteList ) {
            String siteName = site.getSiteName();
            ScmType.DatasourceType dataType = site.getDataType();
            switch ( dataType ) {
            case SEQUOIADB:
                String domainName;
                List< String > domainNames = TestSdbTools
                        .getDomainNames( site.getDataDsUrl() );
                // 与元数据Domain分开
                if ( domainNames.size() >= 2 ) {
                    domainName = domainNames.get( 1 );
                } else {
                    domainName = domainNames.get( 0 );
                }
                ScmSdbDataLocation scmSdbDataLocation = new ScmSdbDataLocation(
                        siteName, domainName );
                scmSdbDataLocation.setCsShardingType( ScmShardingType );
                if ( ScmShardingType != ScmShardingType.NONE ) {
                    scmSdbDataLocation.setClShardingType( ScmShardingType );
                }
                scmDataLocationList.add( scmSdbDataLocation );
                break;
            case HBASE:
                ScmHbaseDataLocation scmHbaseDataLocation = new ScmHbaseDataLocation(
                        siteName );
                scmHbaseDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmHbaseDataLocation );
                break;
            case HDFS:
                ScmHdfsDataLocation scmHdfsDataLocation = new ScmHdfsDataLocation(
                        siteName );
                scmHdfsDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmHdfsDataLocation );
                break;
            case CEPH_S3:
                ScmCephS3DataLocation scmCephS3DataLocation = new ScmCephS3DataLocation(
                        siteName );
                scmCephS3DataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmCephS3DataLocation );
                break;
            case CEPH_SWIFT:
                ScmCephSwiftDataLocation scmCephSwiftDataLocation = new ScmCephSwiftDataLocation(
                        siteName );
                scmCephSwiftDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmCephSwiftDataLocation );
                break;
            case SFTP:
                ScmSftpDataLocation scmSftpDataLocation = new ScmSftpDataLocation(
                        siteName );
                scmSftpDataLocation.setShardingType( ScmShardingType );
                scmDataLocationList.add( scmSftpDataLocation );
                break;
            default:
                Assert.fail(
                        "dataSourceType not match: " + site.getDataType() );
            }
        }
        return scmDataLocationList;
    }

    /**
     * @descreption 校验工作区下CS
     * @param wsName
     * @param session
     * @return
     * @throws ScmException
     */
    public static void checkWsCs( String wsName, ScmSession session )
            throws ScmException {
        Sequoiadb rSdb = null;
        try {
            rSdb = new Sequoiadb( mainSdbUrl, sdbUserName, sdbPassword );
            // check workspace's cs
            String metaCSName = wsName + "_META";
            rSdb.getCollectionSpace( metaCSName );
            Assert.fail( "ws " + wsName
                    + " already deleted, meta cs should not exist" );
        } catch ( BaseException e ) {
            if ( e.getErrorCode() != -34 ) {
                throw e;
            }
        } finally {
            if ( rSdb != null ) {
                rSdb.close();
            }
        }
    }

    // /**
    // * @descreption 创建工作区打开tag检索
    // * @param session
    // * @param wsName
    // * @param enableTagRetrieval
    // * @param siteNum
    // * @return
    // * @throws ScmException
    // * @throws InterruptedException
    // */
    // public static ScmWorkspace createWS( ScmSession session, String wsName,
    // boolean enableTagRetrieval, int siteNum )
    // throws ScmException, InterruptedException {
    // ScmWorkspaceConf conf = new ScmWorkspaceConf();
    // conf.setDataLocations( getDataLocationList( siteNum ) );
    // conf.setMetaLocation( getMetaLocation( ScmShardingType.YEAR ) );
    // conf.setEnableTagRetrieval( enableTagRetrieval );
    // conf.setName( wsName );
    // return createWS( session, conf );
    // }
    //
    // /**
    // * @descreption 创建工作区打开tag检索并指定标签库domain
    // * @param session
    // * @param wsName
    // * @param siteNum
    // * @param domainName
    // * @return
    // * @throws ScmException
    // * @throws InterruptedException
    // */
    // public static ScmWorkspace createWS( ScmSession session, String wsName,
    // int siteNum, String domainName )
    // throws ScmException, InterruptedException {
    // ScmWorkspaceConf conf = new ScmWorkspaceConf();
    // conf.setDataLocations( getDataLocationList( siteNum ) );
    // conf.setMetaLocation( getMetaLocation( ScmShardingType.YEAR ) );
    // conf.setName( wsName );
    // conf.setEnableTagRetrieval( true );
    // conf.setTagLibMetaOption( new ScmTagLibMetaOption( domainName ) );
    // return createWS( session, conf );
    // }
}
