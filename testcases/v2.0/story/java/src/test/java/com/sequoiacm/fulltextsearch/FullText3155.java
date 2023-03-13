package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFileFulltextInfo;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-3155 :: 非空工作区且工作区索引状态为created，有消息等待被消费，工作区删除索引
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3155 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String filePath = null;
    private String fileNameBase = "file3155_";
    private int fileNum = 20;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.DOC );
        // 创建多个多版本文件
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    filePath );
            fileIdList.add( fileId );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.updateContent( filePath );
        }
    }

    @Test
    private void test() throws Exception {
        // 工作区创建索引,模式异步
        BSONObject condition = new BasicBSONObject();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( condition, ScmFulltextMode.async ) );

        // 等待工作区索引状态：CREATED
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 增删文件
        createAndDeleteFile();

        // 工作区删除索引
        ScmFactory.Fulltext.dropIndex( ws );

        // 创建多个文件
        for ( int i = 2 * fileNum; i < 3 * fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    filePath );
            fileIdList.add( fileId );
        }

        // 等待工作区索引状态为NONE
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );

        // 检查工作区索引信息
        checkIndexInfo();

        // 检查文件索引信息
        for ( ScmId fileId : fileIdList ) {
            ScmFileFulltextInfo fileIndexInfo = ScmFactory.Fulltext
                    .getFileIndexInfo( ws, fileId );
            Assert.assertEquals( fileIndexInfo.getStatus(),
                    ScmFileFulltextStatus.NONE );
        }

        // 全文检索
        try {
            ScmFactory.Fulltext.simpleSeracher( ws )
                    .fileCondition( new BasicBSONObject() ).match( "test" )
                    .search();
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private void checkIndexInfo() throws ScmException {
        // 获取工作区索引信息
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertNull( indexInfo.getMode() );
        Assert.assertNull( indexInfo.getFileMatcher() );
        Assert.assertNull( indexInfo.getFulltextLocation() );
        Assert.assertNull( indexInfo.getJobInfo() );
    }

    private void createAndDeleteFile() throws ScmException {
        // 删除文件
        for ( int i = 0; i < fileNum / 2; i++ ) {
            ScmFactory.File.deleteInstance( ws, fileIdList.remove( i ), true );
        }

        // 创建多个文件
        for ( int i = fileNum; i < 2 * fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    filePath );
            fileIdList.add( fileId );
        }

        // 删除文件
        for ( int i = 1 * fileIdList.size() / 3; i < fileIdList.size(); i++ ) {
            ScmFactory.File.deleteInstance( ws, fileIdList.remove( i ), true );
        }
    }
}