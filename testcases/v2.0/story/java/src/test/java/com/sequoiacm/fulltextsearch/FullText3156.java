package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
import java.util.List;

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
 * @Description: SCM-3156 ::工作区索引状态是creating、deleting、none等，指定文件重新建索引
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3156 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNameBase = "file3156_";
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
        // 工作区索引状态是NONE
        try {
            ScmFactory.Fulltext.rebuildFileIndex( ws, fileIdList.get( 0 ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                throw e;
            }
        }
        // 工作区索引状态为CREATING
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );

        // 工作区索引状态为DELETING
        ScmFactory.Fulltext.dropIndex( ws );
        try {
            ScmFactory.Fulltext.rebuildFileIndex( ws, fileIdList.get( 0 ) );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_IS_DELETING ) {
                throw e;
            }
        }
        // 检查结果
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );

        // 检查工作区索引信息
        ScmFulltexInfo indexInfo = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertNull( indexInfo.getFulltextLocation() );
        Assert.assertNull( indexInfo.getFileMatcher() );
        Assert.assertNull( indexInfo.getJobInfo() );
        Assert.assertNull( indexInfo.getMode() );
        long count = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, new BasicBSONObject(
                        "external_data.fulltext_status", "CREATED" ) );
        Assert.assertEquals( count, 0 );
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
}