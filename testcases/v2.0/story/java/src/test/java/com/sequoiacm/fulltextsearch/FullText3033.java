package com.sequoiacm.fulltextsearch;

import java.io.File;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextHighlightOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
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
 * @Description: SCM-3033 :: 指定查询范围和带高亮显示，全文检索
 * @author fanyu
 * @Date:2020/11/16
 * @version:1.0
 */
public class FullText3033 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3033";
    private ScmId fileId = null;
    private File localPath = null;
    private String content = "text ";
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath, content, 50 );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        // 创建文件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileId = file.save();
        file.updateContent( filePath );

        // 创建索引
        ScmFactory.Fulltext.createIndex( ws, new ScmFulltextOption(
                new BasicBSONObject(), ScmFulltextMode.sync ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        // 所有版本
        ScmFulltextHighlightOption option1 = new ScmFulltextHighlightOption();
        option1.setFragmentSize( content.length() - 1 );
        ScmCursor< ScmFulltextSearchResult > actFulltextResults1 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_ALL ).match( content )
                .highlight( option1 ).search();
        checkRestult( actFulltextResults1, 2, option1.getNumOfFragments(),
                "[<em>text</em>, <em>text</em>, <em>text</em>, <em>text</em>, <em>text</em>]" );

        // 当前版本
        ScmFulltextHighlightOption option2 = new ScmFulltextHighlightOption();
        option2.setFragmentSize( content.length() - 1 );
        option2.setNumOfFragments( 1 );
        ScmCursor< ScmFulltextSearchResult > actFulltextResults2 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_CURRENT ).match( content )
                .highlight( option2 ).search();
        checkRestult( actFulltextResults2, 1, option2.getNumOfFragments(),
                "[<em>text</em>]" );

        // 历史版本
        ScmFulltextHighlightOption option3 = new ScmFulltextHighlightOption();
        option3.setFragmentSize( content.length() - 1 );
        option3.setNumOfFragments( 2 );
        ScmCursor< ScmFulltextSearchResult > actFulltextResults3 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_HISTORY ).match( content )
                .highlight( option3 ).search();
        checkRestult( actFulltextResults3, 1, option3.getNumOfFragments(),
                "[<em>text</em>, <em>text</em>]" );

        // 历史版本
        ScmFulltextHighlightOption option4 = new ScmFulltextHighlightOption();
        option4.setFragmentSize( -1 );
        option4.setNumOfFragments( -1 );
        ScmCursor< ScmFulltextSearchResult > actFulltextResults4 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_HISTORY ).match( content )
                .highlight( option4 ).search();
        String highLightStr = "[<em>text</em> <em>text</em> <em>text</em> <em>text</em> <em>text</em> "
                + "<em>text</em> <em>text</em> <em>text</em> <em>text</em> <em>text</em>]";
        checkRestult( actFulltextResults4, 1, 1, highLightStr );

        // 历史版本
        ScmFulltextHighlightOption option5 = new ScmFulltextHighlightOption();
        option5.setFragmentSize( 0 );
        option5.setNumOfFragments( 0 );
        ScmCursor< ScmFulltextSearchResult > actFulltextResults5 = ScmFactory.Fulltext
                .simpleSeracher( ws ).fileCondition( new BasicBSONObject() )
                .scope( ScmType.ScopeType.SCOPE_HISTORY ).match( content )
                .highlight( option5 ).search();
        String highLightStr1 = "[<em>text</em> <em>text</em> <em>text</em> <em>text</em> <em>text</em> "
                + "<em>text</em> <em>text</em> <em>text</em> <em>text</em> <em>text</em>]";
        checkRestult( actFulltextResults5, 1, 1, highLightStr1 );
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

    private void checkRestult(
            ScmCursor< ScmFulltextSearchResult > actFulltextResults, int expNum,
            int highLightSize, String highLightStr ) throws ScmException {
        int i = 0;
        while ( actFulltextResults.hasNext() ) {
            i++;
            ScmFulltextSearchResult actResult = actFulltextResults.getNext();
            Assert.assertNotNull( actResult.getScore() );
            Assert.assertEquals( actResult.getFileBasicInfo().getFileId().get(),
                    fileId.get() );
            List< String > expHighlight = actResult.getHighlightTexts();
            Assert.assertEquals( expHighlight.size(), highLightSize );
            Assert.assertEquals( expHighlight.toString(), highLightStr );
        }
        Assert.assertEquals( i, expNum );
    }
}