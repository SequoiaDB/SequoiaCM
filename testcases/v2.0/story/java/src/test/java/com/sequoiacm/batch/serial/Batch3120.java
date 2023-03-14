package com.sequoiacm.batch.serial;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3120:工作区batch_file_name_unique为true时，并发删除批次/批次关联文件/批次解除文件/更改批次属性
 * @author fanyu
 * @Date:2020/10/15
 * @version:1.0
 */

public class Batch3120 extends TestScmBase {
    private SiteWrapper site = null;
    private String wsName = "ws3120";
    private String batchName = "batch3120";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3120_";
    private int fileNum = 20;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String batchId = "NO3120";
    private String dirName = "/dir3120";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为NONE,设置batch_file_name_unique为true
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.NONE, null, null, true );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        // 创建批次
        ScmBatch batch = ScmFactory.Batch.createInstance( ws, batchId );
        batch.setName( batchName );
        batch.save();
        // 准备文件
        prepareFile();
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( ScmId fileId : fileIdList ) {
            threadExec.addWorker( new AttachFile( fileId ) );
        }
        for ( ScmId fileId : fileIdList ) {
            threadExec.addWorker( new DetachFile( fileId ) );
        }
        threadExec.addWorker( new UpdateBatch( batchId ) );
        threadExec.addWorker( new DeleteBatch( batchId ) );
        threadExec.run();

        // 检查结果
        long count = ScmFactory.Batch.countInstance( ws, ScmQueryBuilder
                .start( ScmAttributeName.Batch.NAME ).is( batchName ).get() );
        Assert.assertEquals( count, 0 );
    }

    @AfterClass
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null ) {
            session.close();
        }
    }

    private class AttachFile {
        private ScmId fileId;

        public AttachFile( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void attachFile() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws,
                        new ScmId( batchId, false ) );
                batch.attachFile( fileId );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.BATCH_NOT_FOUND
                        && e.getError() != ScmError.FILE_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DetachFile {
        private ScmId fileId;

        public DetachFile( ScmId fileId ) {
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void detachFile() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws,
                        new ScmId( batchId, false ) );
                batch.detachFile( fileId );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.BATCH_NOT_FOUND
                        && e.getError() != ScmError.FILE_NOT_IN_BATCH
                        && e.getError() != ScmError.FILE_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateBatch {
        private String batchId;

        public UpdateBatch( String batchId ) {
            this.batchId = batchId;
        }

        @ExecuteOrder(step = 1)
        private void updateBatch() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmBatch batch = ScmFactory.Batch.getInstance( ws,
                        new ScmId( batchId, false ) );
                batch.setName( batchName + "_new" );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.BATCH_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

    }

    private class DeleteBatch {
        private String batchId;

        public DeleteBatch( String batchId ) {
            this.batchId = batchId;
        }

        @ExecuteOrder(step = 1)
        private void deleteBatch() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFactory.Batch.deleteInstance( ws,
                        new ScmId( batchId, false ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.BATCH_NOT_FOUND ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private void prepareFile() throws ScmException {
        ScmDirectory scmDirectory = ScmFactory.Directory.createInstance( ws,
                dirName );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            if ( i < fileNum / 2 ) {
                file.setDirectory( scmDirectory.getId() );
            }
            fileIdList.add( file.save() );
        }
    }
}
