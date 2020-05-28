package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1133 :: 创建多级文件夹
 * @author fanyu
 * @Date:2018年4月19日
 * @version:1.0
 */
public class CreateMutiDir1133 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private String dirBasePath = "/CreateMutiDir1133";
    private String fullPath1 = dirBasePath
            + "/1133_a/1133_b/1133_c/1133_e/1133_f";
    private String fullPath2 = dirBasePath
            + "_1/1133_x/1133_x/1133_x/1133_x/1133_x";
    private SiteWrapper site;
    private WsWrapper wsp;
    private String author = "CreateMutiDir1133";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            deleteDir( ws, fullPath1 );
            deleteDir( ws, fullPath2 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        // create different dir
        createDir( ws, fullPath1 );
        BSONObject cond = new BasicBSONObject();
        cond.put( "name", "1133_f" );
        cond.put( "path", fullPath1 + "/" );
        cond.put( "paName", "1133_e" );
        check( ScmFactory.Directory.getInstance( ws, fullPath1 ), cond );

        // create same dir
        createDir( ws, fullPath2 );
        BSONObject cond1 = new BasicBSONObject();
        cond1.put( "name", "1133_x" );
        cond1.put( "path", fullPath2 + "/" );
        cond1.put( "paName", "1133_x" );
        check( ScmFactory.Directory.getInstance( ws, fullPath2 ), cond1 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                deleteDir( ws, fullPath1 );
                deleteDir( ws, fullPath2 );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void check( ScmDirectory dir, BSONObject cond ) {
        try {
            Assert.assertEquals( dir.getName(), cond.get( "name" ) );
            Assert.assertEquals( dir.getPath(), cond.get( "path" ) );
            Assert.assertEquals( dir.getUpdateUser(), TestScmBase.scmUserName );
            Assert.assertEquals( dir.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( dir.getWorkspaceName(), wsp.getName() );
            Assert.assertNotNull( dir.getCreateTime() );
            Assert.assertNotNull( dir.getUpdateTime() );
            Assert.assertEquals( dir.getParentDirectory().getName(),
                    cond.get( "paName" ) );
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
                e.printStackTrace();
                Assert.fail( e.getMessage() );
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
