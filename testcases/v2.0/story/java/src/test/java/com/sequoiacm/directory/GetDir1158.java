package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1158 :: 获取根文件夹/获取多级文件夹下的文件夹
 * @author fanyu
 * @Date:2018年4月25日
 * @version:1.0
 */
public class GetDir1158 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/GetDir1158";
    private String fullPath1 = dirBasePath + "/1158_a";
    private ScmDirectory dir;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            for ( int i = 'b'; i < 'f'; i++ ) {
                deleteDir( ws, dirBasePath + "/1158_a/1158_" + ( char ) i );
            }

            dir = createDir( ws, fullPath1 );
            for ( int i = 'b'; i < 'f'; i++ ) {
                ScmFactory.Directory.createInstance( ws,
                        dirBasePath + "/1158_a/1158_" + ( char ) i );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        try {
            // check root dir
            ScmDirectory dir1 = ScmFactory.Directory.getInstance( ws, "/" );
            BSONObject cond1 = new BasicBSONObject();
            cond1.put( "name", "/" );
            cond1.put( "path", "/" );
            cond1.put( "ws", wsp.getName() );
            cond1.put( "paName", "/" );
            cond1.put( "updateUser", "admin" );
            cond1.put( "user", "admin" );
            check( dir1, cond1 );

            // check other dir
            ScmDirectory dir2 = dir.getSubdirectory( "1158_c" );
            BSONObject cond2 = new BasicBSONObject();
            cond2.put( "name", "1158_c" );
            cond2.put( "path", dirBasePath + "/1158_a/1158_c/" );
            cond2.put( "ws", wsp.getName() );
            cond2.put( "paName", "1158_a" );
            cond2.put( "updateUser", TestScmBase.scmUserName );
            cond2.put( "user", TestScmBase.scmUserName );
            check( dir2, cond2 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 'b'; i < 'f'; i++ ) {
                    deleteDir( ws, dirBasePath + "/1158_a/1158_" + ( char ) i );
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

    private void check( ScmDirectory dir, BSONObject cond ) {
        try {
            Assert.assertEquals( dir.getName(), cond.get( "name" ) );
            Assert.assertEquals( dir.getPath(), cond.get( "path" ) );
            Assert.assertEquals( dir.getUpdateUser(),
                    cond.get( "updateUser" ) );
            Assert.assertEquals( dir.getUser(), cond.get( "user" ) );
            Assert.assertEquals( dir.getWorkspaceName(), cond.get( "ws" ) );
            Assert.assertNotNull( dir.getCreateTime() );
            Assert.assertNotNull( dir.getUpdateTime() );
            if ( !cond.get( "name" ).equals( "/" ) ) {
                Assert.assertEquals( dir.getParentDirectory().getName(),
                        cond.get( "paName" ) );
            }
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
