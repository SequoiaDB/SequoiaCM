package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiadb.exception.BaseException;

/**
 * test content:get workspace dataSrcinfo testlink-case:SCM-1828
 *
 * @author wuyan
 * @Date 2018.06.21
 * @version 1.00
 */
public class GetWorkSpaceInfo1828 extends TestScmBase {
    private static SiteWrapper site = null;
    private boolean runSuccess = false;
    private ScmSession session = null;
    private String wsName = "ws1828";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException {
        createAndGetWorkSpace();
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createAndGetWorkSpace() throws ScmException {
        // create ws
        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
        List< ScmDataLocation > scmDataLocationList = new ArrayList< ScmDataLocation >();
        ScmMetaLocation scmMetaLocation = new ScmSdbMetaLocation(
                rootSite.getSiteName(), ScmShardingType.YEAR, TestSdbTools
                        .getDomainNames( rootSite.getMetaDsUrl() ).get( 0 ) );

        siteList = ScmInfo.getAllSites();
        List< String > siteNames = new ArrayList< String >();
        List< String > domainNames = new ArrayList< String >();
        for ( int i = 0; i < siteList.size(); i++ ) {
            String siteName = siteList.get( i ).getSiteName();
            DatasourceType dataType = siteList.get( i ).getDataType();
            if ( dataType.equals( DatasourceType.SEQUOIADB ) ) {
                String domainName = TestSdbTools
                        .getDomainNames( siteList.get( i ).getDataDsUrl() )
                        .get( 0 );
                siteNames.add( siteName );
                domainNames.add( domainName );
                scmDataLocationList
                        .add( new ScmSdbDataLocation( siteName, domainName,
                                ScmShardingType.YEAR, ScmShardingType.MONTH ) );
                break;
            }
        }

        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( scmDataLocationList );
        conf.setMetaLocation( scmMetaLocation );
        conf.setName( wsName );
        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace( session, conf );

        // get ws and check the result
        List< ScmDataLocation > list = ws.getDataLocations();
        ScmSdbDataLocation scmSdbDataLocation = ( ScmSdbDataLocation ) list
                .get( 0 );
        Assert.assertEquals( scmSdbDataLocation.getDomainName(),
                domainNames.get( 0 ) );
        Assert.assertEquals( scmSdbDataLocation.getClShardingType(),
                ScmShardingType.MONTH );
        Assert.assertEquals( scmSdbDataLocation.getCsShardingType(),
                ScmShardingType.YEAR );
        Assert.assertEquals( scmSdbDataLocation.getType(),
                DatasourceType.SEQUOIADB );
        Assert.assertEquals( scmSdbDataLocation.getSiteName(),
                siteNames.get( 0 ) );
    }
}
