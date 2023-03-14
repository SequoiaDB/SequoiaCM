package com.sequoiacm.fulltextsearch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.common.MimeType;
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

/**
 * @Description: SCM-2985 :: 更新文件创建索引，其中历史版本文件未创建索引
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText2985 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file2985";
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath, "黎明前的黑暗", 1024 );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).greaterThan( 1024 * 200 )
                .get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Throwable {
        // 创建文件,不符合工作区索引条件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setMimeType( MimeType.PLAIN );
        file.setContent( filePath );
        fileId = file.save();

        // 检查结果
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 0 );

        // 更新文件, 符合工作区索引条件
        ScmFile updateFile = ScmFactory.File.getInstance( ws, fileId );
        byte[] bytes = new byte[ 1024 * 300 ];
        new Random().nextBytes( bytes );
        updateFile.updateContent( new ByteArrayInputStream( bytes ) );

        // 检索文件
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).greaterThanEquals( 0 )
                .get();
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 2 );
        FullTextUtils.searchAndCheckResults( ws, ScmType.ScopeType.SCOPE_ALL,
                fileCondition, fileCondition );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.File.deleteInstance( ws, fileId, true );
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
        }
    }
}