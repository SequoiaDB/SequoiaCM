package com.sequoiacm.net.directory;

import java.io.IOException;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:specify directory to create multiple files,one of the version
 * files specifies the directory
 * testlink-case:SCM-2065
 *
 * @author wuyan
 * @Date 2018.07.12
 * @version 1.00
 */

public class CreateMutiVerFileWithDir2065a extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private ScmId fileId = null;

    private ScmDirectory scmDir;
    private String dirBasePath = "/CreatefileWiteDir2065";
    private String fullPath =
            dirBasePath + "/2065_a/2065_b/2065_c/2065_e/2065_f/";
    private String authorName = "CreateFileWithDir2065";
    private String fileName = "filedir2065";
    private byte[] writeData = new byte[ 1024 * 10 ];
    private byte[] updateData = new byte[ 1024 * 2 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        session = TestScmTools.createSession( branSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        ScmDirUtils.deleteDir( ws, fullPath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        fileId = VersionUtils
                .createFileByStream( ws, fileName, writeData, authorName );
        scmDir = ScmDirUtils.createDir( ws, fullPath );
        updateFileWithDirAndCheckContent( ws );
        checkFileDir( ws, fileId, scmDir );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmDirUtils.deleteDir( ws, fullPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void updateFileWithDirAndCheckContent( ScmWorkspace ws )
            throws Exception {
        //update ten times and add a directory to the second update
        int times = 10;
        int addDirTime = 2;
        for ( int i = 0; i < times; i++ ) {
            if ( i == addDirTime ) {
                ScmDirUtils
                        .updateContentWithDir( ws, fileId, updateData, scmDir );
            } else {
                VersionUtils.updateContentByStream( ws, fileId, updateData );
            }

            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            int majorVersion = file.getMajorVersion();

            String fullFileName = "";
            if ( i < addDirTime ) {
                fullFileName = fileName;
            } else {
                fullFileName = fullPath + fileName;
            }
            ScmDirUtils
                    .CheckFileContentByStream( ws, fullFileName, majorVersion,
                            updateData );
        }

    }

    private void checkFileDir( ScmWorkspace ws, ScmId fileId,
            ScmDirectory scmDir ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getDirectory().toString(),
                scmDir.toString() );
    }

}