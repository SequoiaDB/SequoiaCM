package com.sequoiacm.version;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1646:当前文件内容为空，更新文件内容
 * @author wuyan
 * @createDate 2018.06.01
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateContent1646 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private boolean runSuccess = false;
    private String fileName = "file1646";
    private int fileSize = 0;
    private int dataSize = 1024 * 10;
    private byte[] filedata = new byte[ dataSize ];
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        fileId = ScmFileUtils.create( ws, fileName, filePath );
        // test a:updateContent by empty file
        updateContentByEmptyFile();
        // test b:updateContext by stream
        updateContentByStream();

        // check result
        int currentVersion = 3;
        int historyVersion1 = 1;
        int historyVersion2 = 2;
        byte[] expdata = new byte[ 0 ];
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                filedata );
        VersionUtils.CheckFileContentByFile( ws, fileName, historyVersion2,
                filePath, localPath );
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion1,
                expdata );
        checkFileSize( currentVersion, dataSize );
        checkFileSize( historyVersion1, fileSize );
        checkFileSize( historyVersion2, fileSize );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    // test a:updateContent by empty file
    private void updateContentByEmptyFile() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( filePath );
    }

    // test b:updateContext by stream
    private void updateContentByStream() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        new Random().nextBytes( filedata );
        file.updateContent( new ByteArrayInputStream( filedata ) );
    }

    private void checkFileSize( int version, int expSize ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, version, 0 );
        Assert.assertEquals( file.getSize(), expSize );
        Assert.assertEquals( file.getMajorVersion(), version );
    }
}