package com.sequoiacm.net.directory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

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
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create multiple directory,include multilevel directory
 * testlink-case:SCM-2063
 *
 * @author wuyan
 * @Date 2018.07.12
 * @version 1.00
 */

public class CreateMutiDirs2063 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private List< String > dirNames = new ArrayList< String >();
    private LinkedBlockingDeque< ScmId > fileIDs = new LinkedBlockingDeque<
            ScmId >();

    private String authorName = "CreateFileWithDir2063";
    private String fileName = "filedir2063";
    private byte[] writeData = new byte[ 1024 * 2 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        session = TestScmTools.createSession( branSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        //clean dir
        String dirPath = "/CreatefileWiteDir2063";
        for ( int i = 0; i < 50; i++ ) {
            dirPath = "/CreatefileWiteDir2063" + "_" + i + "/";
            ;
            if ( i % 2 == 0 ) {
                dirPath = "/CreatefileWiteDir2063" + "_" + i + "/";
            } else {
                dirPath = "/CreatefileWiteDir2063";
                int level = 100;
                for ( int j = 0; j < level; j++ ) {
                    dirPath = dirPath + "_" + i + "_subdir_" + j + "/";

                }
            }
            ScmDirUtils.deleteDir( ws, dirPath );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // create 50 dirs
        new Random().nextBytes( writeData );
        List< CreateDir > createDirs = new ArrayList<>( 50 );
        String dirPath = "/CreatefileWiteDir2063";
        for ( int i = 0; i < 50; i++ ) {
            if ( i % 2 == 0 ) {
                dirPath = "/CreatefileWiteDir2063" + "_" + i + "/";
            } else {
                dirPath = "/CreatefileWiteDir2063";
                int level = 100;
                for ( int j = 0; j < level; j++ ) {
                    dirPath = dirPath + "_" + i + "_subdir_" + j + "/";

                }
            }

            createDirs.add( new CreateDir( dirPath ) );
            dirNames.add( dirPath );
        }

        for ( CreateDir createDir : createDirs ) {
            createDir.start();
        }

        for ( CreateDir createDir : createDirs ) {
            Assert.assertTrue( createDir.isSuccess(), createDir.getErrorMsg() );
        }

        checkDirAndFileNums();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                for ( ScmId fileId : fileIDs ) {
                    try {
                        ScmFactory.File.deleteInstance( ws, fileId, true );
                    } catch ( ScmException e ) {
                        System.out.println( "MSG = " + e.getMessage() );
                    }
                }
                for ( String fullPath : dirNames ) {
                    ScmDirUtils.deleteDir( ws, fullPath );
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileDirInfoAndData( ScmWorkspace ws, ScmId fileId,
            ScmDirectory scmDir ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getDirectory().toString(),
                scmDir.toString() );
        VersionUtils.CheckFileContentByStream( ws,
                scmDir.getPath() + file.getFileName(), file.getMajorVersion(),
                writeData );
    }

    private void checkDirAndFileNums() {
        int expNums = 50;
        Assert.assertEquals( dirNames.size(), expNums, "dir nums is error!" );
        Assert.assertEquals( fileIDs.size(), expNums, "file nums is error!" );
    }

    private ScmId createFileWithDir( ScmWorkspace ws, String fileName,
            byte[] data, String authorName,
            ScmDirectory dir ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        if ( dir != null ) {
            file.setDirectory( dir );
        }
        file.setMimeType( fileName + ".txt" );
        //add tags
        ScmTags tags = new ScmTags();
        tags.addTag(
                "我是一个标签  2063                                                " +
                        "                                                    " +
                        "                                                    " +
                        "                            "
                        + "                                " );
        tags.addTag( "THIS IS TAG2063!" );
        tags.addTag( "tag *&^^^^^*90234@#$%!~asf" );
        file.setTags( tags );
        ScmId fileId = file.save();
        return fileId;
    }

    public class CreateDir extends TestThreadBase {
        String fullPath;

        public CreateDir( String fullPath ) {
            this.fullPath = fullPath;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmDirectory scmDir = ScmDirUtils.createDir( ws, fullPath );
                ScmId fileId = createFileWithDir( ws,
                        fileName + "_" + scmDir.getId(), writeData, authorName,
                        scmDir );
                fileIDs.offer( fileId );
                checkFileDirInfoAndData( ws, fileId, scmDir );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}