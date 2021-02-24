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
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
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

/**
 * @Description: SCM-3015 :: 指定匹配条件，工作区创建索引
 * @author fanyu
 * @Date:2020/11/11
 * @version:1.0
 */
public class FullText3015 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3015_";
    private String oldAuthName = "author3015_old";
    private String newAuthName = "author3015_new";
    private String rootDirId = null;
    private String dirId = null;
    private String dirName = "/dir3015";
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List<ScmId> fileIdList2 = new ArrayList<>();
    private int fileNum = 100;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 获取根目录id
        rootDirId = ScmFactory.Directory.getInstance( ws, "/" ).getId();
        dirId = ScmFactory.Directory.createInstance( ws, dirName ).getId();
        // 存量数据
        prepareFile( fileNameBase, fileNum );
    }

    @Test
    private void test() throws Exception {
        // 匹配0个文件
        BSONObject fileCondition = ScmQueryBuilder.start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                        .and( ScmAttributeName.File.AUTHOR ).is( newAuthName ).get();
        test(fileCondition);

        // 匹配部分文件
        fileCondition = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR ).is( oldAuthName )
                .and( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId ).get();
        test(fileCondition);

        // 匹配全部文件
        fileCondition = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR ).is( oldAuthName ).get();
        test(fileCondition);
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

    private void test(BSONObject fileCondition) throws Exception {
        // 创建索引
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        // 检查工作区索引信息
        checkIndexInfo(fileCondition);
        // 更新和删除文件
        updateAndDeleteFile();
        ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance( ws, ScmType.ScopeType.SCOPE_CURRENT, fileCondition );
        while ( cursor.hasNext() ){
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED, cursor.getNext().getFileId() );
        }
        // 全文检索，检查结果
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(), fileCondition );
        // 删除索引
        ScmFactory.Fulltext.dropIndex( ws );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.NONE, (int)ScmFactory.File.countInstance( ws, ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(  ) ) );
    }

    private void updateAndDeleteFile() throws Exception {
        // 删除旧文件
        ScmId deletedFileId = fileIdList1.remove( new Random(  ).nextInt( fileIdList1.size() ) );
        ScmFactory.File.deleteInstance( ws, deletedFileId, true );

        // 更新旧文件属性
        ScmId updatedFileId1 = fileIdList1.get( new Random(  ).nextInt( fileIdList1.size() ) );
        ScmFile updateFile1 = ScmFactory.File.getInstance( ws,updatedFileId1  );
        updateFile1.setDirectory( rootDirId );
        updateFile1.setAuthor( newAuthName );

        ScmId updatedFileId2 = fileIdList2.get( new Random(  ).nextInt( fileIdList1.size() ) );
        ScmFile updateFile2 = ScmFactory.File.getInstance( ws,updatedFileId2  );
        updateFile2.setDirectory( rootDirId );
        updateFile2.setAuthor( oldAuthName );
    }

    private void checkIndexInfo(BSONObject fileCondition) throws Exception {
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertEquals( indexInfo.getFileMatcher(), fileCondition );
        Assert.assertNotNull( indexInfo.getFulltextLocation() );
        Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
        Assert.assertEquals( indexInfo.getStatus(), ScmFulltextStatus.CREATED );
        ScmFulltextJobInfo jodInfo = indexInfo.getJobInfo();
        long expCount = ScmFactory.File.countInstance( ws, ScmType.ScopeType.SCOPE_CURRENT, fileCondition );
        while ( jodInfo.getSuccessCount() !=  expCount ){
            jodInfo = ScmFactory.Fulltext.getIndexInfo( ws ).getJobInfo();
        }
       try {
           Assert.assertEquals( jodInfo.getEstimateFileCount(), expCount );
           Assert.assertEquals( jodInfo.getErrorCount(), 0 );
           Assert.assertEquals( jodInfo.getSuccessCount(), expCount  );
           Assert.assertEquals( jodInfo.getProgress(), 100 );
           Assert.assertNotNull( jodInfo.getSpeed() );
       }catch ( AssertionError e ){
            throw new Exception( "jodInfo = " + jodInfo.toString(), e );
       }
    }

    private void prepareFile( String fileNameBase,int fileNum )
            throws Exception {
        for ( int i = 0; i < fileNum/2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setAuthor( oldAuthName );
            file.setContent( TestTools.LocalFile
                    .getFileByType(  TestTools.LocalFile.FileType.TEXT ) );
            file.setDirectory( rootDirId );
            fileIdList1.add( file.save() );
        }

        for ( int i = fileNum/2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setAuthor( oldAuthName );
            file.setContent( TestTools.LocalFile
                    .getFileByType(  TestTools.LocalFile.FileType.DOCX ) );
            file.setDirectory( dirId );
            fileIdList2.add( file.save() );
        }
    }
}