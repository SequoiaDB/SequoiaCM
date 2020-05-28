package com.sequoiacm.multids;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
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
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.dsutils.CephSwiftUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1082:通过AwsS3驱动写缓存文件，ceph数据源Object已存在
 * @author huangxiaoni init
 * @date 2018.1.22
 */

public class Ceph_writeFile_cephObjectExist1082 extends TestScmBase {
    private static SiteWrapper rootSite = null;
    private static SiteWrapper cephSite = null;
    private static DatasourceType cephDsType = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int fileSize = 200 * 1024;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private String fileName = "scm1082";
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        try {
            rootSite = ScmInfo.getRootSite();
            int siteNum = ScmInfo.getSiteNum();
            List< SiteWrapper > branSites = ScmInfo
                    .getBranchSites( siteNum - 1 );
            for ( SiteWrapper branSite : branSites ) {
                DatasourceType dsType = branSite.getDataType();
                if ( dsType.equals( DatasourceType.CEPH_S3 )
                        || dsType.equals( DatasourceType.CEPH_SWIFT ) ) {
                    cephSite = branSite;
                    cephDsType = dsType;
                    break;
                }
            }

            if ( null == cephSite ) {
                throw new SkipException( "Not ceph env, skip." );
            }

            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( rootSite );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            fileId = ScmFileUtils.create( ws, fileName, filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // object exist in the ceph
        if ( cephDsType.equals( DatasourceType.CEPH_S3 ) ) {
            CephS3Utils.putObject( cephSite, wsp, fileId, filePath );
        } else if ( cephDsType.equals( DatasourceType.CEPH_SWIFT ) ) {
            CephSwiftUtils.createObject( cephSite, wsp, fileId, filePath );
        }

        // operBiz in the SCM
        this.readScmFile( cephSite );

        // remove rootSite's file
        TestSdbTools.Lob.removeLob( rootSite, wsp, fileId );
        this.readScmFile( cephSite );

        // check result
        SiteWrapper[] expSites = { rootSite, cephSite };
        ScmFileUtils.checkMeta( ws, fileId, expSites );

        runSuccess = true;
    }

    private void readScmFile( SiteWrapper site ) {
        ScmSession ss = null;
        try {
            ss = TestScmTools.createSession( site );
            ScmWorkspace wss = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    ss );

            ScmFile file = ScmFactory.File.getInstance( wss, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != ss ) {
                ss.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}