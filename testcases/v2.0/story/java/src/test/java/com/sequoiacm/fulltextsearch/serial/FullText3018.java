package com.sequoiacm.fulltextsearch.serial;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-3018 :: 多个工作区创建索引
 * @author fanyu
 * @Date:2020/11/11
 * @version:1.0
 */
public class FullText3018 extends TestScmBase {
    private AtomicInteger runSuccessFlag = new AtomicInteger( 0 );
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName1 = null;
    private String wsName2 = null;
    private String wsName3 = null;
//    private String wsName4 = null;
//    private String wsName5 = null;
//    private String wsName6 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;
    private ScmWorkspace ws3 = null;
//    private ScmWorkspace ws4 = null;
//    private ScmWorkspace ws5 = null;
//    private ScmWorkspace ws6 = null;
    private List< ScmId > fileIdList1 = null;
    private List< ScmId > fileIdList2 = null;
    private List< ScmId > fileIdList3 = null;
//    private List< ScmId > fileIdList4 = null;
//    private List< ScmId > fileIdList5 = null;
//    private List< ScmId > fileIdList6 = null;
    private String fileNameBase = "file3018_";
    private int fileNum = 30;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName1 = WsPool.get();
        wsName2 = WsPool.get();
        wsName3 = WsPool.get();
//        wsName4 = WsPool.get();
//        wsName5 = WsPool.get();
//        wsName6 = WsPool.get();
        ws1 = ScmFactory.Workspace.getWorkspace( wsName1, session );
        ws2 = ScmFactory.Workspace.getWorkspace( wsName2, session );
        ws3 = ScmFactory.Workspace.getWorkspace( wsName3, session );
//        ws4 = ScmFactory.Workspace.getWorkspace( wsName4, session );
//        ws5 = ScmFactory.Workspace.getWorkspace( wsName5, session );
//        ws6 = ScmFactory.Workspace.getWorkspace( wsName6, session );
        fileIdList1 = prepareFile( ws1 );
        fileIdList2 = prepareFile( ws2 );
        fileIdList3 = prepareFile( ws3 );
//        fileIdList4 = prepareFile( ws4 );
//        fileIdList5 = prepareFile( ws5 );
//        fileIdList6 = prepareFile( ws6 );
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        return new Object[][] { { wsName1, fileIdList1 },
                { wsName2, fileIdList2 }, { wsName3, fileIdList3 }/*,
                { wsName4, fileIdList4 }, { wsName5, fileIdList5 },
                { wsName6, fileIdList6 } */};
    }

    @Test(dataProvider = "dataProvider")
    private void test( String wsName, List< ScmId > fileIdList )
            throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
        // 等待索引建立
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        // 检查索引信息
        checkIndexInfo( ws, new BasicBSONObject() );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                fileIdList.size() );
        // 增删改查文件
        crudFile( ws, fileIdList );
        // 全文检索，检查结果
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject(), new BasicBSONObject() );
        runSuccessFlag.incrementAndGet();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccessFlag.get() == generateRangData().length
                    || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws1, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws2, fileId, true );
                }
                for ( ScmId fileId : fileIdList3 ) {
                    ScmFactory.File.deleteInstance( ws3, fileId, true );
                }
//                for ( ScmId fileId : fileIdList4 ) {
//                    ScmFactory.File.deleteInstance( ws4, fileId, true );
//                }
//                for ( ScmId fileId : fileIdList5 ) {
//                    ScmFactory.File.deleteInstance( ws5, fileId, true );
//                }
//                for ( ScmId fileId : fileIdList6 ) {
//                    ScmFactory.File.deleteInstance( ws6, fileId, true );
//                }
                ScmFactory.Fulltext.dropIndex( ws1 );
                ScmFactory.Fulltext.dropIndex( ws2 );
                ScmFactory.Fulltext.dropIndex( ws3 );
//                ScmFactory.Fulltext.dropIndex( ws4 );
//                ScmFactory.Fulltext.dropIndex( ws5 );
//                ScmFactory.Fulltext.dropIndex( ws6 );
                FullTextUtils.waitWorkSpaceIndexStatus( ws1,
                        ScmFulltextStatus.NONE );
                FullTextUtils.waitWorkSpaceIndexStatus( ws2,
                        ScmFulltextStatus.NONE );
                FullTextUtils.waitWorkSpaceIndexStatus( ws3,
                        ScmFulltextStatus.NONE );
//                FullTextUtils.waitWorkSpaceIndexStatus( ws4,
//                        ScmFulltextStatus.NONE );
//                FullTextUtils.waitWorkSpaceIndexStatus( ws5,
//                        ScmFulltextStatus.NONE );
//                FullTextUtils.waitWorkSpaceIndexStatus( ws6,
//                        ScmFulltextStatus.NONE );
            }
        } finally {
            if ( wsName1 != null ) {
                WsPool.release( wsName1 );
            }
            if ( wsName2 != null ) {
                WsPool.release( wsName2 );
            }
            if ( wsName3 != null ) {
                WsPool.release( wsName3 );
            }
//            if ( wsName4 != null ) {
//                WsPool.release( wsName4 );
//            }
//            if ( wsName5 != null ) {
//                WsPool.release( wsName5 );
//            }
//            if ( wsName6 != null ) {
//                WsPool.release( wsName6 );
//            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void crudFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws Exception {
        // 创建文件，符合工作区条件
        String filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.DOCX );
        ScmId fileId = ScmFileUtils.create( ws, fileNameBase + fileNum,
                filePath );
        fileIdList.add( fileId );

        // 新增文件版本
        for ( ScmId updateFileId : fileIdList ) {
            ScmFile file = ScmFactory.File.getInstance( ws, updateFileId );
            file.updateContent( filePath );
        }

        // 更新旧文件属性
        ScmId updatedFileId = fileIdList
                .remove( new Random().nextInt( fileIdList.size() - 1 ) );
        ScmFile updateFile = ScmFactory.File.getInstance( ws, updatedFileId );
        updateFile.setAuthor( fileNameBase );
        fileIdList.add( updatedFileId );

        // 删除旧文件
        ScmId deletedFileId = fileIdList
                .remove( new Random().nextInt( fileIdList.size() - 1 ) );
        ScmFactory.File.deleteInstance( ws, deletedFileId, true );
    }

    private void checkIndexInfo( ScmWorkspace ws, BSONObject fileCondition )
            throws Exception {
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertEquals( indexInfo.getFileMatcher(), fileCondition );
        Assert.assertNotNull( indexInfo.getFulltextLocation() );
        Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
        ScmFulltextJobInfo jodInfo = indexInfo.getJobInfo();
        long expCount = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, fileCondition );
        while ( jodInfo.getProgress() != 100 ) {
            jodInfo = ScmFactory.Fulltext.getIndexInfo( ws ).getJobInfo();
        }
        try {
            Assert.assertEquals( jodInfo.getEstimateFileCount(), expCount );
            Assert.assertEquals( jodInfo.getErrorCount(), 0 );
            Assert.assertEquals( jodInfo.getSuccessCount(), expCount );
            Assert.assertEquals( jodInfo.getProgress(), 100 );
            Assert.assertNotNull( jodInfo.getSpeed() );
        } catch ( AssertionError e ) {
            throw new Exception( "jodInfo = " + jodInfo.toString(), e );
        }
    }

    private List< ScmId > prepareFile( ScmWorkspace ws ) throws Exception {
        List< ScmId > fileIdList = new ArrayList<>();
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    TestTools.LocalFile.getFileByType(
                            TestTools.LocalFile.FileType.DOCX ) );
            fileIdList.add( fileId );
        }
        return fileIdList;
    }
}