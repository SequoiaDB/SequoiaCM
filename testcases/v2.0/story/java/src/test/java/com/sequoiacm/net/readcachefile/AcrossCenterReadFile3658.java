package com.sequoiacm.net.readcachefile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-3658:同时指定偏移读和跨站读不缓存
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3658 extends TestScmBase {
    private final int branSitesNum = 2;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePathA = null;
    private String filePathB = null;
    private String fileName = "file3648";
    private WsWrapper wsp = null;
    private List< SiteWrapper > branSites = null;
    private ScmSession branchSite1session;
    private ScmSession branchSite2session;
    private ScmWorkspace branchSite1Ws;
    private ScmWorkspace branchSite2Ws;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private ScmId fileIdA = null;
    private ScmId fileIdB = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePathA = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";
        filePathB = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePathA, fileSize );
        TestTools.LocalFile.createFile( filePathB, fileSize );

        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();
        branchSite1 = branSites.get( 0 );
        branchSite2 = branSites.get( 1 );
        branchSite1session = TestScmTools.createSession( branchSite1 );
        branchSite2session = TestScmTools.createSession( branchSite2 );
        branchSite1Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1session );
        branchSite2Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite2session );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // branchSite1创建文件
        fileIdA = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePathA );
        fileIdB = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePathB );

        // 跨站点读取A缓存至本地
        AcrossCenterReadFileUtils.readFile( branchSite2Ws, fileIdA, localPath );

        // branchSite2跨站点读A,同时指定偏移读和跨站点不缓存
        SiteWrapper[] expSites = { branchSite1, branchSite2 };
        String actInputStreamReadFile = AcrossCenterReadFileUtils
                .getInputStreamReadFile( branchSite2Ws, fileIdA, localPath,
                        CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE
                                | CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK );
        Assert.assertEquals( TestTools.getMD5( actInputStreamReadFile ),
                TestTools.getMD5( filePathA ) );
        ScmFileUtils.checkMeta( branchSite2Ws, fileIdA, expSites );

        // branchSite2跨站点读B,同时指定偏移读和跨站点不缓存
        try {
            AcrossCenterReadFileUtils.getInputStreamReadFile( branchSite2Ws,
                    fileIdB, localPath,
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE
                            | CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess ) {
            try {
                ScmFactory.File.deleteInstance( branchSite1Ws, fileIdA, true );
                ScmFactory.File.deleteInstance( branchSite1Ws, fileIdB, true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                branchSite1session.close();
                branchSite2session.close();
            }
        }
    }
}