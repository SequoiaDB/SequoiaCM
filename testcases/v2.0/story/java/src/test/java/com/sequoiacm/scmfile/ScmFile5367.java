package com.sequoiacm.scmfile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5367:上传文件与删除文件并发，指定相同FileId
 * @author ZhangYanan
 * @date 2022/11/03
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile5367 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private String fileAuthor = "file5367_";
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmWorkspace ws = null;
    private ScmId fileID = null;
    private BSONObject queryCond = null;
    private int threadUploadSuccessNums = 0;
    private int threadDeleteSuccessNums = 0;

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
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        String fileIdStr = ScmFileUtils.getFileIdByDate( new Date() );
        fileID = new ScmId( fileIdStr );
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadUploadFileById( fileAuthor ) );
        es.addWorker( new ThreadDeleteFileById() );
        es.run();

        if ( threadUploadSuccessNums == 1 && threadDeleteSuccessNums == 0 ) {
            SiteWrapper[] expSites = { rootSite };
            ScmFileUtils.checkMetaAndData( wsp, fileID, expSites, localPath,
                    filePath );
        } else if ( threadUploadSuccessNums == 1
                && threadDeleteSuccessNums == 1 ) {
            try {
                ScmFactory.File.deleteInstance( ws, fileID, true );
                Assert.fail( "预期失败实际成功！" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
        } else {
            Assert.fail( "文件上传预期成功，实际失败！" );
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

    public class ThreadUploadFileById extends ResultStore {
        private final String fileName;

        public ThreadUploadFileById( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws ScmException {
            try ( ScmSession session = TestScmTools.createSession( rootSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( fileName );
                file.setAuthor( fileAuthor );
                file.setFileId( fileID );
                ScmId scmId = file.save();

                Assert.assertEquals( fileID, scmId, "文件id与指定的fileId不一致，指定的id为:"
                        + fileID + " ;实际文件id为:" + scmId );
                threadUploadSuccessNums++;
            }
        }
    }

    public class ThreadDeleteFileById extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws ScmException {
            try ( ScmSession session = TestScmTools.createSession( rootSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.File.deleteInstance( ws, fileID, true );
                threadDeleteSuccessNums++;
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
        }
    }
}