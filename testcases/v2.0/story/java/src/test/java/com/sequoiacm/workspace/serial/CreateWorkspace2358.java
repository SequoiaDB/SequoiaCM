package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.HbaseUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @Description SCM-2357:指定非默认的namespace,创建ws
 * @author fanyu
 * @date 2019年01月07日
 */
public class CreateWorkspace2358 extends TestScmBase {
    private String wsName = "ws2358";
    private String namespace = "default";
    private ScmSession session = null;
    private SiteWrapper site = null;
    private Calendar cal = null;

    private String fileName = "scmfile2358";
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        cal = Calendar.getInstance();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        for ( SiteWrapper tmpsite : sites ) {
            if ( tmpsite.getDataType()
                    .equals( ScmType.DatasourceType.HBASE ) ) {
                site = tmpsite;
                break;
            }
        }
        if ( site == null ) {
            throw new SkipException( "the site of hbase is not existed" );
        }
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // create and set ScmHbaseLocation
        ScmHbaseDataLocation scmHbaseDataLocation = new ScmHbaseDataLocation(
                site.getSiteName() );
        scmHbaseDataLocation.setNamespace( namespace );
        // create workspace
        createWS( session, wsName, ScmInfo.getSiteNum(), scmHbaseDataLocation );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        // write file in workspace
        writeAndRead( wsName );
        // only check tableName in namespace
        String tableName = null;
        if ( cal.get( Calendar.MONTH ) + 1 < 10 ) {
            tableName = wsName + "_SCMFILE_" + cal.get( Calendar.YEAR ) + "0"
                    + ( cal.get( Calendar.MONTH ) + 1 );
        } else {
            tableName = wsName + "_SCMFILE_" + cal.get( Calendar.YEAR )
                    + ( cal.get( Calendar.MONTH ) + 1 );
        }
        Assert.assertTrue( HbaseUtils.isInNS( site, namespace, tableName ),
                "expect tableName is in namespace,tableName = " + tableName );
    }

    @AfterClass
    private void tearDown() throws ScmException, IOException {
        ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        TestTools.LocalFile.removeFile( localPath );
        if ( session != null ) {
            session.close();
        }
    }

    private void writeAndRead( String wsName ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setCreateTime( Calendar.getInstance().getTime() );
        file.setContent( filePath );
        ScmId fileId = file.save();

        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file1.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
    }

    private ScmWorkspace createWS( ScmSession session, String wsName,
            int siteNum, ScmDataLocation scmDataLocation )
            throws ScmException, InterruptedException {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
        ScmMetaLocation scmMetaLocation = null;
        scmMetaLocation = new ScmSdbMetaLocation( rootSite.getSiteName(),
                ScmShardingType.YEAR, TestSdbTools
                        .getDomainNames( rootSite.getMetaDsUrl() ).get( 0 ) );
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
                scmDataLocationList.add( scmDataLocation );
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
