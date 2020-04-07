/**
 *
 */
package com.sequoiacm.workspace.serial;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephSwiftDataLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHdfsDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description CreateWorksPace1937.java 使用ws不包含的站点创建ws
 * @author luweikang
 * @date 2018年7月5日
 */
public class CreateWorksPace1937 extends TestScmBase {
    private String wsName = "ws1937";
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite1 = null;
    private SiteWrapper branSite2 = null;
    private String metaDomainName = null;
    private String rootDomainName = null;
    private List< ScmDataLocation > scmDataLocationList = new ArrayList<
            ScmDataLocation >();
    private ScmSdbMetaLocation scmMetaLocation = null;

    @BeforeClass
    private void setUp() throws ScmException {

        rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = ScmInfo.getBranchSites( 2 );
        branSite1 = siteList.get( 0 );
        branSite2 = siteList.get( 1 );
        session = TestScmTools.createSession( branSite2 );

        metaDomainName = TestSdbTools.getDomainNames( rootSite.getMetaDsUrl() )
                .get( 0 );
        rootDomainName = TestSdbTools.getDomainNames( rootSite.getDataDsUrl() )
                .get( 0 );

        scmDataLocationList.add( new ScmSdbDataLocation( rootSite.getSiteName(),
                rootDomainName ) );
        scmDataLocationList.add( dataLocation( branSite1 ) );
        scmMetaLocation = new ScmSdbMetaLocation( rootSite.getSiteName(),
                ScmShardingType.YEAR, metaDomainName );

    }

    @Test(groups = { "fourSite" })
    private void test() throws ScmException, InterruptedException {

        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( scmDataLocationList );
        conf.setMetaLocation( scmMetaLocation );
        conf.setName( wsName );
        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace( session, conf );
        scmMetaLocation = ( ScmSdbMetaLocation ) ws.getMetaLocation();
        ScmSession session1 = TestScmTools.createSession( branSite1 );

        ScmWorkspaceUtil.wsSetPriority( session1, wsName );
        ScmId fileId = null;
        try {
            ws = ScmFactory.Workspace.getWorkspace( wsName, session1 );

            ScmFile file = ScmFactory.File.createInstance( ws );
            byte[] test = new byte[ 1024 ];
            new Random().nextBytes( test );
            file.setFileName( "file1937" );
            file.setContent( new ByteArrayInputStream( test ) );
            fileId = file.save();

            Thread.sleep( 1000 );

            ScmFactory.File.deleteInstance( ws, fileId, true );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            session1.close();
        }
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmDataLocation dataLocation( SiteWrapper site )
            throws ScmInvalidArgumentException {
        ScmDataLocation data = null;
        String siteName = site.getSiteName();
        switch ( site.getDataType().toString() ) {
        case "sequoiadb":
            String domainName = TestSdbTools
                    .getDomainNames( site.getDataDsUrl() ).get( 0 );
            data = new ScmSdbDataLocation( siteName, domainName );
            break;
        case "hbase":
            data = new ScmHbaseDataLocation( siteName );
            break;
        case "hdfs":
            data = new ScmHdfsDataLocation( siteName );
            break;
        case "ceph_s3":
            data = new ScmCephS3DataLocation( siteName );
            break;
        case "ceph_swift":
            data = new ScmCephSwiftDataLocation( siteName );
            break;
        default:
            Assert.fail( "dataSourceType not match: " +
                    site.getDataType().toString() );
        }
        return data;
    }
}
