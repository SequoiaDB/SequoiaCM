package com.sequoiacm.fulltextsearch;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description SCM-2989 :: 跨站点更新文件内容
 * @author wuyan
 * @Date 2020.09.14
 * @version 1.00
 */

public class UpdateFile2989 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private final int branSitesNum = 2;
    private List< SiteWrapper > branSites = null;
    private ScmId fileId = null;
    private String fileName = "file2989";
    private String wsName = null;

    @BeforeClass
    private void setUp() throws Exception {
        branSites = ScmInfo.getBranchSites( branSitesNum );
        sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
        sessionB = ScmSessionUtils.createSession( branSites.get( 1 ) );
        wsName = WsPool.get();
        wsA = ScmFactory.Workspace.getWorkspace( wsName, sessionA );
        wsB = ScmFactory.Workspace.getWorkspace( wsName, sessionB );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "name", fileName );
        ScmFactory.Fulltext.createIndex( wsA,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        String filePath = TestTools.LocalFile.getRandomFile();
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        ScmFile file = ScmFactory.File.getInstance( wsB, fileId );
        file.updateContent( filePath );
        FullTextUtils.waitFilesStatus( wsB, ScmFileFulltextStatus.CREATED, 2 );

        // 全文检索
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "name", fileName );
        FullTextUtils.searchAndCheckResults( wsA, ScopeType.SCOPE_CURRENT,
                matcher, matcher );
        BSONObject matcherA = new BasicBSONObject();
        matcherA.put( "size", file.getSize() );
        FullTextUtils.searchAndCheckResults( wsA, ScopeType.SCOPE_HISTORY,
                matcherA, matcherA );
        FullTextUtils.searchAndCheckResults( wsA, ScopeType.SCOPE_ALL, matcherA,
                new BasicBSONObject() );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                ScmFactory.Fulltext.dropIndex( wsA );
                FullTextUtils.waitWorkSpaceIndexStatus( wsA,
                        ScmFulltextStatus.NONE );
            }
        } finally {
            if ( wsName != null ) {
                WsPool.release( wsName );
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }
}
