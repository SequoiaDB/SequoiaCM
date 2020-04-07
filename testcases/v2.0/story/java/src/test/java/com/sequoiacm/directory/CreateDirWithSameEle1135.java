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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1135 :: 创建文件夹，父文件夹下已存在同名文件夹或文件
 * @author fanyu
 * @Date:2018年4月23日
 * @version:1.0
 */
public class CreateDirWithSameEle1135 extends TestScmBase {
    private boolean runSuccess;
    private ScmSession session;
    private ScmWorkspace ws;
    private SiteWrapper site;
    private WsWrapper wsp;
    private String dirBasePath = "/CreateDirWithSameEle1135";
    private ScmDirectory scmDir;
    private String eleName = "1135_same";
    private String eleName1 = "1135_diff";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( eleName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );
            deleteDir( ws, dirBasePath );

            scmDir = ScmFactory.Directory.createInstance( ws, dirBasePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        createDirWithSameDir();
        createDirWithSameFile();
        createDirWithDiff();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                deleteDir( ws, dirBasePath + "/" + eleName1 );
                deleteDir( ws, dirBasePath );
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

    private void createDirWithSameDir() throws ScmException {
        ScmDirectory dir = null;
        try {
            dir = ScmFactory.Directory
                    .createInstance( ws, dirBasePath + "/" + eleName );
            ScmFactory.Directory
                    .createInstance( ws, dirBasePath + "/" + eleName );
            Assert.fail(
                    "dir alreay existed,it should not create same dir " +
                            "successfully" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( dir != null ) {
                ScmFactory.Directory.deleteInstance( ws, dir.getPath() );
            }
        }
    }

    private void createDirWithSameFile() throws ScmException {
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( eleName );
            file.setDirectory( scmDir );
            fileId = file.save();
            ScmFactory.Directory
                    .createInstance( ws, dirBasePath + "/" + eleName );
            Assert.fail(
                    "parent dir alreay have a file with same name,it should " +
                            "not create dir successfully" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }
    }

    private void createDirWithDiff() throws ScmException {
        ScmId fileId = null;
        ScmDirectory dir = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( eleName + "_file" );
            file.setDirectory( scmDir );
            fileId = file.save();
            dir = ScmFactory.Directory
                    .createInstance( ws, dirBasePath + "/" + eleName1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            if ( dir != null ) {
                ScmFactory.Directory.deleteInstance( ws, dir.getPath() );
            }
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
