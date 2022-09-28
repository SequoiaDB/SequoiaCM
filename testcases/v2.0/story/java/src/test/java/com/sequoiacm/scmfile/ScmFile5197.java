package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5197:写文件和按路径删除文件并发
 * @author ZhangYanan
 * @date 2022/09/06
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile5197 extends TestScmBase {
    private boolean runSuccess = false;
    private boolean isDeleteSuccess = false;
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file5197";
    private int fileSize = 1024 * 1024;
    private ArrayList< ScmId > fileIdList = new ArrayList<>();
    private String filePath = null;
    private File localPath = null;
    private BSONObject queryCond = null;

    @BeforeClass()
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        DeleteFile deleteFile = new DeleteFile();
        CreateFile createFile = new CreateFile();
        t.addWorker( deleteFile );
        t.addWorker( createFile );
        t.run();

        // 校验文件
        if ( isDeleteSuccess ) {
            try {
                ScmFactory.File.getInstanceByPath( ws, "/" + fileName );
                Assert.fail( "the file expected not exist" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
        } else {
            SiteWrapper[] expSite = { rootSite };
            ScmScheduleUtils.checkScmFile( ws, fileIdList, expSite );
        }
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class DeleteFile {
        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools.createSession( rootSite )) {
                // 等待一段时间，等待创建文件线程执行
                Thread.sleep( 70 );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.File.deleteInstanceByPath( ws, "/" + fileName,
                        true );
                isDeleteSuccess = true;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
                        .getErrorCode() )
                    throw e;
            }
        }
    }

    private class CreateFile {
        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools.createSession( rootSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                fileIdList.add( ScmFileUtils.create( ws, fileName, filePath ) );
            }
        }
    }
}