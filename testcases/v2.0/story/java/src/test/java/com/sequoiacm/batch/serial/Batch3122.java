package com.sequoiacm.batch.serial;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
 * @Description: SCM-3122:工作区batch_file_name_unique为true时,并发重命名文件和批次解除文件
 * @author fanyu
 * @Date:2020/10/15
 * @version:1.0
 */

public class Batch3122 extends TestScmBase {
    private SiteWrapper site = null;
    private String wsName = "ws3122";
    private String batchName = "batch3122";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3122_";
    private int fileNum = 20;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String batchId = "NO3122";
    private String dirName = "/dir3122";

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
        // 批次关联文件
        for ( int i = 0; i < fileNum; i++ ) {
            batch.attachFile( fileIdList.get( i ) );
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 文件id
        ScmId fileId1 = fileIdList.get( fileNum / 2 );
        ScmId fileId2 = fileIdList.get( ( fileNum * 2 ) / 3 );
        // 同名
        threadExec.addWorker( new RenameFile( fileId1, fileNameBase + 2 ) );
        // 不同名
        threadExec.addWorker(
                new RenameFile( fileId2, fileNameBase + "new_" + 1 ) );
        // 批次解除关联
        threadExec.addWorker( new DetachFile( fileId1 ) );
        threadExec.addWorker( new DetachFile( fileId2 ) );
        threadExec.run();

        // 检查结果
        ScmBatch getBatch = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchId, false ) );
        Assert.assertEquals( getBatch.listFiles().size(), fileNum - 2 );
    }

    @AfterClass
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null ) {
            session.close();
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
                if ( e.getError() != ScmError.BATCH_FILE_SAME_NAME ) {
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class RenameFile {
        private ScmId fileId;
        private String newName;

        public RenameFile( ScmId fileId, String newName ) {
            this.fileId = fileId;
            this.newName = newName;
        }

        @ExecuteOrder(step = 1)
        private void rename() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setFileName( newName );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.BATCH_FILE_SAME_NAME
                        && e.getError() != ScmError.OPERATION_TIMEOUT ) {
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
