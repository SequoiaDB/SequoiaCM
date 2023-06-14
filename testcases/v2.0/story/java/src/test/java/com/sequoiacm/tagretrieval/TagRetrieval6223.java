//package com.sequoiacm.tagretrieval;
//
//import com.sequoiacm.testcommon.listener.GroupTags;
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
//import com.sequoiacm.testcommon.ScmInfo;
//import com.sequoiacm.testcommon.ScmSessionUtils;
//import com.sequoiacm.testcommon.SiteWrapper;
//import com.sequoiacm.testcommon.TestScmBase;
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//
///**
// * @Descreption SCM-6223:使用contains、notcontains按字符串标签精确匹配检索文件
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6223 extends TestScmBase {
//    private String fileNameBase = "file6223_";
//    private String fileAuthor = "author6223";
//    private String wsName = "ws6223";
//    private SiteWrapper site = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//
//    @BeforeClass
//    public void setUp() throws Exception {
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
//        // 包含abc，忽略大小写的文件
//        ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                .contains( "abc", true, false ).build();
//        String[] expFileList = { fileNameBase + 1, fileNameBase + 2,
//                fileNameBase + 3 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 不包含abc，忽略大小写的文件
//        cond = ScmTagConditionBuilder.builder().tags()
//                .notContains( "abc", true, false ).build();
//        expFileList = new String[] { fileNameBase + 4 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 包含ABC、tag2，不忽略大小写的标签
//        cond = ScmTagConditionBuilder.builder().tags()
//                .contains( "ABC", false, false )
//                .contains( "tag2", false, false ).build();
//        expFileList = new String[] { fileNameBase + 2 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 不包含ABC，不忽略大小写的标签
//        cond = ScmTagConditionBuilder.builder().tags()
//                .notContains( "ABC", false, false ).build();
//        expFileList = new String[] { fileNameBase + 1, fileNameBase + 3,
//                fileNameBase + 4 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
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
//        file.save();
//    }
//}