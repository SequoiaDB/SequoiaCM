package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
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
 * @Description: SCM-3017 :: 工作区索引状态为created、creating、deleting，工作区创建索引
 * @author fanyu
 * @Date:2020/11/11
 * @version:1.0
 */
public class FullText3017 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNameBase = "file3017_";
    private int fileNum = 50;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    TestTools.LocalFile.getFileByType(
                            TestTools.LocalFile.FileType.DOC ) );
            fileIdList.add( fileId );
        }
    }

    @Test
    private void test() throws Exception {
        // 工作区创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );

        // 工作区索引状态为CREATING
        try {
            ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                    new BasicBSONObject(), ScmFulltextMode.async ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_IS_CREATING && e
                    .getError() != ScmError.FULL_TEXT_INDEX_ALREADY_CREATED ) {
                throw e;
            }
        }

        // 工作区索引状态为CREATED
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        try {
            ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                    new BasicBSONObject(), ScmFulltextMode.async ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_ALREADY_CREATED ) {
                throw e;
            }
        }
        // 检查结果
        checkIndexInfo( new BasicBSONObject() );
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject(), new BasicBSONObject() );

        // 工作区索引状态为DELETING
        ScmFactory.Fulltext.dropIndex( ws );
        try {
            ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                    new BasicBSONObject(), ScmFulltextMode.async ) );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_IS_DELETING ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFulltexInfo indexInfo1 = ScmFactory.Fulltext
                        .getIndexInfo( ws );
                if ( indexInfo1.getStatus()
                        .equals( ScmFulltextStatus.CREATING ) ) {
                    FullTextUtils.waitWorkSpaceIndexStatus( ws,
                            ScmFulltextStatus.CREATED );
                    ScmFactory.Fulltext.dropIndex( ws );
                } else if ( indexInfo1.getStatus()
                        .equals( ScmFulltextStatus.CREATED ) ) {
                    ScmFactory.Fulltext.dropIndex( ws );
                }
                FullTextUtils.waitWorkSpaceIndexStatus( ws,
                        ScmFulltextStatus.NONE );
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

    private void checkIndexInfo( BSONObject fileCondition ) throws Exception {
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertEquals( indexInfo.getFileMatcher(), fileCondition );
        Assert.assertNotNull( indexInfo.getFulltextLocation() );
        Assert.assertEquals( indexInfo.getMode(), ScmFulltextMode.async );
        Assert.assertEquals( indexInfo.getStatus(), ScmFulltextStatus.CREATED );
        ScmFulltextJobInfo jodInfo = indexInfo.getJobInfo();
        long count = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, fileCondition );
        while ( jodInfo.getSuccessCount() != count ) {
            jodInfo = ScmFactory.Fulltext.getIndexInfo( ws ).getJobInfo();
        }
        try {
            Assert.assertEquals( jodInfo.getEstimateFileCount(), count );
            Assert.assertEquals( jodInfo.getErrorCount(), 0 );
            Assert.assertEquals( jodInfo.getSuccessCount(), count );
            Assert.assertEquals( jodInfo.getProgress(), 100 );
            Assert.assertNotNull( jodInfo.getSpeed() );
        } catch ( AssertionError e ) {
            throw new Exception( "jodInfo = " + jodInfo.toString(), e );
        }
    }
}