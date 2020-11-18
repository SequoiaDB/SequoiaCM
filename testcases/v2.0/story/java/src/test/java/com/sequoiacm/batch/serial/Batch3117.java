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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description SCM-3117:指定batch_file_name_unique为false，重命名文件
 * @author fanyu
 * @version 1.00
 * @Date 2020/10/14
 */
public class Batch3117 extends TestScmBase {
    private String wsName = "ws3117";
    private String batchName = "batch3117";
    private String batchId = "NO3117";
    private String fileNameBase = "file3117";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String dirName = "/dir3117";
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmId fileId3 = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 指定batch_sharding_type为NONE,不设置batch_id_time_regexp、batch_id_time_parttern，batch_file_name_unique为false
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.NONE, null, null, false );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        prepareFile();
    }

    @Test
    private void test() throws Exception {
        // 不指定id创建批次
        ScmBatch batch = ScmFactory.Batch.createInstance( ws, batchId );
        batch.setName( batchName );
        batch.save();

        // 批次关联文件
        ScmBatch getBatchA = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchId, false ) );
        getBatchA.attachFile( fileId1 );
        getBatchA.attachFile( fileId2 );
        getBatchA.attachFile( fileId3 );

        // 重命名文件
        ScmFile scmFile1 = ScmFactory.File.getInstance( ws, fileId2 );
        // 与批次内文件不同名
        scmFile1.setFileName( fileNameBase + "_1" );
        ScmFile scmFile2 = ScmFactory.File.getInstance( ws, fileId3 );
        // 与批次内文件同名
        scmFile2.setFileName( fileNameBase + "_3" );

        // 获取批次信息，检查结果
        checkResults();
        // 删除批次
        ScmFactory.Batch.deleteInstance( ws, new ScmId( batchId, false ) );
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
        file3.setFileName( fileNameBase + "_2" );
        fileId3 = file3.save();
    }

    private void checkResults() throws ScmException {
        ScmBatch getBatchB = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchId, false ) );
        Assert.assertEquals( getBatchB.getName(), batchName );
        Assert.assertEquals( getBatchB.getId().get(), batchId );
        List< ScmFile > files = getBatchB.listFiles();
        Assert.assertEquals( files.size(), 3 );
        for ( ScmFile file : files ) {
            System.out.println( "file = " + file.toString() );
            if ( file.getAuthor().equals( "A" ) ) {
                Assert.assertEquals( file.getFileName(), fileNameBase + "_1" );
            } else {
                Assert.assertEquals( file.getFileName(), fileNameBase + "_3" );
            }
            Assert.assertEquals( file.getBatchId().get(), batchId );
            Assert.assertEquals( file.getSize(), 0 );
        }
    }
}
