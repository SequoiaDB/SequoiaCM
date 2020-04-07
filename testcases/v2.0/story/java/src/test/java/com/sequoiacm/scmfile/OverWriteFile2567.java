package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;
import java.util.Random;

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
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2567:多次重复覆盖上传文件
 * @author fanyu
 * @Date:2019年8月21日
 * @version:1.0
 */
public class OverWriteFile2567 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String name = "2567";
    private ScmId fileId;
    private File localPath;
    private int fileSize = 1024 * new Random().nextInt( 1024 );
    private String filePath;
    private String updateFilePath;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        updateFilePath =
                localPath + File.separator + "localFile_" + ( fileSize + 1 ) +
                        ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updateFilePath, fileSize + 1 );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( name ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        prepareFile();
    }

    @Test
    private void test() throws Exception {
        //overwrite is true
        ScmFile scmFile = ScmFactory.File.createInstance( ws );
        scmFile.setFileName( name );
        scmFile.setContent( updateFilePath );
        String newVal = name + "-new";
        scmFile.setAuthor( newVal );
        scmFile.setTitle( newVal );
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( newVal );
        scmFile.setTags( scmTags );
        fileId = scmFile.save( new ScmUploadConf( true ) );

        //overwrite again
        ScmFile scmFile1 = ScmFactory.File.createInstance( ws );
        scmFile1.setFileName( name );
        scmFile1.setContent( filePath );
        String newVal1 = name + "-new";
        scmFile1.setAuthor( newVal1 );
        scmFile1.setTitle( newVal1 );
        ScmTags scmTags1 = new ScmTags();
        scmTags1.addTag( newVal1 );
        scmFile1.setTags( scmTags1 );
        fileId = scmFile1.save( new ScmUploadConf( true ) );
        //get scm file and directory and check
        ScmFile actFile = ScmFactory.File.getInstance( ws, fileId );
        checkFile( actFile, newVal1, fileSize, filePath, scmTags1 );
        //delete file
        ScmFactory.File.deleteInstance( ws, fileId, true );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareFile() throws ScmException {
        //create tags
        ScmTags scmTags = new ScmTags();
        scmTags.addTag( name );
        //create file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        file.setAuthor( name );
        file.setTitle( name );
        file.setTags( scmTags );
        file.setContent( filePath );
        fileId = file.save();
    }

    private void checkFile( ScmFile file, String expVal, int expSize,
            String expFilePath, ScmTags expScmTags ) throws Exception {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getFileName(), name );
            Assert.assertEquals( file.getAuthor(), expVal );
            Assert.assertEquals( file.getTitle(), expVal );
            Assert.assertEquals( file.getMimeType(), "text/plain" );
            Assert.assertEquals( file.getSize(), expSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getTags().toSet().toString(),
                    expScmTags.toSet().toString() );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            file.getContent( downloadPath );
            // check content
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( expFilePath ) );
        } catch ( AssertionError e ) {
            throw new Exception(
                    "fileName = " + file.getFileName() + "fileId = " +
                            fileId.get(), e );
        }
    }
}
