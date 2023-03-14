package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1200:ScmFactory.Directory中的列取操作参数校验
 * @author fanyu
 * @Date:2018年4月26日
 * @version:1.0
 */
public class ListInstance_Param1200 extends TestScmBase {
    private boolean runSuccess1;
    private boolean runSuccess2;
    private boolean runSuccess3;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/ListInstance_Param1201";
    private String fullPath1 = dirBasePath
            + "/1201_a/ListInstance_Param1201文件夹a/1201_c";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            deleteDir( ws, fullPath1 );
            createDir( ws, fullPath1 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        BSONObject cond = null;
        String name = "ListInstance_Param1201文件夹a";
        int i = 0;
        int expNum = 1;
        try {
            cond = ScmQueryBuilder.start( ScmAttributeName.Directory.NAME )
                    .is( name ).get();
            ScmCursor< ScmDirectory > dirCursor = ScmFactory.Directory
                    .listInstance( ws, cond );
            while ( dirCursor.hasNext() ) {
                ScmDirectory dir = dirCursor.getNext();
                Assert.assertEquals( dir.getName(), name );
                i++;
            }
            Assert.assertEquals( i, expNum );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testWsIsNull() throws ScmException {
        BSONObject cond = null;
        String name = "ListInstance_Param1201文件夹a";
        try {
            cond = ScmQueryBuilder.start( ScmAttributeName.Directory.NAME )
                    .is( name ).get();
            ScmFactory.Directory.listInstance( null, cond );
            Assert.fail( "exp success but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIllegalMatch() throws ScmException {
        BSONObject cond = null;
        String name = "$ListInstance_Param1201文件夹a";
        try {
            cond = ScmQueryBuilder.start( name ).is( name ).get();
            ScmFactory.Directory.listInstance( ws, cond );
            Assert.fail( "exp success but act success" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            if ( e.getError() != ScmError.METASOURCE_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess3 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIllegalMatch1() throws ScmException {
        BSONObject cond = null;
        String name = "$ListInstance_Param1201文件夹a";
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, "/" );
            cond = ScmQueryBuilder.start( name ).is( name ).get();
            dir.listDirectories( cond );
            Assert.fail( "exp success but act success" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            if ( e.getError() != ScmError.METASOURCE_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess3 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testIllegalMatch3() throws ScmException {
        BSONObject cond = null;
        String name = "$ListInstance_Param1201文件夹a";
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, "/" );
            cond = ScmQueryBuilder.start( name ).is( name ).get();
            dir.listFiles( cond );
            Assert.fail( "exp success but act success" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            if ( e.getError() != ScmError.METASOURCE_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess3 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess1 || runSuccess2 || runSuccess3
                    || TestScmBase.forceClear ) {
                deleteDir( ws, fullPath1 );
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
