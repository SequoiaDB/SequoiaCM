package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiacm.client.core.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.checksum.ChecksumException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @description SCM-1367:创建断点文件，以文件方式上传数据 SCM-3922:创建断点文件，以文件方式上传数据
 * @author wuyan
 * @createDate 2018.05.13
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */

public class BreakpointFile1367_3922 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "breakpointfile1367";
    private int[] fileSizes = { 1024 * 1024 * 4, 1024 * 1024 * 5,
            1024 * 1024 * 16, 1024 * 1024 * 45, 1024 * 1024 * 60 };
    private File localPath = null;
    private String filePath = null;
    private List< String > filePathList = new ArrayList<>();
    private List< ScmId > fileIdList = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSizes[ i ] + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSizes[ i ] );
            filePathList.add( filePath );
        }

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        List< ScmBreakpointFile > breakpointFile = new ArrayList<>();
        for ( int i = 0; i < fileSizes.length; i++ ) {
            breakpointFile.add( createBreakpointFile( fileName + i,
                    filePathList.get( i ), fileSizes[ i ] ) );
            checkFileData( breakpointFile.get( i ), fileName + i,
                    filePathList.get( i ) );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile( String fileName,
            String filePath, int fileSize )
            throws ScmException, ChecksumException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.ADLER32 );
        breakpointFile.upload( new File( filePath ) );

        // check file's attribute
        Assert.assertEquals( breakpointFile.getUploadSize(), fileSize );
        Assert.assertEquals( breakpointFile.getWorkspace(), ws );
        Assert.assertEquals( breakpointFile.isCompleted(), true );
        return breakpointFile;
    }

    private void checkFileData( ScmBreakpointFile breakpointFile,
            String fileName, String filePath ) throws Exception {
        // save to file, than down file check the file data
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileIdList.add( file.save() );

        // down file
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );

        // check results
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
    }

}