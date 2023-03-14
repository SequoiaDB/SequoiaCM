package com.sequoiacm.directory.serial;

import java.io.IOException;
import java.util.List;

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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2940:连接不同站点创建/重命名/移动/获取/删除同一目录
 * @author fanyu
 * @Date:2020年06月28日
 * @version:1.0
 */
public class CrudDir2940 extends TestScmBase {
    private List< SiteWrapper > sites;
    private WsWrapper wsp;
    private String fullPath = "/dir2940/dir2940A/dir2940B/dir2940C/dir2940D";
    private ScmSession session;
    private ScmWorkspace ws;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        sites = ScmInfo.getAllSites();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( sites.get( 0 ) );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理环境
        String dirPath = fullPath;
        while ( !dirPath.equals( "" ) ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, dirPath );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                    throw e;
                }
            }
            dirPath = dirPath.substring( 0, dirPath.lastIndexOf( "/" ) );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        ScmSession session1 = null;
        ScmSession session2 = null;
        try {
            session1 = ScmSessionUtils.createSession( sites.get( 0 ) );
            session2 = ScmSessionUtils.createSession( sites.get( 1 ) );
            ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session1 );
            ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session2 );

            // 连接站点1创建目录
            String dirPath = "/";
            while ( fullPath.indexOf( "/", dirPath.length() ) != -1 ) {
                dirPath = dirPath + fullPath.substring( dirPath.length(),
                        fullPath.indexOf( "/", dirPath.length() ) );
                ScmFactory.Directory.createInstance( ws1, dirPath );
                dirPath = dirPath + "/";
            }
            ScmFactory.Directory.createInstance( ws1, fullPath );

            // 连接站点2重命名目录
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws2,
                    fullPath );
            dir.rename( "dir2940E" );

            // 连接站点1移动目录
            ScmDirectory srcDir = ScmFactory.Directory.getInstance( ws1,
                    "/dir2940/dir2940A/dir2940B/dir2940C" );
            ScmDirectory destDir = ScmFactory.Directory.getInstance( ws1,
                    "/dir2940/dir2940A" );
            // 重命名和移动后目录： /dir2940/dir2940A/dir2940C/dir2940E、
            // /dir2940/dir2940A/dir2940B
            srcDir.move( destDir );

            // 连接站点2获取目录
            // 重命名前的目录：/dir2940/dir2940A/dir2940B/dir2940C/dir2940D
            try {
                ScmFactory.Directory.getInstance( ws2, fullPath );
                Assert.fail( "exp failed but act success!!!!" );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                    throw e;
                }
            }

            // 移动后的目录： /dir2940/dir2940A/dir2940C/dir2940E
            String movePath = "/dir2940/dir2940A/dir2940C/dir2940E";
            ScmDirectory directory = ScmFactory.Directory.getInstance( ws2,
                    movePath );
            Assert.assertEquals( directory.getPath(), movePath + "/" );

            // 连接站点1删除目录
            // 重命名前的目录：/dir2940/dir2940A/dir2940B/dir2940C/dir2940D
            try {
                ScmFactory.Directory.deleteInstance( ws1, fullPath );
                Assert.fail( "exp failed but act success!!!!" );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                    throw e;
                }
            }
            // 移动后的目录： /dir2940/dir2940A/dir2940C/dir2940E
            ScmFactory.Directory.deleteInstance( ws, movePath );
            Assert.assertEquals(
                    ScmFactory.Directory.isInstanceExist( ws1, movePath ),
                    false );

            // 连接站点2删除目录
            String deletePath1 = "/dir2940/dir2940A/dir2940C";
            String deletePath2 = "/dir2940/dir2940A/dir2940B";
            ScmFactory.Directory.deleteInstance( ws2, deletePath2 );
            while ( !deletePath1.equals( "" ) ) {
                ScmFactory.Directory.deleteInstance( ws2, deletePath1 );
                deletePath1 = dirPath.substring( 0,
                        deletePath1.lastIndexOf( "/" ) );
            }

            // 连接站点1获取目录
            String getDirPath = fullPath;
            while ( !getDirPath.equals( "" ) ) {
                try {
                    ScmFactory.Directory.getInstance( ws1, getDirPath );
                    Assert.fail( "exp failed but act success!!!!" );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                        throw e;
                    }
                }
                getDirPath = getDirPath.substring( 0,
                        getDirPath.lastIndexOf( "/" ) );
            }
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
            if ( session2 != null ) {
                session2.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        session.close();
    }
}
