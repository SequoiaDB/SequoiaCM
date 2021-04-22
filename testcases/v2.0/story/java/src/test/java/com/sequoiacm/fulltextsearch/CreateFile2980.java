package com.sequoiacm.fulltextsearch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description SCM-2980:新建文件建立索引，删除文件(file_matcher条件覆盖一个字段/多个字段)
 * @author wuyan
 * @Date 2020.09.14
 * @version 1.00
 */

public class CreateFile2980 extends TestScmBase {
    @DataProvider(name = "fileMatcherConds")
    public Object[][] generatePageSize() {
        // 创建全文索引,匹配条件为一个字段
        BSONObject matcherA = new BasicBSONObject();
        matcherA.put( "name", fileName );
        // 创建全文索引,匹配条件为多个字段
        BSONObject matcherB = new BasicBSONObject();
        matcherB.put( "name", fileName );
        matcherB.put( "author", fileName );
        return new Object[][] {
                // the parameter : matcher
                new Object[] { matcherA }, new Object[] { matcherB } };
    }

    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private ScmId fileId = null;
    private String fileName = "file2980";
    private String wsName = null;

    @BeforeClass
    private void setUp() throws Exception {
        sessionM = TestScmTools.createSession( ScmInfo.getRootSite() );
        sessionB = TestScmTools.createSession( ScmInfo.getBranchSite() );
        wsName = WsPool.get();
        wsM = ScmFactory.Workspace.getWorkspace( wsName, sessionM );
        wsB = ScmFactory.Workspace.getWorkspace( wsName, sessionB );
    }

    @Test(dataProvider = "fileMatcherConds", groups = { "twoSite", "fourSite" })
    private void test( BSONObject matcher ) throws Exception {
        createIndexAndFile( wsM, matcher );
        createIndexAndFile( wsB, matcher );
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( TestScmBase.forceClear ) {
                ScmFactory.Fulltext.dropIndex( wsM );
                FullTextUtils.waitWorkSpaceIndexStatus( wsM,
                        ScmFulltextStatus.NONE );
            }
        } finally {
            if ( wsName != null ) {
                WsPool.release( wsName );
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void createIndexAndFile( ScmWorkspace ws, BSONObject matcher )
            throws Exception {
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
        String filePath = TestTools.LocalFile.getRandomFile();
        fileId = ScmFileUtils.create( ws, fileName, filePath );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 1 );

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                matcher, matcher );
        // 删除文件
        ScmFactory.File.deleteInstance( ws, fileId, true );
        // 全文检索
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_ALL,
                new BasicBSONObject(), new BasicBSONObject() );

        ScmFactory.Fulltext.dropIndex( ws );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.NONE );
    }
}
