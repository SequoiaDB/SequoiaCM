package com.sequoiacm.directory.serial;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-3133:关闭目录功能，创建/查询/删除/新增文件版本/更新文件属性/目录相关操作
 * @author fanyu
 * @Date:2020/10/20
 * @version:1.0
 */

public class EnableDir3133 extends TestScmBase {
    private String wsName = "ws3133";
    private String batchName = "batch3133";
    private String fileNameBase = "file3133_";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String batchIdA = "2020";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        BreakpointUtil.checkDBDataSource();
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 关闭目录功能
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum(),
                ScmShardingType.YEAR, ".*", "yyyy", false, false );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        Assert.assertEquals( ws.isEnableDirectory(), false );
        Assert.assertEquals( ws.getBatchShardingType(), ScmShardingType.YEAR );
    }

    @Test(groups = { GroupTags.base })
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
        // 创建文件
        for ( int i = 0; i < 10; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            if ( i % 2 == 0 ) {
                file.setFileName( fileNameBase + "A" );
            } else {
                file.setFileName( fileNameBase + "B" );
            }
            file.setAuthor( fileNameBase );
            file.setContent( new ByteArrayInputStream( bytes ) );
            fileIdList.add( file.save() );
        }

        // 创建文件，设置目录属性
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileNameBase + "A" );
        file.setDirectory( "000000000000000000000001" );
        try {
            file.save();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        // 断点文件转换成普通文件: 同名和不同名
        for ( int i = 0; i < 3; i++ ) {
            ScmBreakpointFile breakpointFile1 = null;
            if ( i % 2 == 0 ) {
                breakpointFile1 = ScmFactory.BreakpointFile.createInstance( ws,
                        fileNameBase + "_C" );
            } else {
                breakpointFile1 = ScmFactory.BreakpointFile.createInstance( ws,
                        fileNameBase + "_D" );
            }
            InputStream inputStream1 = new ByteArrayInputStream( bytes );
            breakpointFile1.upload( inputStream1 );
            inputStream1.close();

            // 断点文件转换成普通文件
            ScmFile file1 = ScmFactory.File.createInstance( ws );
            file1.setContent( breakpointFile1 );
            file1.setFileName( fileNameBase + "E" );
            file1.setAuthor( fileNameBase );
            fileIdList.add( file1.save() );
        }
    }

    private void queryFile() throws ScmException {
        // 通过id查询
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileIdList.get( 0 ) );
        Assert.assertEquals( file1.getFileName(), fileNameBase + "A" );

        // 通过文件路径查询
        try {
            ScmFactory.File.getInstanceByPath( ws, "/" + fileNameBase + "B" );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        try {
            ScmFactory.File.getInstanceByPath( ws, "/" + fileNameBase + "B", 1,
                    0 );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

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
        Assert.assertEquals( i, fileIdList.size() );
    }

    private void updateFileContent() throws ScmException {
        // 新增文件版本
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileIdList.get( 1 ) );
        byte[] bytes = new byte[ 1024 * 10 ];
        new Random().nextBytes( bytes );
        file1.updateContent( new ByteArrayInputStream( bytes ) );

        // 获取文件检查结果
        ScmFile file2 = ScmFactory.File.getInstance( ws, fileIdList.get( 1 ) );
        Assert.assertEquals( file2.getSize(), 1024 * 10 );
    }

    private void updaetFileAttr() throws ScmException {
        // 获取文件
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileIdList.get( 0 ) );

        // 更新非目录属性
        file1.setFileName( fileNameBase + "B" );
        file1.setAuthor( fileNameBase );

        // 更新目录属性
        try {
            file1.setDirectory( "000000000000000000000012" );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }
    }

    private void crudBatch() throws ScmException {
        // 创建批次
        ScmBatch batch = ScmFactory.Batch.createInstance( ws, batchIdA );
        batch.setName( batchName );
        batch.save();

        // 批次关联文件
        for ( ScmId fileId : fileIdList ) {
            batch.attachFile( fileId );
        }

        // 获取批次
        ScmBatch getBatch = ScmFactory.Batch.getInstance( ws,
                new ScmId( batchIdA, false ) );
        Assert.assertEquals( getBatch.listFiles().size(), fileIdList.size() );

        // 批次解除关联
        for ( ScmId fileId : fileIdList ) {
            batch.detachFile( fileId );
        }

        // 删除批次
        ScmFactory.Batch.deleteInstance( ws, new ScmId( batchIdA, false ) );
    }

    private void deleteFile() throws ScmException {
        for ( ScmId fileId : fileIdList ) {
            // 删除文件
            ScmFactory.File.deleteInstance( ws, fileId, true );
        }
    }

    private void crudDir() throws ScmException {
        try {
            ScmFactory.Directory.createInstance( ws, "/" + fileNameBase );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        try {
            ScmFactory.Directory.countInstance( ws, new BasicBSONObject() );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        try {
            ScmFactory.Directory.deleteInstance( ws, "/" + fileNameBase );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        try {
            ScmFactory.Directory.getInstance( ws, "/" );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        try {
            ScmFactory.Directory.isInstanceExist( ws, "/" );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        try {
            ScmFactory.Directory.listInstance( ws, new BasicBSONObject() );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }

        ScmFile file = ScmFactory.File.getInstance( ws, fileIdList.get( 0 ) );
        try {
            file.getDirectory();
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_FEATURE_DISABLE ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( session != null ) {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            session.close();
        }
    }
}
