package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;

/**
 * @Description SCM-4050:创建时不指定BreakponitFileType参数 ，断点续传文件
 * @Author zhangyanan
 * @Date 2021.11.5
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.11.5
 * @version 1.00
 */

public class BreakpointFile4050 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String uploadFileName = "uploadBreakpointfile4050_";
    private String uploadFailedFileName = "uploadFailedBreakpointfile4050_";
    private File localPath = null;
    private boolean runSuccessCount = false;
    private int[] cuntinueUploadFileSizeList = { 1024 * 1024 * 5,
            1024 * 1024 * 10 };
    private int[] cuntinueUploadFailedFileSizeList = { 1024 * 1024 * 3,
            1024 * 1024 * 12 };
    private int cuntinueUploadNum = 3;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Exception {
        // 校验续传大小小于5M和非5M整倍数场景
        for ( int i = 0; i < cuntinueUploadFailedFileSizeList.length; i++ ) {
            ScmBreakpointFile breakpointFile1 = ScmFactory.BreakpointFile
                    .createInstance( ws, uploadFailedFileName + i );
            cuntinueUploadFailed( breakpointFile1,
                    cuntinueUploadFailedFileSizeList[ i ] );
            ScmFactory.BreakpointFile.deleteInstance( ws,
                    uploadFailedFileName + i );
        }
        // 校验续传大小等于5M和5M整倍数场景
        for ( int i = 0; i < cuntinueUploadFileSizeList.length; i++ ) {
            ScmBreakpointFile breakpointFile2 = ScmFactory.BreakpointFile
                    .createInstance( ws, uploadFileName + i );
            byte[] data = cuntinueUploadBreakpointFile( breakpointFile2,
                    cuntinueUploadFileSizeList[ i ] );
            checkFileData( data, uploadFileName + i );
        }
        runSuccessCount = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccessCount || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private void checkFileData( byte[] data, String fileName )
            throws Exception {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileId = file.save();

        // down file
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( data ) );
        TestTools.LocalFile.removeFile( downloadPath );
        ScmFactory.File.deleteInstance( ws, fileId, true );
    }

    private byte[] cuntinueUploadBreakpointFile(
            ScmBreakpointFile breakpointFile, int fileSize )
            throws ScmException, IOException {
        // create file
        byte[] data = new byte[ fileSize * cuntinueUploadNum ];
        new Random().nextBytes( data );
        int copyNo = 0;
        int uploadSize = fileSize;
        boolean isCompleted = false;
        for ( int i = 0; i < cuntinueUploadNum; i++ ) {
            byte[] datapart1 = new byte[ uploadSize ];
            System.arraycopy( data, copyNo, datapart1, 0, uploadSize );
            if ( i == cuntinueUploadNum - 1 ) {
                isCompleted = true;
            }
            breakpointFile.incrementalUpload(
                    new ByteArrayInputStream( datapart1 ), isCompleted );
            copyNo += fileSize;
        }

        // check file's attribute
        Assert.assertEquals( breakpointFile.getUploadSize(),
                fileSize * cuntinueUploadNum );
        Assert.assertEquals( breakpointFile.getWorkspace(), ws );
        Assert.assertEquals( breakpointFile.isCompleted(), true );
        return data;
    }

    private void cuntinueUploadFailed( ScmBreakpointFile breakpointFile,
            int fileSize ) throws ScmException {
        byte[] data = new byte[ fileSize * 2 ];
        new Random().nextBytes( data );
        // create file
        int copyNo = 0;
        int uploadSize = fileSize;
        byte[] datapart1 = new byte[ uploadSize ];
        System.arraycopy( data, copyNo, datapart1, 0, uploadSize );
        breakpointFile.incrementalUpload( new ByteArrayInputStream( datapart1 ),
                false );

        copyNo += fileSize;
        byte[] datapart2 = new byte[ uploadSize ];
        System.arraycopy( data, copyNo, datapart2, 0, uploadSize );
        try {
            breakpointFile.incrementalUpload(
                    new ByteArrayInputStream( datapart2 ), false );
            Assert.fail( "the upload shoould have failed but succeeded !" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.DATA_BREAKPOINT_WRITE_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
    }
}