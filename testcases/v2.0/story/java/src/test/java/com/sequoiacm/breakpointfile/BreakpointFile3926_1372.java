package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.core.*;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-3926:空文件执行续传 SCM-1372:空文件执行续传
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3926_1372 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private static final String fileBaseName = "file3926_";
    private static final String fileAuthor = "author3926";
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private BSONObject query = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile" + ".txt";
        downloadPath = localPath + File.separator + "downloadFile" + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        query = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, query );

    }

    @DataProvider(name = "dataProvider")
    public Object[][] FileSize() {
        int m = 1024 * 1024;
        return new Object[][] { { fileBaseName + "0m", 0 },
                { fileBaseName + "3m", m * 3 },
                { fileBaseName + "15m", m * 15 } };
    }

    @Test(groups = { "twoSite", "fourSite" }, dataProvider = "dataProvider")
    private void test( String filename, int continueSize ) throws Exception {
        preparaEnv( continueSize );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, filename );
        // 上传空文件
        breakpointFile.incrementalUpload(
                ( new ByteArrayInputStream( new byte[ 0 ] ) ), false );

        // 续传文件
        ScmFile file = continueUpload( breakpointFile, filename );

        // 校验md5和文件属性
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        checkFileAttributes( file, filename, continueSize );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, query );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileAttributes( ScmFile file, String fileName,
            int fileSize ) {
        Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getAuthor(), fileAuthor );
        Assert.assertEquals( file.getTitle(), fileName );
        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), 1 );
        Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
        Assert.assertNotNull( file.getCreateTime().getTime() );
    }

    private ScmFile continueUpload( ScmBreakpointFile breakpointFile,
            String fileName ) throws ScmException {
        breakpointFile.upload( new File( filePath ) );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setAuthor( fileAuthor );
        file.setTitle( fileName );
        file.setFileName( fileName );
        ScmId fileId = file.save();
        fileIds.add( fileId );
        return file;
    }

    private void preparaEnv( int fileSize ) throws IOException {
        TestTools.LocalFile.removeFile( filePath );
        TestTools.LocalFile.removeFile( downloadPath );
        TestTools.LocalFile.createFile( filePath, fileSize );
    }
}