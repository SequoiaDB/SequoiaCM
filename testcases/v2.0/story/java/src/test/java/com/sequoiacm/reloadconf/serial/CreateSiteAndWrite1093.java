/**
 *
 */
package com.sequoiacm.reloadconf.serial;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmSiteUtils;

/**
 * @Description:SCM-1093:新增站点,刷新业务配置和写文件并发
 * @author fanyu
 * @Date:2018年1月31日
 * @version:1.0
 */
public class CreateSiteAndWrite1093 extends TestScmBase {
    private static String fileName = "ReloadConfAndWrite1093";
    private static SiteWrapper rootSite = null;
    private static WsWrapper wsp = null;
    private SiteWrapper branceSite = null;
    private String newSiteName = "test1";
    private List< ScmId > fileIdList = new CopyOnWriteArrayList< ScmId >();
    private int fileSize = 1024 * 1024 * 1;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( rootSite );
            ScmSiteUtils.deleteSite( session, newSiteName );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        Write w = new Write();
        ReloadConf r = new ReloadConf();
        r.start( 1 );
        w.start( 20 );
        Assert.assertEquals( w.isSuccess(), true, w.getErrorMsg() );
        Assert.assertEquals( r.isSuccess(), true, r.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            ScmSiteUtils.deleteSite( session, newSiteName );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class Write extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                for ( int i = 0; i < 2; i++ ) {
                    ScmFile file = ScmFactory.File.createInstance( ws );
                    file.setContent( filePath );
                    file.setFileName( fileName + "_" + UUID.randomUUID() );
                    ScmId fileId = file.save();
                    fileIdList.add( fileId );
                    SiteWrapper[] expSites = { branceSite };
                    ScmFileUtils.checkMetaAndData( wsp, fileId, expSites,
                            localPath, filePath );
                }
            } catch ( ScmException e ) {
                System.out.println( "Error : " + e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ReloadConf extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                ScmConfigOption scOpt = new ScmConfigOption(
                        TestScmBase.gateWayList.get( 0 ) + "/"
                                + rootSite.getSiteServiceName(),
                        TestScmBase.scmUserName, TestScmBase.scmPassword );
                session = ScmFactory.Session
                        .createSession( SessionType.NOT_AUTH_SESSION, scOpt );
                // TODO 暂时将创建新站点使用的数据源写死为192.168.28.104（mysql的数据源集群），待ci资源扩充后修改
                String dsUrl = "192.168.28.104:11810";
                String user = "sdbadmin";
                String passwdPath = "sdbadmin";
                ScmSiteUtils.createSite( session, newSiteName,
                        TestScmBase.gateWayList.get( 0 ), 1, dsUrl, user,
                        passwdPath );
                // if ( rootSite.getDataType()
                // .equals( DatasourceType.SEQUOIADB ) ) {
                // ScmSiteUtils.createSite( session, newSiteName,
                // TestScmBase.gateWayList.get( 0 ), 1,
                // rootSite.getDataDsUrl(), user, passwdPath );
                // } else if ( rootSite.getDataType()
                // .equals( DatasourceType.HBASE ) ) {
                // ScmSiteUtils.createSite( session, newSiteName,
                // TestScmBase.gateWayList.get( 0 ), 2,
                // rootSite.getDataDsUrl(), user, passwdPath );
                // } else if ( rootSite.getDataType()
                // .equals( DatasourceType.CEPH_S3 ) ) {
                // ScmSiteUtils.createSite( session, newSiteName,
                // TestScmBase.gateWayList.get( 0 ), 3,
                // rootSite.getDataDsUrl(), user, passwdPath );
                // } else if ( rootSite.getDataType()
                // .equals( DatasourceType.CEPH_SWIFT ) ) {
                // ScmSiteUtils.createSite( session, newSiteName,
                // TestScmBase.gateWayList.get( 0 ), 4,
                // rootSite.getDataDsUrl(), user, passwdPath );
                // } else {
                // throw new Exception(
                // "DatasourceType is not exist,please check"
                // + rootSite.getDataType() );
                // }
                ScmSiteUtils.deleteSite( session, newSiteName );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
