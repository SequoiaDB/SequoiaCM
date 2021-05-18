package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
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
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsPool;
import com.sequoiacm.testcommon.scmutils.FullTextUtils;

/**
 * @Description SCM-3036:无文件创建过索引，inspect工作区
 * @author wuyan
 * @Date 2020.09.17
 * @version 1.00
 */

public class WsIndex3036 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String wsName = "ws3036";
    private List< ScmId > fileIdList = new ArrayList< >();

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test
    private void test() throws Exception {
        // 创建全文索引
        String author = "author3036";
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", author );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        ScmFactory.Fulltext.inspectIndex( ws );

        String fileNamePrefix = "file3036_";
        int fileNum = 20;
        fileIdList = createFiles( fileNamePrefix, author, fileNum );

        // 删除5个旧文件、更新10个旧文件不匹配索引
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            if ( i >= 5 && i < 15 ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setAuthor( "nomatcher3036" );
            } else if ( i >= 15 ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }
        ScmFactory.Fulltext.inspectIndex( ws );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 5 );

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                matcher, matcher );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < 15; i++ ) {
                    ScmId fileId = fileIdList.get( i );
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private List< ScmId > createFiles( String fileNamePrefix, String author,
            int fileNum ) throws Exception {
        List< ScmId > fileIdList = new ArrayList< >();
        for ( int i = 0; i < fileNum; i++ ) {
            String filePath = TestTools.LocalFile.getRandomFile();
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNamePrefix + "_" + i );
            file.setAuthor( author );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
        return fileIdList;
    }
}
