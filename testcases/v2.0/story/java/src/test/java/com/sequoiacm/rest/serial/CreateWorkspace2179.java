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

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephSwiftDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHdfsDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description CreateWorkspace2179.java 创建workspace
 * @author luweikang
 * @date 2018年5月24日
 */
public class CreateWorkspace2179 extends TestScmBase {

    private static SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws2179";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        int siteNum = ScmInfo.getSiteNum();
        createWorkspace( wsName, siteNum );

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

    private void createWorkspace( String wsName, int siteNum )
            throws Exception {
        BSONObject dataJson = dataJson( siteNum );
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        String response = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "/workspaces/" + wsName )
                .setParameter( "workspace_conf", dataJson )
                .setResponseType( String.class ).exec().getBody().toString();
        BSONObject obj = ( BSONObject ) JSON.parse( response );
        rest.disconnect();
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
            if ( i == 14 )
                Assert.fail( "create ws is not done in 15 seconds: "
                        + obj.toString() );
        }

        Assert.assertEquals( ws.getDataLocations().size(),
                ScmInfo.getSiteNum() );

    }

    /**
     * @param siteNum
     * @return
     * @throws ScmInvalidArgumentException
     * @throws
     */
    private BSONObject dataJson( int siteNum )
            throws ScmInvalidArgumentException {

        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
        List< BSONObject > scmDataLocationList = new ArrayList< BSONObject >();
        ScmMetaLocation scmMetaLocation = null;
        scmMetaLocation = new ScmSdbMetaLocation( rootSite.getSiteName(),
                ScmShardingType.YEAR, TestSdbTools
                        .getDomainNames( rootSite.getMetaDsUrl() ).get( 0 ) );

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
                        .add( new ScmSdbDataLocation( siteName, domainName )
                                .getBSONObject() );
                break;
            case "hbase":
                scmDataLocationList.add(
                        new ScmHbaseDataLocation( siteName ).getBSONObject() );
                break;
            case "hdfs":
                scmDataLocationList.add(
                        new ScmHdfsDataLocation( siteName ).getBSONObject() );
                break;
            case "ceph_s3":
                scmDataLocationList.add(
                        new ScmCephS3DataLocation( siteName ).getBSONObject() );
                break;
            case "ceph_swift":
                scmDataLocationList
                        .add( new ScmCephSwiftDataLocation( siteName )
                                .getBSONObject() );
                break;
            default:
                Assert.fail( "dataSourceType not match: " + dataType );
            }
        }

        BSONObject dataBson = new BasicBSONObject();
        dataBson.put( "data_location", scmDataLocationList );
        dataBson.put( "meta_location", scmMetaLocation.getBSONObject() );
        return dataBson;
    }

}
