package com.sequoiacm.scmfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2675 :: 不存在scm文件，使用流和文件路径覆盖文件
 * @author fanyu
 * @Date:2019年10月24日
 * @version:1.0
 */
public class OverWriteFile2675 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String[] fileNames = { "file2675A", "file2675B" };
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private File localPath;
    private int fileSize = 1024 * new Random().nextInt( 1024 );
    private String filePath;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).in( fileNames ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { GroupTags.base }) // over write file by stream
    private void test1() throws Exception {
        ScmFile scmFile = ScmFactory.File.createInstance( ws );
        scmFile.setFileName( fileNames[ 0 ] );
        scmFile.setAuthor( fileNames[ 0 ] );
        scmFile.setContent( new FileInputStream( new File( filePath ) ) );
        ScmId fileId = scmFile.save( new ScmUploadConf( true ) );
        fileIdList.add( fileId );
        // check file
        checkFile( fileId, fileNames[ 0 ] );
        runSuccess = true;
    }

    @Test // over write file by filePath
    private void test2() throws Exception {
        ScmFile scmFile = ScmFactory.File.createInstance( ws );
        scmFile.setFileName( fileNames[ 1 ] );
        scmFile.setAuthor( fileNames[ 1 ] );
        scmFile.setContent( filePath );
        ScmId fileId = scmFile.save( new ScmUploadConf( true ) );
        fileIdList.add( fileId );
        // check file
        checkFile( fileId, fileNames[ 1 ] );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFile( ScmId fileId, String fileName ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileName(), fileName );
            Assert.assertEquals( file.getAuthor(), fileName );
            Assert.assertEquals( file.getTitle(), "" );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getTags().toSet().size(), 0 );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            Assert.assertNull( file.getBatchId() );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            // check content
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( filePath ) );
        } catch ( AssertionError e ) {
            throw new Exception( "scmFile = " + file.toString(), e );
        }
    }
}
