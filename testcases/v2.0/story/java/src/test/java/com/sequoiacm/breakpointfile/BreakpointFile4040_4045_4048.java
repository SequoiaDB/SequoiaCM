package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;

/**
 * @Description SCM-4040:创建可缓存断点文件，多次分段续传 SCM-4045:支持缓存断点文件续传空文件
 *              SCM-4048:多次分段续传，每段大小不一致
 * @Author zhangyanan
 * @Date 2021.11.5
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.11.5
 * @version 1.00
 */

public class BreakpointFile4040_4045_4048 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "breakpointfile4040_";
    private File localPath = null;
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );

    @BeforeClass
    private void setUp() throws ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBDataSource();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite,
            GroupTags.fourSite }, dataProvider = "dataProvider")
    private void test( int fileSize, int[] dataSizes, String fileName )
            throws Exception {
        // create breakpointfile
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, new ScmBreakpointFileOption(),
                        ScmType.BreakpointFileType.BUFFERED );
        byte[] data = uploadBreakpointFile( breakpointFile, fileSize,
                dataSizes );
        checkFileData( data, fileName );
        runSuccessCount.incrementAndGet();
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccessCount.get() == generateDate().length
                || TestScmBase.forceClear ) {
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

    private byte[] uploadBreakpointFile( ScmBreakpointFile breakpointFile,
            int fileSize, int[] dataSizes ) throws ScmException {
        byte[] data = new byte[ fileSize ];
        new Random().nextBytes( data );
        int copyNo = 0;
        for ( int i = 0; i < dataSizes.length; i++ ) {
            int uploadSize = dataSizes[ i ];
            byte[] datapart = new byte[ uploadSize ];
            if ( dataSizes[ i ] != 0 ) {
                System.arraycopy( data, copyNo, datapart, 0, uploadSize );
            }
            boolean isCompleted = false;
            if ( i == dataSizes.length - 1 ) {
                isCompleted = true;
            }
            breakpointFile.incrementalUpload(
                    new ByteArrayInputStream( datapart ), isCompleted );
            copyNo += dataSizes[ i ];
        }
        // check file's attribute
        Assert.assertEquals( breakpointFile.getUploadSize(), fileSize );
        Assert.assertEquals( breakpointFile.getChecksum(), 0 );
        Assert.assertTrue( breakpointFile.isCompleted() );
        return data;
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        // 续传数据大小小于5
        int fileSize1 = 1024 * 1024 * 9;
        int[] fileUploadSizesList1 = { 1024 * 1024, 1, 1024 * 1024 * 5 - 1,
                1024 * 1024 * 3 };
        String fileName1 = fileName + 1;
        // 续传数据大小等于5M
        int fileSize2 = 1024 * 1024 * 15;
        int[] fileUploadSizesList2 = { 1024 * 1024 * 5, 1024 * 1024 * 5,
                1024 * 1024 * 5 };
        String fileName2 = fileName + 2;
        // 续传数据大小为5M整数
        int fileSize3 = 1024 * 1024 * 45;
        int[] fileUploadSizesList3 = { 1024 * 1024 * 10, 1024 * 1024 * 15,
                1024 * 1024 * 20 };
        String fileName3 = fileName + 3;
        // 续传数据大小非为5M整倍数
        int fileSize4 = 1024 * 1024 * 40;
        int[] fileUploadSizesList4 = { 1024 * 1024 * 11, 1024 * 1024 * 16,
                1024 * 1024 * 13 };
        String fileName4 = fileName + 4;
        // SCM-4045:支持缓存断点文件续传空文件
        // 第一片数据为0M
        int fileSize5 = 1024 * 1024 * 12;
        int[] fileUploadSizesList5 = { 0, 1024 * 1024 * 9, 1024 * 1024 * 3 };
        String fileName5 = fileName + 5;
        // 中间续传0M数据
        int fileSize6 = 1024 * 1024 * 6;
        int[] fileUploadSizesList6 = { 1024 * 1024 * 3, 0, 1024 * 1024 * 3 };
        String fileName6 = fileName + 6;
        // 最后续传为0M
        int fileSize7 = 1024 * 1024 * 20;
        int[] fileUploadSizesList7 = { 1024 * 1024 * 11, 1024 * 1024 * 9, 0 };
        String fileName7 = fileName + 7;

        // SCM-4048:多次分段续传，每段大小不一致
        int fileSize8 = 1024 * 1024 * 30;
        int[] fileUploadSizesList8 = { 1024 * 1024 * 3, 1024 * 1024 * 5,
                1024 * 1024 * 10, 1024 * 1024 * 12 };
        String fileName8 = fileName + 8;

        return new Object[][] { { fileSize1, fileUploadSizesList1, fileName1 },
                { fileSize2, fileUploadSizesList2, fileName2 },
                { fileSize3, fileUploadSizesList3, fileName3 },
                { fileSize4, fileUploadSizesList4, fileName4 },
                { fileSize5, fileUploadSizesList5, fileName5 },
                { fileSize6, fileUploadSizesList6, fileName6 },
                { fileSize7, fileUploadSizesList7, fileName7 },
                { fileSize8, fileUploadSizesList8, fileName8 } };
    }
}