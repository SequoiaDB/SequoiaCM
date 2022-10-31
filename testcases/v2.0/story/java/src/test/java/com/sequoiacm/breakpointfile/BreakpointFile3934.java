package com.sequoiacm.breakpointfile;

import java.io.*;
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

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-3934:续传文件大小大于cephS3默认分片大小
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3934 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "file3934";
    private static final int m = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private boolean runSuccess = false;
    private BSONObject query = null;

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
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        query = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
    }

    @DataProvider(name = "dataProvider")
    public Object[] FileSize() {
        return new Object[] { m * 5 * 3 + m, m * 5 * 12 };
    }

    @Test(groups = { "twoSite", "fourSite" }, dataProvider = "dataProvider")
    private void test( int fileSize ) throws Exception {
        TestTools.LocalFile.removeFile( downloadPath );
        ScmFileUtils.cleanFile( wsp, query );

        // 使用byte数组创建localFile
        byte[] b = createFileByByteArray( fileSize );

        // 创建断点文件
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        // 按默认分片多次上传
        uploadBreakPointFile( breakpointFile, b );

        // 转换为SCM文件校验MD5
        ScmFile file = createFileByBreakPointFile( breakpointFile );
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
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

    private byte[] subByte( byte[] b, int begin, int length ) {
        byte[] subByte = new byte[ length ];
        System.arraycopy( b, begin, subByte, 0, length );
        return subByte;
    }

    private ScmFile createFileByBreakPointFile(
            ScmBreakpointFile breakpointFile ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.save();
        return file;
    }

    private void uploadBreakPointFile( ScmBreakpointFile breakpointFile,
            byte[] b ) throws ScmException {
        // 获取上传分片为5m的次数
        int uploadTimes = b.length / ( m * 5 );
        // 获取最后一片的size
        int lastUploadSize = b.length % ( m * 5 );

        for ( int i = 0; i < uploadTimes; i++ ) {
            boolean isLastUpload = false;
            if ( i == uploadTimes - 1 && lastUploadSize == 0 ) {
                isLastUpload = true;
            }
            breakpointFile.incrementalUpload(
                    new ByteArrayInputStream( subByte( b, m * 5 * i, m * 5 ) ),
                    isLastUpload );
        }
        if ( lastUploadSize != 0 ) {
            breakpointFile.incrementalUpload(
                    new ByteArrayInputStream(
                            subByte( b, m * 5 * uploadTimes, lastUploadSize ) ),
                    true );
        }
    }

    private byte[] createFileByByteArray( int fileSize ) throws IOException {
        TestTools.LocalFile.removeFile( filePath );
        byte[] b = new byte[ fileSize ];
        new Random().nextBytes( b );
        FileOutputStream fileOutputStream = new FileOutputStream( filePath );
        fileOutputStream.write( b );
        return b;
    }
}