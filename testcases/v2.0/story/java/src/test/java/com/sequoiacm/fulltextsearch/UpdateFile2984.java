package com.sequoiacm.fulltextsearch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.TestTools.LocalFile.FileType;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description SCM-2984:更新文件创建索引
 * @author wuyan
 * @Date 2020.09.14
 * @version 1.00
 */

public class UpdateFile2984 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2984";
    private String wsName = null;
    private String dirName = "/CreateDir2984";
    private ScmDirectory dir;

    @BeforeClass
    private void setUp() throws Exception {
        sessionM = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        wsM = ScmFactory.Workspace.getWorkspace( wsName, sessionM );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "name", fileName );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );

        dir = ScmFactory.Directory.createInstance( ws, dirName );
        String filePath = TestTools.LocalFile.getRandomFile();
        fileId = createFile( ws, dir, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        String filePath = TestTools.LocalFile.getFileByType( FileType.PNG );
        ScmFile file = ScmFactory.File.getInstance( wsM, fileId );
        file.setDirectory( dir );
        file.setMimeType( MimeType.PNG );
        file.updateContent( filePath );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 2 );

        // 全文检索
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "name", fileName );
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                matcher, matcher );
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_ALL,
                new BasicBSONObject(), new BasicBSONObject() );
        runSuccess = true;

    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
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
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private ScmId createFile( ScmWorkspace ws, ScmDirectory dir,
            String filePath ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setDirectory( dir );
        file.setContent( filePath );
        ScmId fileId = file.save();
        return fileId;
    }
}
