package com.sequoiacm.s3.object;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @descreption SCM-4282 :: 创建SCM文件，文件名包含特殊字符
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4282 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws_test_A = null;
    private ScmWorkspace ws_test_B = null;
    private boolean runSuccess = false;
    private String key = "object4282";
    private File localPath = null;
    private String filePath = null;
    private ScmId fileAId;
    private ScmId fileCId;
    private int fileSize = 1024;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );

        // 开启目录工作区
        WsWrapper wsp = ScmInfo.getWs();
        ws_test_A = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        Assert.assertTrue( ws_test_A.isEnableDirectory(),
                ws_test_A.toString() );
        // 未开启目录工作区
        ws_test_B = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        Assert.assertFalse( ws_test_B.isEnableDirectory(),
                ws_test_B.toString() );
    }

    @Test
    public void test() throws ScmException, IOException {
        // "\%;:*?"<>|" will success if directory is enable
        ScmFile fileA = ScmFactory.File.createInstance( ws_test_A );
        fileA.setFileName( key + "\\%;:*?\"<>|" );
        fileA.setContent( filePath );
        fileAId = fileA.save();

        // '/' failed if directory is enable
        try {
            ScmFile fileB = ScmFactory.File.createInstance( ws_test_A );
            fileB.setFileName( key + "/" );
            fileB.setContent( filePath );
            fileB.save();
            Assert.fail( "'/' is invalid in file name if directory is enable" );
        } catch ( ScmException e ) {
            Assert.assertEquals( "Invalid argument",
                    e.getError().getErrorDescription() );
        }

        // "/" success if directory is not enable
        ScmFile fileC = ScmFactory.File.createInstance( ws_test_B );
        fileC.setFileName( key + "/" );
        fileC.setContent( filePath );
        fileCId = fileC.save();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws_test_A, fileAId, true );
                ScmFactory.File.deleteInstance( ws_test_B, fileCId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
