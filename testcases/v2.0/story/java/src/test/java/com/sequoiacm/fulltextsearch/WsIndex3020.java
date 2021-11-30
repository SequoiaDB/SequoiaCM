package com.sequoiacm.fulltextsearch;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextModifiler;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
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
 * @Description SCM-3020:工作区索引状态为created，无文件创建过索引，工作区更新索引
 * @author wuyan
 * @Date 2020.09.17
 * @version 1.00
 */

public class WsIndex3020 extends TestScmBase {
    private boolean runSuccess = false;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String author = "author3020";
    private String newAuthor = "author3020_NEW";
    private List< ScmId > fileIdListA = new ArrayList<>();
    private List< ScmId > fileIdListB = new ArrayList<>();
    private String wsName = null;

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建全文索引
        BSONObject matcher = new BasicBSONObject();
        matcher.put( "author", author );
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( matcher, ScmFulltextMode.sync ) );
    }

    @Test
    private void test() throws Exception {
        String fileNamePrefix = "file3020_";
        int fileNumA = 20;
        fileIdListA = createFiles( fileNamePrefix, newAuthor, fileNumA );

        // 更新ws的file_matcher条件
        ScmFulltextModifiler modifiler = new ScmFulltextModifiler();
        BSONObject newMatcher = new BasicBSONObject();
        newMatcher.put( "author", newAuthor );
        modifiler.newFileCondition( newMatcher );
        ScmFactory.Fulltext.alterIndex( ws, modifiler );
        for ( int i = 0; i < fileIdListA.size(); i++ ) {
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                    fileIdListA.get( i ) );
        }

        // 新增文件匹配创建索引条件
        String fileNamePrefixB = "file3020new_";
        int fileNumB = 10;
        fileIdListB = createFiles( fileNamePrefixB, newAuthor, fileNumB );

        // 删除5个旧文件、更新10个旧文件不匹配索引
        for ( int i = 0; i < fileNumA; i++ ) {
            ScmId fileId = fileIdListA.get( i );
            if ( i >= 5 && i < 15 ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.setAuthor( author );
                FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.NONE,
                        fileId );
            } else if ( i >= 15 ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }

        // 检查ws索引状态
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
        // 检查新增文件索引状态
        for ( int i = 0; i < fileNumB; i++ ) {
            ScmId fileId = fileIdListB.get( i );
            FullTextUtils.waitFileStatus( ws, ScmFileFulltextStatus.CREATED,
                    fileId );
        }
        ScmFactory.Fulltext.inspectIndex( ws );
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED,
                fileNumB + 5 );

        // 全文检索
        FullTextUtils.searchAndCheckResults( ws, ScopeType.SCOPE_CURRENT,
                new BasicBSONObject(), newMatcher );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileIdListA.size(); i++ ) {
                    ScmId fileId = fileIdListA.get( i );
                    try {
                        ScmFactory.File.deleteInstance( ws, fileId, true );
                    } catch ( ScmException e ) {
                        Assert.assertEquals( e.getError(),
                                ScmError.FILE_NOT_FOUND, e.getMessage() );
                    }
                }
                for ( int i = 0; i < fileIdListB.size(); i++ ) {
                    ScmId fileId = fileIdListB.get( i );
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

        List< ScmId > fileIdList = new ArrayList<>();
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
