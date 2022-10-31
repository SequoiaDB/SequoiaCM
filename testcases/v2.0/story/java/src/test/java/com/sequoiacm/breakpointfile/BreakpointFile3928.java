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
import org.testng.annotations.Test;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-3928:多次分段续传，中间续传分段小于cephS3默认分片长度
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3928 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "file3928";
    private static final int m = 1024 * 1024;
    private byte[] buff = null;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private boolean runSuccess = false;
    private BSONObject query = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile" + ".txt";
        downloadPath = localPath + File.separator + "downloadFile" + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        buff = new byte[ m * 14 ];
        new Random().nextBytes( buff );
        FileOutputStream fileOutputStream = new FileOutputStream(
                new File( filePath ) );
        fileOutputStream.write( buff );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        query = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
    }

    // SEQUOIACM-748
    @Test(groups = { "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        // 创建断点文件
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );

        // 执行多次续传，分别为5m、6m、4m、5m
        breakpointFile.incrementalUpload(
                new ByteArrayInputStream( subByte( buff, 0, m * 5 ) ), false );
        breakpointFile.incrementalUpload(
                new ByteArrayInputStream( subByte( buff, m * 5, m * 6 ) ),
                false );
        breakpointFile.incrementalUpload(
                new ByteArrayInputStream( subByte( buff, m * 11, m * 4 ) ),
                false );
        breakpointFile.incrementalUpload(
                new ByteArrayInputStream( subByte( buff, m * 15, m * 5 ) ),
                true );

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
}