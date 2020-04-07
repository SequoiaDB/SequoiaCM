package com.sequoiacm.workspace.serial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

/**
 * @Description SCM-2257:创建ws指定元数据CS、CL的创建参数
 * @author fanyu
 * @date 2018年09月26日
 */
public class CreateWorkspace2257 extends TestScmBase {

    private String wsName = "ws2257";
    private ScmSession session = null;
    private SiteWrapper site = null;
    private Sequoiadb db = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        db = TestSdbTools.getSdb( site.getMetaDsUrl() );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        //create and set ScmSdbMetaLocation
        ScmSdbMetaLocation scmMetaLocation = new ScmSdbMetaLocation(
                site.getSiteName(), ScmShardingType.YEAR,
                TestSdbTools.getDomainNames( site.getMetaDsUrl() ).get( 0 ) );
        BSONObject csOpt = createCsOption();
        BSONObject clOpt = createClOption();

        //check getCsOptions() and getClOptions
        Assert.assertEquals( scmMetaLocation.getClOptions(), null );
        Assert.assertEquals( scmMetaLocation.getCsOptions(), null );

        //set CsOptions and ClOptions
        scmMetaLocation.setCsOptions( csOpt );
        scmMetaLocation.setClOptions( clOpt );

        //check getCsOptions() and getClOptions
        Assert.assertEquals( scmMetaLocation.getClOptions().toString(),
                clOpt.toString() );
        Assert.assertEquals( scmMetaLocation.getCsOptions().toString(),
                csOpt.toString() );

        // create workspace
        createWS( session, wsName, ScmInfo.getSiteNum(), scmMetaLocation );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        // write file in workspace
        write( wsName );

        // check
        BSONObject clBSON = snapshot( Sequoiadb.SDB_SNAP_CATALOG,
                "{\"Name\":\"" + wsName + "_META.FILE_" +
                        Calendar.getInstance().get( Calendar.YEAR ) + "\"}" )
                .getCurrent();
        BSONObject csBSON = snapshot( Sequoiadb.SDB_SNAP_COLLECTIONSPACES,
                "{\"Name\":\"" + wsName + "_META\"}" )
                .getCurrent();
        Assert.assertEquals( csBSON.get( "PageSize" ),
                csOpt.get( "PageSize" ) );
        Assert.assertEquals( csBSON.get( "LobPageSize" ),
                csOpt.get( "LobPageSize" ) );
        Assert.assertEquals( clBSON.get( "CompressionTypeDesc" ),
                clOpt.get( "CompressionType" ) );
        Assert.assertEquals( clBSON.get( "Partition" ),
                clOpt.get( "Partition" ) );
        Assert.assertEquals( clBSON.get( "AutoSplit" ),
                clOpt.get( "AutoSplit" ) );
        Assert.assertEquals( clBSON.get( "AttributeDesc" ),
                "Compressed | StrictDataMode" );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( db != null ) {
                db.close();
            }
        }
    }

    private DBCursor snapshot( int snapType, String matcher ) {
        DBCursor info = db.getSnapshot( snapType, matcher, null, null );
        return info;
    }

    private void write( String wsName ) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( wsName );
        file.setCreateTime( Calendar.getInstance().getTime() );
        file.save();
    }

    private BSONObject createCsOption() {
        BSONObject opt = new BasicBSONObject();
        opt.put( "PageSize", 8192 );
        opt.put( "LobPageSize", 4096 );
        return opt;
    }

    private BSONObject createClOption() {
        BSONObject opt = new BasicBSONObject();
        opt.put( "Partition", 16 );
        opt.put( "ReplSize", 3 );
        opt.put( "Compressed", true );
        opt.put( "CompressionType", "snappy" );
        opt.put( "AutoSplit", false );
        opt.put( "AutoIndexId", true );
        opt.put( "StrictDataMode", true );
        return opt;
    }

    private ScmWorkspace createWS( ScmSession session, String wsName,
            int siteNum, ScmMetaLocation scmMetaLocation )
            throws ScmException, InterruptedException {

        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
        List< ScmDataLocation > scmDataLocationList = new ArrayList<
                ScmDataLocation >();
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

        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( scmDataLocationList );
        conf.setMetaLocation( scmMetaLocation );
        conf.setName( wsName );
        ScmFactory.Workspace.createWorkspace( session, conf );

        ScmWorkspace ws = null;
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            try {
                ws = ScmFactory.Workspace.getWorkspace( wsName, session );
                break;
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                    throw e;
                }
            }
        }
        return ws;
    }
}
