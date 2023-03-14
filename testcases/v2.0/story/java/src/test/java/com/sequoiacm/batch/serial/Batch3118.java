package com.sequoiacm.batch.serial;

import java.util.List;

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

/**
 * @Description SCM-3118:指定batch_file_name_unique为true，重命名文件
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/14
 */
public class Batch3118 extends TestScmBase {
    private String wsName = "ws3118";
    private String batchName = "batch3118";
    private ScmId batchId = null;
    private String fileNameBase = "file3118";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String dirName = "/dir3118";
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmId fileId3 = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为YEAR,isFileNameUnique为true
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.YEAR, null, null, true );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        prepareFile();
    }

    @Test
    private void test() throws Exception {
        // 不指定id创建批次
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();

        // 批次关联文件
        ScmBatch getBatchA = ScmFactory.Batch.getInstance( ws, batchId );
        getBatchA.attachFile( fileId1 );
        getBatchA.attachFile( fileId2 );
        getBatchA.attachFile( fileId3 );

        // 重命名文件
        ScmFile scmFile1 = ScmFactory.File.getInstance( ws, fileId2 );
        // 与批次内文件同名
        try {
            scmFile1.setFileName( fileNameBase + "_1" );
            Assert.fail(
                    "exp failed but act success!!! fileId = " + fileId2.get() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BATCH_FILE_SAME_NAME ) {
                throw e;
            }
        }
        ScmFile scmFile2 = ScmFactory.File.getInstance( ws, fileId3 );
        // 与批次内文件不同名
        scmFile2.setFileName( fileNameBase + "_4" );

        // 获取批次信息，检查结果
        checkResults();
        // 删除批次
        ScmFactory.Batch.deleteInstance( ws, batchId );
        // 检查结果
        long count = ScmFactory.Batch.countInstance( ws, ScmQueryBuilder
                .start( ScmAttributeName.Batch.NAME ).is( batchName ).get() );
        Assert.assertEquals( count, 0 );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null )
            session.close();
    }

    private void prepareFile() throws ScmException {
        // 创建文件
        ScmFile file1 = ScmFactory.File.createInstance( ws );
        file1.setFileName( fileNameBase + "_1" );
        file1.setAuthor( "A" );
        fileId1 = file1.save();

        // 创建同名文件
        ScmDirectory directory = ScmFactory.Directory.createInstance( ws,
                dirName );
        ScmFile file2 = ScmFactory.File.createInstance( ws );
        file2.setFileName( fileNameBase + "_2" );
        file2.setAuthor( "A" );
        file2.setDirectory( directory.getId() );
        fileId2 = file2.save();

        // 创建不同名文件
        ScmFile file3 = ScmFactory.File.createInstance( ws );
        file3.setAuthor( "B" );
        file3.setFileName( fileNameBase + "_3" );
        fileId3 = file3.save();
    }

    private void checkResults() throws ScmException {
        ScmBatch getBatchB = ScmFactory.Batch.getInstance( ws, batchId );
        Assert.assertEquals( getBatchB.getName(), batchName );
        Assert.assertEquals( getBatchB.getId(), batchId );
        List< ScmFile > files = getBatchB.listFiles();
        Assert.assertEquals( files.size(), 3 );
        for ( ScmFile file : files ) {
            Assert.assertEquals( file.getBatchId(), batchId );
            Assert.assertEquals( file.getSize(), 0 );
        }
    }
}
