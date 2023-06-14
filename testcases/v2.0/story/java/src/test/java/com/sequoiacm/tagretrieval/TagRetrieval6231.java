//package com.sequoiacm.tagretrieval;
//
//import java.io.File;
//import java.util.*;
//
//import org.bson.BSONObject;
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType;
//import com.sequoiacm.client.core.*;
//import com.sequoiacm.client.element.ScmFileBasicInfo;
//import com.sequoiacm.client.element.ScmTags;
//import com.sequoiacm.client.element.tag.ScmTagCondition;
//import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.exception.ScmError;
//import com.sequoiacm.testcommon.*;
//import com.sequoiacm.testcommon.listener.GroupTags;
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//
///**
// * @Descreption SCM-6231:使用标签检索组合文件属性条件查询文件
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6231 extends TestScmBase {
//    private String fileNameBase = "file6231_";
//    private String fileAuthor = "author6231";
//    private String wsName = "ws6231";
//    private SiteWrapper site = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private File localPath = null;
//    private String filePath = null;
//    private String updatePath = null;
//    private int fileSize = 1024 * 100;
//
//    @BeforeClass
//    public void setUp() throws Exception {
//        localPath = new File( TestScmBase.dataDirectory + File.separator
//                + TestTools.getClassName() );
//        filePath = localPath + File.separator + "localFile_" + fileSize
//                + ".txt";
//        updatePath = localPath + File.separator + "updateFile_" + fileSize / 2
//                + ".txt";
//        TestTools.LocalFile.removeFile( localPath );
//        TestTools.LocalFile.createDir( localPath.toString() );
//        TestTools.LocalFile.createFile( filePath, fileSize );
//        TestTools.LocalFile.createFile( updatePath, fileSize / 2 );
//
//        site = ScmInfo.getSite();
//        session = ScmSessionUtils.createSession( site );
//        ScmWorkspaceUtil.deleteWs( wsName, session );
//        ScmWorkspaceUtil.createWS( session, wsName, true );
//        ScmWorkspaceUtil.wsSetPriority( session, wsName );
//        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
//        prepareData();
//    }
//
//    private void prepareData() throws ScmException {
//        // 构造4个测试文件，编号依次为1,2,3,4
//        ScmTags file1Tag = TagRetrievalUtils.createTag( "tag1", "tag2", "aBc" );
//        ScmTags file2Tag = TagRetrievalUtils.createTag( "tag2", "ABC" );
//        ScmTags file3Tag = TagRetrievalUtils.createTag( "tag2", "abc" );
//        ScmTags file4Tag = TagRetrievalUtils.createTag( "tag1" );
//
//        createFileWithTag( ws, fileNameBase + 1, file1Tag );
//        createFileWithTag( ws, fileNameBase + 2, file2Tag );
//        createFileWithTag( ws, fileNameBase + 3, file3Tag );
//        createFileWithTag( ws, fileNameBase + 4, file4Tag );
//    }
//
//    @Test(groups = { GroupTags.base })
//    public void test() throws ScmException, InterruptedException {
//        // 指定标签包含tag1，文件size为100k的文件查询
//        ScmTagCondition tagCond = ScmTagConditionBuilder.builder().tags()
//                .contains( "tag1", true, false ).build();
//        BSONObject queryCond = ScmQueryBuilder
//                .start( ScmAttributeName.File.SIZE ).is( fileSize ).get();
//        String[] expFileList = { fileNameBase + 1, fileNameBase + 4 };
//        waitFileTagBuild( ws, tagCond, queryCond, ScmType.ScopeType.SCOPE_ALL,
//                expFileList.length );
//        ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_ALL, tagCond, queryCond, null, 0, -1 );
//        checkSearchFileList( scmCursor, 1, expFileList );
//
//        // 指定标签包含tag*，历史版本为1的文件查询
//        tagCond = ScmTagConditionBuilder.builder().tags()
//                .contains( "tag*", true, true ).build();
//        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.MAJOR_VERSION )
//                .is( 1 ).get();
//        expFileList = new String[] { fileNameBase + 1, fileNameBase + 2,
//                fileNameBase + 3, fileNameBase + 4 };
//        waitFileTagBuild( ws, tagCond, queryCond,
//                ScmType.ScopeType.SCOPE_HISTORY, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_HISTORY, tagCond, queryCond, null, 0,
//                -1 );
//        checkSearchFileList( scmCursor, 1, expFileList );
//
//        // 指定标签包含abc，查询所有版本文件
//        tagCond = ScmTagConditionBuilder.builder().tags()
//                .contains( "abc", false, true ).build();
//        waitFileTagBuild( ws, tagCond, null, ScmType.ScopeType.SCOPE_ALL, 2 );
//        scmCursor = ScmFactory.Tag.searchFile( ws, ScmType.ScopeType.SCOPE_ALL,
//                tagCond, null, null, 0, -1 );
//        checkSearchFileAllVersion( scmCursor, fileNameBase + 3 );
//
//        // 指定标签包含tag*，文件元数据属性包含标签tag1
//        tagCond = ScmTagConditionBuilder.builder().tags()
//                .contains( "abc", false, true ).build();
//        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.TAGS )
//                .is( TagRetrievalUtils.createTag( "tag1" ) ).get();
//        try {
//            ScmFactory.Tag.searchFile( ws, ScmType.ScopeType.SCOPE_ALL, tagCond,
//                    queryCond, null, 0, -1 );
//            Assert.fail( "should throw exception" );
//        } catch ( RuntimeException e ) {
//            if ( !( e.getMessage().contains( "json can't serialize type" ) ) ) {
//                throw e;
//            }
//        }
//    }
//
//    private void checkSearchFileAllVersion(
//            ScmCursor< ScmFileBasicInfo > scmCursor, String fileName )
//            throws ScmException {
//        List< Integer > expVersions = new ArrayList<>();
//        expVersions.add( 1 );
//        expVersions.add( 2 );
//        List< Integer > actVersions = new ArrayList<>();
//        while ( scmCursor.hasNext() ) {
//            ScmFileBasicInfo fileInfo = scmCursor.getNext();
//            Assert.assertEquals( fileInfo.getFileName(), fileName );
//            actVersions.add( fileInfo.getMajorVersion() );
//        }
//        scmCursor.close();
//        Assert.assertEqualsNoOrder( actVersions.toArray(),
//                expVersions.toArray() );
//    }
//
//    @AfterClass
//    public void tearDown() throws Exception {
//        try {
//            ScmWorkspaceUtil.deleteWs( wsName, session );
//        } finally {
//            session.close();
//        }
//    }
//
//    private void createFileWithTag( ScmWorkspace ws, String fileName,
//            ScmTags scmTags ) throws ScmException {
//        ScmFile file = ScmFactory.File.createInstance( ws );
//        file.setFileName( fileName );
//        file.setAuthor( fileAuthor );
//        file.setTags( scmTags );
//        file.setContent( filePath );
//        file.save();
//        file.updateContent( updatePath );
//    }
//
//    private static void waitFileTagBuild( ScmWorkspace ws, ScmTagCondition cond,
//            BSONObject fileCond, ScmType.ScopeType type, long expCountFile )
//            throws ScmException, InterruptedException {
//        long actCountFile = -1;
//        int i = 0;
//        do {
//            try {
//                actCountFile = ScmFactory.Tag.countFile( ws, type, cond,
//                        fileCond );
//            } catch ( ScmException e ) {
//                if ( !e.getError().equals( ScmError.METASOURCE_ERROR ) ) {
//                    throw e;
//                }
//            }
//            i++;
//            Thread.sleep( 1000 );
//            if ( i > 60 ) {
//                Assert.fail( "countFile超时,countFile=" + actCountFile );
//            }
//        } while ( actCountFile != expCountFile );
//    }
//
//    private static void checkSearchFileList(
//            ScmCursor< ScmFileBasicInfo > scmCursor, int majorVersion,
//            String... fileNameList ) throws ScmException {
//        Set< String > actFileList = new HashSet<>();
//        Set< String > expFileList = new HashSet<>(
//                Arrays.asList( fileNameList ) );
//        while ( scmCursor.hasNext() ) {
//            ScmFileBasicInfo fileInfo = scmCursor.getNext();
//            Assert.assertEquals( fileInfo.getMajorVersion(), majorVersion,
//                    "majorVersion不符合预期 fileInfo：" + fileInfo );
//            actFileList.add( fileInfo.getFileName() );
//        }
//        scmCursor.close();
//        Assert.assertEquals( actFileList, expFileList );
//    }
//}