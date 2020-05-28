package com.sequoiacm.rest.serial;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONException;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephSwiftDataLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHdfsDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description CreateWorkspace2179.java 更新workspace
 * @author luweikang
 * @date 2018年5月24日
 */
public class UpdateWorkspace2180 extends TestScmBase {

    private static SiteWrapper site = null;
    private static SiteWrapper branchSite1 = null;
    private static SiteWrapper branchSite2 = null;
    private ScmSession session = null;
    private String wsName = "ws2180";
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {

        site = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = ScmInfo.getBranchSites( 2 );
        branchSite1 = siteList.get( 0 );
        branchSite2 = siteList.get( 1 );
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        int siteNum = ScmInfo.getSiteNum();
        ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        wsRemoveSite();
        wsAddAndRemoveSite();
        wsAddSite();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmWorkspaceUtil.deleteWs( wsName, session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    /**
     * @param siteNum
     * @return
     * @throws ScmInvalidArgumentException
     * @throws JSONException
     */
    private BSONObject siteBson( SiteWrapper site )
            throws ScmInvalidArgumentException, JSONException {

        String siteName = site.getSiteName();
        String dataType = site.getDataType().toString();
        switch ( dataType ) {
        case "sequoiadb":
            String domainName = TestSdbTools
                    .getDomainNames( site.getDataDsUrl() ).get( 0 );
            return new ScmSdbDataLocation( siteName, domainName )
                    .getBSONObject();
        case "hbase":
            return new ScmHbaseDataLocation( siteName ).getBSONObject();
        case "hdfs":
            return new ScmHdfsDataLocation( siteName ).getBSONObject();
        case "ceph_s3":
            return new ScmCephS3DataLocation( siteName ).getBSONObject();
        case "ceph_swift":
            return new ScmCephSwiftDataLocation( siteName ).getBSONObject();
        default:
            Assert.fail( "dataSourceType not match: " + dataType );
        }

        return null;
    }

    /**
     * @throws Exception
     *
     */
    private void wsRemoveSite() throws Exception {
        BSONObject updator = new BasicBSONObject();
        updator.put( "rmove_data_location", branchSite1.getSiteName() );

        System.out.println( updator );

        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        String response = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/workspaces/" + wsName )
                .setParameter( "updator", updator.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        BSONObject obj = ( BSONObject ) JSON.parse( response );
        System.out.println( obj );
        rest.disconnect();
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            ScmWorkspace updateWs = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            List< ScmDataLocation > dataList = updateWs.getDataLocations();
            List< String > nameList = new ArrayList<>();
            for ( ScmDataLocation data : dataList ) {
                nameList.add( data.getSiteName() );
            }
            for ( int j = 0; j < dataList.size(); j++ ) {
                if ( !nameList.contains( branchSite1.getSiteName() ) ) {
                    Assert.assertEquals( updateWs.getDataLocations().size(),
                            ScmInfo.getSiteNum() - 1, obj.toString() );
                    System.out.println( updateWs.getDataLocations().size() );
                    return;
                }
            }
            if ( i == 14 )
                Assert.fail( "remove site is not done in 15 seconds: "
                        + obj.toString() );
        }
    }

    /**
     * @throws Exception
     *
     */
    private void wsAddSite() throws Exception {
        BSONObject siteBson = siteBson( branchSite2 );
        BSONObject updator = new BasicBSONObject();
        updator.put( "add_data_location", siteBson );
        System.out.println( updator );
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        String response = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/workspaces/" + wsName )
                .setParameter( "updator", updator.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        BSONObject obj = ( BSONObject ) JSON.parse( response );
        rest.disconnect();
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            ScmWorkspace updateWs = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            List< ScmDataLocation > dataList = updateWs.getDataLocations();
            List< String > nameList = new ArrayList<>();
            for ( ScmDataLocation data : dataList ) {
                nameList.add( data.getSiteName() );
            }
            for ( int j = 0; j < dataList.size(); j++ ) {

                if ( nameList.contains( branchSite2.getSiteName() ) ) {
                    Assert.assertEquals( updateWs.getDataLocations().size(),
                            ScmInfo.getSiteNum(), obj.toString() );
                    System.out.println( updateWs.getDataLocations().size() );
                    return;
                }
            }
            if ( i == 14 )
                Assert.fail( "add site is not done in 15 seconds: "
                        + obj.toString() );
        }

    }

    /**
     * @throws Exception
     *
     */
    private void wsAddAndRemoveSite() throws Exception {
        BSONObject siteBson = siteBson( branchSite1 );
        BSONObject updator = new BasicBSONObject();
        updator.put( "rmove_data_location", branchSite2.getSiteName() );
        updator.put( "add_data_location", siteBson );
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        String response = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/workspaces/" + wsName )
                .setParameter( "updator", updator.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        BSONObject obj = ( BSONObject ) JSON.parse( response );
        rest.disconnect();
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            ScmWorkspace updateWs = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            List< ScmDataLocation > dataList = updateWs.getDataLocations();
            List< String > nameList = new ArrayList<>();
            for ( ScmDataLocation data : dataList ) {
                nameList.add( data.getSiteName() );
            }
            for ( int j = 0; j < dataList.size(); j++ ) {

                if ( nameList.contains( branchSite1.getSiteName() ) ) {
                    return;
                }
            }
            if ( i == 14 )
                Assert.fail( "add site is not done in 15 seconds: "
                        + obj.toString() );
        }
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            ScmWorkspace updateWs = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            List< ScmDataLocation > dataList = updateWs.getDataLocations();
            List< String > nameList = new ArrayList<>();
            for ( ScmDataLocation data : dataList ) {
                nameList.add( data.getSiteName() );
            }
            for ( int j = 0; j < dataList.size(); j++ ) {

                if ( !nameList.contains( branchSite2.getSiteName() ) ) {
                    System.out.println( "------------------------------------"
                            + ws.getDataLocations().size() );
                    Assert.assertEquals( updateWs.getDataLocations().size(),
                            ScmInfo.getSiteNum() - 1, obj.toString() );
                    return;
                }
            }
            if ( i == 14 )
                Assert.fail( "remove site is not done in 15 seconds: "
                        + obj.toString() );
        }
    }

}
