package com.sequoiacm.fulltextsearch;

import java.util.*;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextJobInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description: SCM-3021 :: 工作区索引状态为created，有文件创建过索引且有消息等待被消费，工作区更新索引
 * @author fanyu
 * @Date:2020/11/11
 * @version:1.0
 */
public class FullText3021 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private Map< String, List< ScmId > > fileIdMap1 = null;
    private Map< String, List< ScmId > > fileIdMap2 = null;
    private String filePath = null;
    private String rootDirId = null;
    private String dirId = null;
    private String dirName = "/dir3021";
    private String fileNameBase = "file3021_";
    private int fileNum = 20;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        rootDirId = ScmFactory.Directory.getInstance( ws, "/" ).getId();
        dirId = ScmFactory.Directory.createInstance( ws, dirName ).getId();
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.DOC );
        fileIdMap1 = prepareFile( fileNameBase, rootDirId, fileNum );
        fileIdMap2 = prepareFile( fileNameBase, dirId, fileNum );
    }

    @Test
    private void test() throws Exception {
        // 更新前后索引条件不相同
        BSONObject condition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        BSONObject modified1 = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( dirId ).get();
        test1( condition1, modified1 );

        // 更新前后索引条件相同
        BSONObject condition2 = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        BSONObject modified2 = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        test1( condition2, modified2 );
        runSuccess = true;
    }

    private void test1( BSONObject condition, BSONObject modified )
            throws Exception {
        // 工作区创建索引
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( condition, ScmFulltextMode.async ) );

        // 等待工作区索引状态为CREATED
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 增删改查旧文件,制造有消息等待消费的场景
        crudFile();

        // 工作区更新索引
        ScmFactory.Fulltext.alterIndex( ws,
                new ScmFulltextModifiler().newFileCondition( modified ) );

        // 等待工作区索引状态为CREATED
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 检查结果
        List< String > fileIdStrList = new ArrayList<>();
        if ( condition.toString().equals( modified.toString() ) ) {
            checkIndexInfo( modified );
            List< ScmId > fileIdList = fileIdMap1.get( "success" );
            for ( ScmId fileId : fileIdList ) {
                fileIdStrList.add( fileId.get() );
            }

        } else {
            checkIndexInfo( modified );
            List< ScmId > fileIdList = fileIdMap2.get( "success" );
            for ( ScmId fileId : fileIdList ) {
                fileIdStrList.add( fileId.get() );
            }
        }
        BSONObject expCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdStrList )
                .get();
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject(), expCondition );

        // 删除索引
        ScmFactory.Fulltext.dropIndex( ws );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( List< ScmId > fileIdList : fileIdMap1.values() ) {
                    for ( ScmId fileId : fileIdList ) {
                        ScmFactory.File.deleteInstance( ws, fileId, true );
                    }
                }
                for ( List< ScmId > fileIdList : fileIdMap2.values() ) {
                    for ( ScmId fileId : fileIdList ) {
                        ScmFactory.File.deleteInstance( ws, fileId, true );
                    }
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

    private void checkIndexInfo( BSONObject expCondition ) throws Exception {
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertEquals( indexInfo.getFileMatcher(), expCondition );
        Assert.assertNotNull( indexInfo.getFulltextLocation() );
        Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
        Assert.assertEquals( indexInfo.getStatus(), ScmFulltextStatus.CREATED );

        ScmFulltextJobInfo jodInfo = indexInfo.getJobInfo();
        while ( jodInfo.getProgress() != 100 ) {
            jodInfo = ScmFactory.Fulltext.getIndexInfo( ws ).getJobInfo();
        }
    }

    private void crudFile() throws Exception {
        List< ScmId > fileIdList1 = fileIdMap1.get( "success" );
        // 创建文件和新增文件版本，符合工作区条件 正确的文件类型
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setDirectory( rootDirId );
            file.setContent( filePath );
            fileIdList1.add( file.save() );
            file.updateContent( filePath );
        }

        List< ScmId > fileIdList2 = fileIdMap1.get( "fail" );
        // 创建文件和新增文件版本，符合工作区条件 不正确的文件类型
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setMimeType( MimeType.FRACTALS );
            file.setDirectory( rootDirId );
            file.setContent( filePath );
            fileIdList2.add( file.save() );
            file.updateContent( filePath );
        }

        // 更新旧文件属性，由符合条件更新为不符合条件
        List< ScmId > fileIdList3 = fileIdMap2.get( "success" );
        int size1 = fileIdList1.size() / 4;
        for ( int i = 0; i < size1; i++ ) {
            ScmId fileId = fileIdList1.remove( i );
            ScmFile updateFile = ScmFactory.File.getInstance( ws, fileId );
            updateFile.setDirectory( dirId );
            fileIdList3.add( fileId );
        }

        // 删除旧文件
        int size2 = fileIdList1.size() / 4;
        for ( int i = 0; i < size2; i++ ) {
            ScmFactory.File.deleteInstance( ws, fileIdList1.remove( i ), true );
        }
    }

    private Map< String, List< ScmId > > prepareFile( String fileNameBase,
            String dirId, int fileNum ) throws Exception {
        Map< String, List< ScmId > > map = new HashMap<>();
        List< ScmId > fileIdList1 = new ArrayList<>();
        // 支持的文件类型
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setContent( filePath );
            file.setDirectory( dirId );
            fileIdList1.add( file.save() );
            file.updateContent( filePath );
        }
        map.put( "success", fileIdList1 );

        // 不支持的文件类型
        List< ScmId > fileIdList2 = new ArrayList<>();
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + UUID.randomUUID() );
            file.setMimeType( MimeType.ENVOY );
            file.setContent( TestTools.LocalFile.getRandomFile() );
            file.setDirectory( dirId );
            fileIdList2.add( file.save() );
            file.updateContent( TestTools.LocalFile.getRandomFile() );
        }
        map.put( "fail", fileIdList2 );
        return map;
    }
}