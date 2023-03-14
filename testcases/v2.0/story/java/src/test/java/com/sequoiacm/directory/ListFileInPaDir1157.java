package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1157 :: 在当前文件夹下，根据条件检索文件
 * @author fanyu
 * @Date:2018年4月26日
 * @version:1.0
 */
public class ListFileInPaDir1157 extends TestScmBase {
    private boolean runSuccess1;
    private boolean runSuccess2;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/ListFileInPaDir1157";
    private String fullPath1 = dirBasePath;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = "ListFileInPaDir1157";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            for ( int i = 'a'; i < 'f'; i++ ) {
                deleteDir( ws, dirBasePath + "/1157_" + ( char ) i );
            }
            createDir( ws, fullPath1 );
            for ( int i = 'a'; i < 'f'; i++ ) {
                ScmFactory.Directory.createInstance( ws,
                        dirBasePath + "/1157_" + ( char ) i );
            }
            createFile( ws, ScmFactory.Directory.getInstance( ws,
                    dirBasePath + "/1157_e" ) );
            createFile( ws, ScmFactory.Directory.getInstance( ws,
                    dirBasePath + "/1157_a" ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void testCondIsNull() {
        try {
            int i = 0;
            int expDirNum = 0;
            ScmDirectory pdir = ScmFactory.Directory.getInstance( ws,
                    fullPath1 );
            ScmCursor< ScmFileBasicInfo > fileCursor = pdir.listFiles( null );
            while ( fileCursor.hasNext() ) {
                ScmFileBasicInfo fileInfo = fileCursor.getNext();
                Assert.assertNotNull( fileInfo.getFileName() );
                i++;
            }
            Assert.assertTrue( i == expDirNum );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { GroupTags.base })
    private void testCond() {
        String dirName = "1157_e";
        int expDirNum = 1;
        ScmFileBasicInfo fileInfo = null;
        try {
            int i = 0;
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author )
                    .and( ScmAttributeName.File.MAJOR_VERSION ).is( 1 )
                    .and( ScmAttributeName.File.TITLE ).is( author )
                    .and( ScmAttributeName.File.CREATE_TIME ).greaterThan( 0 )
                    .and( ScmAttributeName.File.UPDATE_TIME ).greaterThan( 0 )
                    .get();
            ScmDirectory pdir = ScmFactory.Directory.getInstance( ws,
                    fullPath1 + "/" + dirName );
            ScmCursor< ScmFileBasicInfo > fileCursor = pdir.listFiles( cond );
            while ( fileCursor.hasNext() ) {
                fileInfo = fileCursor.getNext();
                Assert.assertNotNull( fileInfo.getFileName() );
                Assert.assertEquals( fileInfo.getFileId().get(),
                        fileIdList.get( 0 ).get() );
                i++;
            }
            Assert.assertTrue( i == expDirNum );
        } catch ( ScmException e ) {
            System.out.println( fileInfo.toString() );
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess2 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess1 || runSuccess2 || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( int i = 'a'; i < 'f'; i++ ) {
                    deleteDir( ws, dirBasePath + "/1157_" + ( char ) i );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws, ScmDirectory dir ) {
        ScmFile file;
        try {
            file = ScmFactory.File.createInstance( ws );
            file.setFileName( author + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            file.setTitle( author );
            file.setDirectory( dir );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private ScmDirectory createDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( String path : pathList ) {
            try {
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
        return ScmFactory.Directory.getInstance( ws,
                pathList.get( pathList.size() - 1 ) );
    }

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private List< String > getSubPaths( String path ) {
        String ele = "/";
        String[] arry = path.split( "/" );
        List< String > pathList = new ArrayList< String >();
        for ( int i = 1; i < arry.length; i++ ) {
            ele = ele + arry[ i ];
            pathList.add( ele );
            ele = ele + "/";
        }
        return pathList;
    }
}
