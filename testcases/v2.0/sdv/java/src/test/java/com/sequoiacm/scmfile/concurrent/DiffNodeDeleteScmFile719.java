package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-719 :本地中心不同节点并发删除文件 1、同一个中心的不同节点并发删除文件； 2、检查删除结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class DiffNodeDeleteScmFile719 extends TestScmBase {
    private static final String author = "DiffNodeDeleteScmFile719";
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmWorkspace ws = null;
    private int fileSize = 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 2;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            prepareFiles();
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {

        DeleteScmFile dThread1 = new DeleteScmFile( fileIdList.get( 0 ) );
        dThread1.start( 5 );

        DeleteScmFile dThread2 = new DeleteScmFile( fileIdList.get( 0 ) );
        dThread2.start( 5 );

        DeleteScmFile dThread3 = new DeleteScmFile( fileIdList.get( 1 ) );
        dThread3.start( 5 );

        if ( !( dThread1.isSuccess() && dThread2.isSuccess() &&
                dThread3.isSuccess() ) ) {
            Assert.fail( dThread1.getErrorMsg() + dThread2.getErrorMsg() +
                    dThread3.getErrorMsg() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }

        }
    }

    private void checkResults( ScmWorkspace ws, ScmId fileId )
            throws Exception {
        try {
            // check meta
            BSONObject cond = new BasicBSONObject( "id", fileId.get() );
            long cnt = ScmFactory.File
                    .countInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( cnt, 0 );

            // check data
            ScmFileUtils.checkData( ws, fileId, localPath, filePath );
            Assert.assertFalse( true,
                    "File is unExisted, except throw e, but success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepareFiles() throws ScmException {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( author + "_" + i + UUID.randomUUID() );
            scmfile.setAuthor( author );
            fileIdList.add( scmfile.save() );
        }
    }

    private class DeleteScmFile extends TestThreadBase {
        private ScmId fileId = null;

        public DeleteScmFile( ScmId fileId ) {
            this.fileId = fileId;
        }

        @Override
        public void exec() throws Exception {
            ScmSession ss = null;
            try {
                ss = TestScmTools.createSession( site );
                ScmWorkspace wks = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), ss );
                ScmFactory.File.getInstance( wks, this.fileId ).delete( true );
                checkResults( wks, this.fileId );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.FILE_NOT_FOUND &&
                        e.getError() != ScmError.DATA_ERROR ) {
                    Assert.fail( e.getMessage() );
                }
            } finally {
                if ( ss != null ) {
                    ss.close();
                }
            }
        }
    }

}
