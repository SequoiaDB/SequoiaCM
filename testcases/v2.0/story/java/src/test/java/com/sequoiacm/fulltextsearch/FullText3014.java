package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextJobInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-3014 :: 有存量数据，索引模式为异步，工作区创建索引后，增删改文件
 * @author fanyu
 * @Date:2020/11/10
 * @version:1.0
 */
public class FullText3014 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3014_";
    private String rootDirId = null;
    private String dirId = null;
    private String dirName = "/dir3014";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private BSONObject fileCondition = null;
    private int fileNum = 50;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 获取根目录id
        rootDirId = ScmFactory.Directory.getInstance( ws, "/" ).getId();
        dirId = ScmFactory.Directory.createInstance( ws, dirName ).getId();
    }

    @Test
    private void test() throws Exception {
        // 存量数据，符合索引条件, 其中一半文件类型正确，一半文件类型不正确
        prepareFile( fileNameBase, rootDirId, fileNum );

        // 创建索引
        fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 检查工作区索引信息
        checkIndexInfo();

        // 增删改查文件
        crudFile();

        // 全文检索，检查结果
        FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                fileIdList2.get( fileIdList2.size() - 1 ) );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                fileIdList1.size() * 2 );
        List< String > fileIdStrList = new ArrayList<>();
        for ( ScmId fileId : fileIdList1 ) {
            fileIdStrList.add( fileId.get() );
        }
        BSONObject expFileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdStrList )
                .get();
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject(), expFileCondition );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                ScmFactory.Directory.deleteInstance( ws, dirName );
                ScmFactory.Fulltext.dropIndex( ws );
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.NONE );
            }
        } finally {
            if ( wsName != null ) {
                WsPool.release( wsName );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void crudFile() throws Exception {
        // 创建文件，符合工作区条件
        String filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.TEXT );
        ScmId fileId = ScmFileUtils.create( ws, fileNameBase + fileNum,
                filePath );
        fileIdList1.add( fileId );

        // 新增文件版本
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( filePath );

        // 更新旧文件属性，由符合条件更新为不符合条件
        ScmId updatedFileId = fileIdList1
                .remove( new Random().nextInt( fileIdList1.size() - 1 ) );
        ScmFile updateFile = ScmFactory.File.getInstance( ws, updatedFileId );
        updateFile.setDirectory( dirId );
        fileIdList2.add( updatedFileId );

        // 删除旧文件
        ScmId deletedFileId = fileIdList1
                .remove( new Random().nextInt( fileIdList1.size() - 1 ) );
        ScmFactory.File.deleteInstance( ws, deletedFileId, true );
    }

    private void checkIndexInfo() throws Exception {
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertEquals( indexInfo.getFileMatcher(), fileCondition );
        Assert.assertNotNull( indexInfo.getFulltextLocation() );
        Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
        Assert.assertEquals( indexInfo.getStatus(), ScmFulltextStatus.CREATED );
        ScmFulltextJobInfo jodInfo = indexInfo.getJobInfo();
        int times = 0;
        while ( jodInfo.getProgress() != 100 ) {
            jodInfo = ScmFactory.Fulltext.getIndexInfo( ws ).getJobInfo();
            if ( times > 60 ) {
                throw new Exception( "wait jodInfo.getProgress()=100 time out,"
                        + jodInfo.toString() );
            }
            times++;
            Thread.sleep( 1000 );
        }
        Assert.assertEquals( jodInfo.getEstimateFileCount(), fileNum * 2 );
        Assert.assertEquals( jodInfo.getErrorCount(), fileIdList2.size() * 2 );
        Assert.assertEquals( jodInfo.getSuccessCount(),
                fileIdList1.size() * 2 );
        Assert.assertEquals( jodInfo.getProgress(), 100 );
        Assert.assertNotNull( jodInfo.getSpeed() );
    }

    private void prepareFile( String fileNameBase, String dirId, int fileNum )
            throws Exception {
        TestTools.LocalFile.FileType[] fileTypes = TestTools.LocalFile.FileType
                .values();
        // 支持的文件类型
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setContent( TestTools.LocalFile
                    .getFileByType( fileTypes[ i % fileTypes.length ] ) );
            file.setDirectory( dirId );
            fileIdList1.add( file.save() );
            file.updateContent( TestTools.LocalFile
                    .getFileByType( fileTypes[ i % fileTypes.length ] ) );
        }

        // 不支持的文件类型
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setMimeType( MimeType.ENVOY );
            file.setContent( TestTools.LocalFile.getRandomFile() );
            file.setDirectory( dirId );
            fileIdList2.add( file.save() );
            file.updateContent( TestTools.LocalFile.getRandomFile() );
        }
    }
}