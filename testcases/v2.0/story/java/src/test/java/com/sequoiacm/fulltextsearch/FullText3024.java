package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
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
 * @Description: SCM-3024 :: 工作区索引状态为none或者deleting，工作区更新索引
 * @author fanyu
 * @Date:2020/11/12
 * @version:1.0
 */
public class FullText3024 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3024_";
    private List< ScmId > fileIdList = new ArrayList<>();
    private String filePath = null;
    private int fileNum = 50;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        filePath = TestTools.LocalFile
                .getFileByType( TestTools.LocalFile.FileType.DOC );
    }

    @Test
    private void test() throws Exception {
        // 工作区索引状态是NONE,工作区更新索引
        try {
            ScmFactory.Fulltext.alterIndex( ws,
                    new ScmFulltextModifiler()
                            .newFileCondition( new BasicBSONObject() )
                            .newMode( ScmFulltextMode.async ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                throw e;
            }
        }

        // 工作区索引状态为DELETING,工作区更新索引
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileNameBase + i,
                    filePath );
            fileIdList.add( fileId );
        }
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        ScmFactory.Fulltext.dropIndex( ws );
        try {
            ScmFactory.Fulltext.alterIndex( ws, new ScmFulltextModifiler()
                    .newFileCondition( new BasicBSONObject() ) );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FULL_TEXT_INDEX_IS_DELETING
                    && e.getError() != ScmError.FULL_TEXT_INDEX_DISABLE ) {
                throw e;
            }
        }

        // 获取工作区索引状态
        ScmFulltexInfo info = ScmFactory.Fulltext.getIndexInfo( ws );
        Assert.assertNull( info.getMode() );
        Assert.assertNull( info.getJobInfo() );
        Assert.assertNull( info.getFileMatcher() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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
}