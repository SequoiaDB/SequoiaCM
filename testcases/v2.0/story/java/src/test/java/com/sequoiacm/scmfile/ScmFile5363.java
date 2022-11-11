package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @descreption SCM-5363:断点文件转换为普通文件
 * @author ZhangYanan
 * @date 2022/11/02
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile5363 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file5363";
    private String breakpointFileName = "breakpointFile5363";
    private int fileSize = 1024 * 1024 * 10;
    private int partSize = 1024 * 1024 * 5;
    private String filePath = null;
    private File localPath = null;
    private ScmId fileID = null;
    private BSONObject queryCond = null;

    @BeforeClass()
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        String fileIdStr = ScmFileUtils.getFileIdByDate( new Date() );
        fileID = new ScmId( fileIdStr );
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // 使用byte数组创建localFile
        byte[] b = createFileByByteArray( fileSize );
        // 创建断点文件
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, breakpointFileName );
        // 按默认分片多次上传
        uploadBreakPointFile( breakpointFile, b );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setFileId( fileID );
        ScmId scmId = file.save();

        Assert.assertEquals( fileID, scmId,
                "文件id与指定的fileId不一致，指定的id为:" + fileID + " ;实际文件id为:" + scmId );

        SiteWrapper[] expSites = { rootSite };
        ScmFileUtils.checkMetaAndData( wsp, fileID, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass()
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

    private byte[] createFileByByteArray( int fileSize ) throws IOException {
        TestTools.LocalFile.removeFile( filePath );
        byte[] b = new byte[ fileSize ];
        new Random().nextBytes( b );
        FileOutputStream fileOutputStream = new FileOutputStream( filePath );
        fileOutputStream.write( b );
        return b;
    }

    private byte[] subByte( byte[] b, int begin, int length ) {
        byte[] subByte = new byte[ length ];
        System.arraycopy( b, begin, subByte, 0, length );
        return subByte;
    }

    private void uploadBreakPointFile( ScmBreakpointFile breakpointFile,
            byte[] b ) throws ScmException {
        // 获取上传分片为5m的次数
        int uploadTimes = b.length / ( partSize );
        // 获取最后一片的size
        int lastUploadSize = b.length % ( partSize );

        for ( int i = 0; i < uploadTimes; i++ ) {
            boolean isLastUpload = false;
            if ( i == uploadTimes - 1 && lastUploadSize == 0 ) {
                isLastUpload = true;
            }
            breakpointFile.incrementalUpload(
                    new ByteArrayInputStream(
                            subByte( b, partSize * i, partSize ) ),
                    isLastUpload );
        }
        if ( lastUploadSize != 0 ) {
            breakpointFile.incrementalUpload( new ByteArrayInputStream(
                    subByte( b, partSize * uploadTimes, lastUploadSize ) ),
                    true );
        }
    }
}