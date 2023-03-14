package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-3924:以输入流方式断点续传文件（指定文件校验） SCM-1369:以输入流方式断点续传文件（指定文件校验）
 * @author YiPan
 * @date 2021/10/28
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3924_1369 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private BSONObject query;

    private String fileName = "file3924";
    private int fileSize = 1024 * 1024 * 60;
    private byte[] data = new byte[ fileSize ];
    private File localPath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        query = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        new Random().nextBytes( data );
        byte[] uploadFirst = Arrays.copyOfRange( data, 0, fileSize / 2 );
        createBreakpointFile( uploadFirst );
        continuesUploadFile( data );
        checkUploadFileData();
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

    private void createBreakpointFile( byte[] datapart ) throws ScmException {
        // create breakpointfile
        ScmChecksumType checksumType = ScmChecksumType.CRC32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );
        breakpointFile.incrementalUpload( new ByteArrayInputStream( datapart ),
                false );
    }

    private void continuesUploadFile( byte[] datapart ) throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        breakpointFile.upload( new ByteArrayInputStream( datapart ) );
    }

    private void checkUploadFileData() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.save();

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );

        // check results
        byte[] fileData = outputStream.toByteArray();
        Assert.assertEquals( fileData, data );
    }

}