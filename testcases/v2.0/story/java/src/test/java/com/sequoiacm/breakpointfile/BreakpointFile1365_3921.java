package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @description SCM-1365:创建断点文件名重复 SCM-3921:创建断点文件名重复
 * @author wuyan
 * @createDate 2018.05.11
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */

public class BreakpointFile1365_3921 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "breakpointfile1365";
    private String filename = "testfile1365";
    private int fileSize = 0;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass()
    private void setUp() throws ScmException, IOException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        createBreakpointFile();
        createBreakpointfileByFileName();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
                ScmFactory.BreakpointFile.deleteInstance( ws, filename );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private void createBreakpointFile() {
        try {
            // create file
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .createInstance( ws, fileName );
            breakpointFile.upload( new File( filePath ) );

            // create the same file name
            ScmFactory.BreakpointFile.createInstance( ws, fileName );
            byte[] data = new byte[ 10 ];
            new Random().nextBytes( data );
            breakpointFile.upload( new ByteArrayInputStream( data ) );

        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }

        }
    }

    private void createBreakpointfileByFileName() throws ScmException {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( filename );
            file.setContent( filePath );
            fileId = file.save();

            // create the same file name
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .createInstance( ws, filename );
            byte[] data = new byte[ 10 ];
            new Random().nextBytes( data );
            breakpointFile.upload( new ByteArrayInputStream( data ) );
            file.setContent( filename );
            file.save();

        } catch ( ScmException e ) {
            if ( ScmError.OPERATION_UNSUPPORTED != e.getError() ) {
                throw e;
            }
        }

    }

}