package com.sequoiacm.breakpointfile;

import java.io.*;
import java.util.Random;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;

/**
 * @Description SCM-4046:BreakpointFile.createInstance接口参数校验
 * @Author zhangyanan
 * @Date 2021.11.5
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.11.5
 * @version 1.00
 */

public class BreakpointFile4046 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName1 = "breakpointfile4046_1";
    private String fileName2 = "breakpointfile4046_2";
    private String fileName3 = "breakpointfile4046_3";
    private File localPath = null;
    private boolean runSuccessCount = false;
    private int fileSize = 1024 * 1024 * 3;
    private int[] fileUploadSizesList = { 1024 * 1024, 1024 * 1024 * 2 };
    private byte[] data;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        data = new byte[ fileSize ];
        new Random().nextBytes( data );
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Exception {
        // ScmType.BreakpointFileType.BUFFERED 上传成功
        ScmBreakpointFile breakpointFile1 = ScmFactory.BreakpointFile
                .createInstance( ws, fileName1, new ScmBreakpointFileOption(),
                        ScmType.BreakpointFileType.BUFFERED );
        byte[] data = uploadBreakpointFile( breakpointFile1 );
        checkFileData( data );

        // ScmType.BreakpointFileType.DIRECTED 上传失败
        ScmBreakpointFile breakpointFile2 = ScmFactory.BreakpointFile
                .createInstance( ws, fileName2, new ScmBreakpointFileOption(),
                        ScmType.BreakpointFileType.DIRECTED );
        theUploadFailed( breakpointFile2 );

        // 不指定BreakpointFileType上传失败
        ScmBreakpointFile breakpointFile3 = ScmFactory.BreakpointFile
                .createInstance( ws, fileName3 );
        theUploadFailed( breakpointFile3 );
        runSuccessCount = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccessCount || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName2 );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName3 );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private void checkFileData( byte[] data ) throws Exception {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName1 );
        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName1 );
        file.setTitle( fileName1 );
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

    private byte[] uploadBreakpointFile( ScmBreakpointFile breakpointFile )
            throws ScmException, IOException {
        int copyNo = 0;
        int uploadSize = fileUploadSizesList[ 0 ];
        byte[] datapart1 = new byte[ uploadSize ];
        System.arraycopy( data, copyNo, datapart1, 0, uploadSize );
        breakpointFile.incrementalUpload( new ByteArrayInputStream( datapart1 ),
                false );
        copyNo += fileUploadSizesList[ 0 ];
        uploadSize = fileUploadSizesList[ 1 ];
        byte[] datapart2 = new byte[ uploadSize ];
        System.arraycopy( data, copyNo, datapart2, 0, uploadSize );
        breakpointFile.incrementalUpload( new ByteArrayInputStream( datapart2 ),
                true );

        // check file's attribute
        Assert.assertEquals( breakpointFile.getUploadSize(), fileSize );
        Assert.assertEquals( breakpointFile.getWorkspace(), ws );
        Assert.assertEquals( breakpointFile.isCompleted(), true );
        return data;
    }

    private void theUploadFailed( ScmBreakpointFile breakpointFile )
            throws ScmException {
        int copyNo = 0;
        int uploadSize = fileUploadSizesList[ 0 ];
        byte[] datapart1 = new byte[ uploadSize ];
        System.arraycopy( data, copyNo, datapart1, 0, uploadSize );
        breakpointFile.incrementalUpload( new ByteArrayInputStream( datapart1 ),
                false );

        copyNo += fileUploadSizesList[ 0 ];
        uploadSize = fileUploadSizesList[ 1 ];
        byte[] datapart2 = new byte[ uploadSize ];
        System.arraycopy( data, copyNo, datapart2, 0, uploadSize );
        try {
            breakpointFile.incrementalUpload(
                    new ByteArrayInputStream( datapart1 ), false );
            Assert.fail( "the upload shoould have failed but succeeded !" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.DATA_BREAKPOINT_WRITE_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
    }

}