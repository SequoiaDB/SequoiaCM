package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-2258:setClOptions参数校验
 * @author fanyu
 * @date 2018年09月26日
 */
public class Param_setClOptions2258 extends TestScmBase {

    private String wsName1 = "ws2258_1";
    private String wsName2 = "ws2258_2";
    private ScmSession session = null;
    private SiteWrapper site = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName1, session );
        ScmWorkspaceUtil.deleteWs( wsName2, session );
    }

    @Test
    private void testValueIsWrong()
            throws ScmException, InterruptedException {
        ScmSdbMetaLocation scmMetaLocation = new ScmSdbMetaLocation(
                site.getSiteName(), ScmShardingType.YEAR,
                TestSdbTools.getDomainNames( site.getMetaDsUrl() ).get( 0 ) );
        scmMetaLocation
                .setClOptions( new BasicBSONObject().append( "Partition", 3 ) );

        // create workspace
        try {
            createWS( session, wsName1, ScmInfo.getSiteNum(), scmMetaLocation );
            ScmWorkspaceUtil.wsSetPriority( session, wsName1 );

            // create file
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName1,
                    session );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( wsName1 );
            file.save();
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METASOURCE_ERROR ) {
                throw e;
            }
        }
    }

    @Test
    private void testKeyIsWrong()
            throws ScmException, InterruptedException {
        ScmSdbMetaLocation scmMetaLocation = new ScmSdbMetaLocation(
                site.getSiteName(), ScmShardingType.YEAR,
                TestSdbTools.getDomainNames( site.getMetaDsUrl() ).get( 0 ) );
        scmMetaLocation.setClOptions(
                new BasicBSONObject().append( "Partition1", 16 ) );

        // create workspace
        try {
            createWS( session, wsName2, ScmInfo.getSiteNum(), scmMetaLocation );
            ScmWorkspaceUtil.wsSetPriority( session, wsName2 );

            // create file
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName2,
                    session );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( wsName2 );
            file.save();
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.METASOURCE_ERROR ) {
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsName1, session );
            ScmWorkspaceUtil.deleteWs( wsName2, session );
        }finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createWS( ScmSession session, String wsName, int siteNum,
            ScmMetaLocation scmMetaLocation )
            throws ScmException, InterruptedException {

        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
        List< ScmDataLocation > scmDataLocationList = new ArrayList< ScmDataLocation >();
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
        conf.setEnableDirectory(true);
    }
}
