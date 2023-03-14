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
 * @Description: SCM-3121:工作区batch_file_name_unique为true时，并发重命名文件和批次关联文件
 * @author fanyu
 * @Date:2020/10/14
 * @version:1.0
 */

public class Batch3121 extends TestScmBase {
    private SiteWrapper site = null;
    private String wsName = "ws3121";
    private String batchName = "batch3121";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3121_";
    private int fileNum = 20;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String batchId = "NO3121";
    private String dirName = "/dir3121";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type,设置batch_file_name_unique为true
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
        for ( int i = 0; i < fileNum / 2; i++ ) {
            batch.attachFile( fileIdList.get( i ) );
        }
    }

    @Test
    private void test() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 重命名文件A与批次内文件不同名
        threadExec.addWorker( new RenameFile( fileIdList.get( fileNum / 2 ),
                fileNameBase + "new_" + 1 ) );
        // 批次关联文件A
        threadExec.addWorker( new AttachFile( fileIdList.get( fileNum / 2 ) ) );
        // 批次内原有文件C重命名，新文件名与文件A同名
        threadExec.addWorker( new RenameFile( fileIdList.get( 0 ),
                fileNameBase + "new_" + 1 ) );

        // 重命名文件B与批次内文件同名
        threadExec.addWorker( new RenameFile( fileIdList.get( fileNum / 2 + 1 ),
                fileNameBase + 1 ) );
        // 批次关联文件B
        threadExec.addWorker(
                new AttachFile( fileIdList.get( fileNum / 2 + 1 ) ) );

        // 批次内原有文件D重命名，新文件与批次内文件不同名，与即将要关联的文件不同名
        threadExec.addWorker( new RenameFile( fileIdList.get( 1 ),
                fileNameBase + "new_" + 2 ) );
        threadExec.run();

        // 检查结果
        ScmBatch getBatch = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchId, false ) );
        Assert.assertTrue(
                fileNum / 2 <= getBatch.listFiles().size()
                        && getBatch.listFiles().size() <= fileNum / 2 + 2,
                getBatch.listFiles().size() + "" );
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
