package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;

/**
 * @descreption SCM-3923:创建断点文件，以输入流方式上传数据 SCM-1368:创建断点文件，以输入流方式上传数据
 * @author YiPan
 * @date 2021/10/28
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3923_1368 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3923";
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private boolean runSuccess = false;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        wsp = ScmInfo.getWs();
        site = sites.get( new Random().nextInt( sites.size() ) );
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
    }

    @DataProvider(name = "dataProvider")
    public Object[] FileSize() {
        int m = 1024 * 1024;
        return new Object[] { m, m * 5, m * 16, m * 15 * 3, m * 30 + 1024,
                m * 50 };
    }

    @Test(groups = { "twoSite", "fourSite" }, dataProvider = "dataProvider")
    private void test( int fileSize ) throws Exception {
        ScmFileUtils.cleanFile( wsp, queryCond );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        TestTools.LocalFile.removeFile( downloadPath );
        BreakpointUtil.createFile( filePath, fileSize );

        // 创建断点文件
        ScmBreakpointFile breakpointFile = createBreakpointFile();
        // 创建scm文件,并将创建的断点文件作为文件的内容
        ScmFile file = breakpointFile2ScmFile( breakpointFile );
        // 校验断点文件md5和文件属性
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        checkFileAttributes( file, fileSize );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile()
            throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        InputStream inputStream = new FileInputStream( filePath );
        breakpointFile.upload( inputStream );
        inputStream.close();
        return breakpointFile;

    }

    private ScmFile breakpointFile2ScmFile( ScmBreakpointFile breakpointFile )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileId = file.save();
        return file;
    }

    private void checkFileAttributes( ScmFile file, int fileSize ) {
        Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
        Assert.assertEquals( file.getFileId(), fileId );

        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getAuthor(), "" );
        Assert.assertEquals( file.getTitle(), fileName );
        Assert.assertEquals( file.getSize(), fileSize );

        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), 1 );

        Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
        Assert.assertNotNull( file.getCreateTime().getTime() );
    }
}
