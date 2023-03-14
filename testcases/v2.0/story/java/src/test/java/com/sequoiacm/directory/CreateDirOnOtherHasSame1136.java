package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1136 :: 创建文件夹，其它文件夹存在同名文件夹或文件
 * @author fanyu
 * @Date:2018年4月23日
 * @version:1.0
 */
public class CreateDirOnOtherHasSame1136 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/CreateDirOnOtherHasSame1136";
    private String subDirBase = "1136";
    private ScmDirectory subDir1;
    private ScmDirectory subDir2;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( subDirBase )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );
            deleteDir( ws, dirBasePath );

            ScmFactory.Directory.createInstance( ws, dirBasePath );
            subDir1 = ScmFactory.Directory.createInstance( ws,
                    dirBasePath + "/" + subDirBase + "_1" );
            subDir2 = ScmFactory.Directory.createInstance( ws,
                    dirBasePath + "/" + subDirBase + "_2" );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // create ele in dir
        createEle( subDir1 );
        // create same ele in other dir
        createEle( subDir2 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                deleteDir( ws, subDir1.getPath() + subDirBase + "_1_dir" );
                deleteDir( ws, subDir2.getPath() + subDirBase + "_1_dir" );
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

    private void createEle( ScmDirectory subDir ) {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( subDirBase + "_1_file" );
            file.setAuthor( subDirBase );
            file.setDirectory( subDir );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
            ScmFactory.Directory.createInstance( ws,
                    subDir.getPath() + subDirBase + "_1_dir" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
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
