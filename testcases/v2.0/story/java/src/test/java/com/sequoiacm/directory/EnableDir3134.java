package com.sequoiacm.directory;

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-3134:打开目录功能，创建/查询/删除/新增文件版本/更新文件属性/目录相关操作
 * @author fanyu
 * @Date:2020/10/20
 * @version:1.0
 */

public class EnableDir3134 extends TestScmBase {
    private String wsName = "ws3134";
    private String batchName = "batch3134";
    private String fileNameBase = "file3134_";
    private String dirName = "/dir3134";
    private String dirId = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String batchIdA = "2020";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 打开目录功能
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.YEAR, ".*", "yyyy", false, true );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        Assert.assertEquals( ws.isEnableDirectory(), true );
        Assert.assertEquals( ws.getBatchShardingType(), ScmShardingType.YEAR );
        ScmDirectory scmDirectory = ScmFactory.Directory.createInstance( ws,
                dirName );
        dirId = scmDirectory.getId();
    }

    @Test
    private void test() throws Exception {
        createFile();
        queryFile();
        updateFileContent();
        updaetFileAttr();
        crudDir();
        crudBatch();
        deleteFile();
    }

    private void createFile() throws Exception {
        byte[] bytes = new byte[ 1024 * 200 ];
        new Random().nextBytes( bytes );
        // 创建文件并设置目录属性
        ScmFile file1 = ScmFactory.File.createInstance( ws );
        file1.setFileName( fileNameBase + "A" );
        file1.setAuthor( fileNameBase );
        file1.setDirectory( dirId );
        fileId = file1.save();
    }

    private void queryFile() throws ScmException {
        // 通过id查询
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file1.getFileName(), fileNameBase + "A" );

        // 通过文件路径查询
        ScmFile file2 = ScmFactory.File.getInstanceByPath( ws,
                dirName + "/" + fileNameBase + "A" );
        Assert.assertEquals( file2.getFileName(), fileNameBase + "A" );
        Assert.assertEquals( file2.getDirectory().getId(), dirId );

        // 列取文件
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, cond );
        int i = 0;
        while ( cursor.hasNext() ) {
            cursor.getNext();
            i++;
        }
        Assert.assertEquals( i, 1 );
    }

    private void updateFileContent() throws ScmException {
        // 新增文件版本
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        byte[] bytes = new byte[ 1024 * 10 ];
        new Random().nextBytes( bytes );
        file1.updateContent( new ByteArrayInputStream( bytes ) );

        // 获取文件检查结果
        ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file2.getSize(), 1024 * 10 );
    }

    private void updaetFileAttr() throws ScmException {
        // 获取文件
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );

        // 更新属性
        file1.setFileName( fileNameBase + "B" );
        file1.setAuthor( fileNameBase );
        ScmDirectory directory = ScmFactory.Directory.getInstance( ws, "/" );
        file1.setDirectory( directory.getId() );

        // 获取文件检查结果
        ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file2.getFileName(), fileNameBase + "B" );
        Assert.assertEquals( file2.getDirectory().getId(), directory.getId() );
    }

    private void crudBatch() throws ScmException {
        // 创建批次
        ScmBatch batch = ScmFactory.Batch.createInstance( ws, batchIdA );
        batch.setName( batchName );
        batch.save();

        // 批次关联文件
        batch.attachFile( fileId );

        // 获取批次
        ScmBatch getBatch = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchIdA, false ) );
        Assert.assertEquals( getBatch.listFiles().size(), 1 );

        // 批次解除关联
        batch.detachFile( fileId );

        // 删除批次
        ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdA, false ) );
    }

    private void deleteFile() throws ScmException {
        // 删除文件
        ScmFactory.File.deleteInstance( ws, fileId, true );
    }

    private void crudDir() throws ScmException {
        String dirName = "/dir3134A";
        ScmFactory.Directory.createInstance( ws, dirName );
        long count = ScmFactory.Directory.countInstance( ws,
                new BasicBSONObject() );
        Assert.assertEquals( count, 3 );

        ScmDirectory directory = ScmFactory.Directory.getInstance( ws,
                dirName );
        Assert.assertEquals( directory.getPath(), dirName + "/" );

        ScmCursor< ScmDirectory > cursor = ScmFactory.Directory
                .listInstance( ws, new BasicBSONObject() );
        int i = 0;
        while ( cursor.hasNext() ) {
            cursor.getNext();
            i++;
        }
        Assert.assertEquals( i, 3 );

        ScmFactory.Directory.deleteInstance( ws, dirName );

        boolean exist = ScmFactory.Directory.isInstanceExist( ws, dirName );
        Assert.assertEquals( exist, false );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null )
            session.close();
    }
}
