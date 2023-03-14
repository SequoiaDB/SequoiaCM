package com.sequoiacm.fulltextsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.exception.ScmException;
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
 * @Description: SCM-2995 :: 消息队列存在消息未被消费，更新文件属性
 * @author fanyu
 * @Date:2020/11/10
 * @version:1.0
 */
public class FullText2995 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = null;
    private ScmWorkspace ws = null;
    private File localPath = null;
    private String filePath = null;
    private String rootDirId = null;
    private String dirName = "/dir2995";
    private String dirId = null;
    private List< ScmId > fileIdList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + 1024 + ".txt";
        TestTools.LocalFile.createFile( filePath, "test", 1024 );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        wsName = WsPool.get();
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        // 获取根目录id
        rootDirId = ScmFactory.Directory.getInstance( ws, "/" ).getId();
        dirId = ScmFactory.Directory.createInstance( ws, dirName ).getId();
        // 创建索引
        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID ).is( rootDirId )
                .get();
        ScmFactory.Fulltext.createIndex( ws,
                new ScmFulltextOption( fileCondition, ScmFulltextMode.async ) );
        FullTextUtils.waitWorkSpaceIndexStatus( ws, ScmFulltextStatus.CREATED );
    }

    @Test
    private void test() throws Exception {
        // 创建新文件，不符合索引条件
        createFile( "file2995A_", dirId, 50 );

        // 创建新文件，符合索引条件
        createFile( "file2995B_", rootDirId, 50 );

        // 更新文件属性，使其符合索引条件
        for ( int i = 0; i < 50; i++ ) {
            ScmFile file = ScmFactory.File.getInstanceByPath( ws,
                    dirName + "/" + "file2995A_" + i );
            file.setDirectory( rootDirId );
        }

        // 检查结果
        FullTextUtils.waitFilesStatus( ws, ScmFileFulltextStatus.CREATED, 100 );
        FullTextUtils.searchAndCheckResults( ws,
                ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                new BasicBSONObject() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
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

    private void createFile( String fileNameBase, String dirId, int fileNum )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setContent( filePath );
            file.setDirectory( dirId );
            fileIdList.add( file.save() );
        }
    }
}