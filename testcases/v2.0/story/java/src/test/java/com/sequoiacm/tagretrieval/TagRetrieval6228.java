//package com.sequoiacm.tagretrieval;
//
//import java.util.HashMap;
//
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
//import com.sequoiacm.testcommon.listener.GroupTags;
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//
///**
// * @Descreption SCM-6228:使用or组合多个contains检索文件
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6228 extends TestScmBase {
//    private String fileNameBase = "file6228_";
//    private String fileAuthor = "author6228";
//    private String wsName = "ws6228";
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
//        HashMap file1CusTomTag = TagRetrievalUtils.createCustomTag(
//                "{'tag1':'vtag1','tag2':'vtag2','aBc':'vaBc'}" );
//        HashMap file2CusTomTag = TagRetrievalUtils
//                .createCustomTag( "{'tag2':'vtag2','ABC':'vABC'}" );
//        HashMap file3CusTomTag = TagRetrievalUtils
//                .createCustomTag( "{'tag2':'vtag2','abc':'vabc'}" );
//        HashMap file4CusTomTag = TagRetrievalUtils
//                .createCustomTag( "{'tag1':'vtag1'}" );
//
//        createFileWithTag( ws, fileNameBase + 1, file1CusTomTag, file1Tag );
//        createFileWithTag( ws, fileNameBase + 2, file2CusTomTag, file2Tag );
//        createFileWithTag( ws, fileNameBase + 3, file3CusTomTag, file3Tag );
//        createFileWithTag( ws, fileNameBase + 4, file4CusTomTag, file4Tag );
//    }
//
//    @Test(groups = { GroupTags.base })
//    public void test() throws ScmException, InterruptedException {
//        // 1. 字符串标签包含tag1 or 自由标签包含tag2=vtag2的文件
//        ScmTagCondition tagCondition = ScmTagConditionBuilder.builder().tags()
//                .contains( "tag1" ).build();
//        ScmTagCondition customTagCondition = ScmTagConditionBuilder.builder()
//                .customTag().contains( "tag2", "vtag2" ).build();
//        ScmTagCondition cond = ScmTagConditionBuilder.builder()
//                .or( tagCondition, customTagCondition ).build();
//        String[] expFileList = { fileNameBase + 1, fileNameBase + 2,
//                fileNameBase + 3, fileNameBase + 4 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 2. 字符串标签包含tag1 or 自由标签包含test=test的文件
//        tagCondition = ScmTagConditionBuilder.builder().tags()
//                .contains( "tag1" ).build();
//        customTagCondition = ScmTagConditionBuilder.builder().customTag()
//                .contains( "test", "test" ).build();
//        cond = ScmTagConditionBuilder.builder()
//                .or( tagCondition, customTagCondition ).build();
//        expFileList = new String[] { fileNameBase + 1, fileNameBase + 4 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 3. 字符串标签包含test or 自由标签包含test=test的文件
//        tagCondition = ScmTagConditionBuilder.builder().tags()
//                .contains( "test" ).build();
//        customTagCondition = ScmTagConditionBuilder.builder().customTag()
//                .contains( "test", "test" ).build();
//        cond = ScmTagConditionBuilder.builder()
//                .or( tagCondition, customTagCondition ).build();
//        expFileList = new String[] {};
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
//            HashMap customTag, ScmTags scmTags ) throws ScmException {
//        ScmFile file = ScmFactory.File.createInstance( ws );
//        file.setFileName( fileName );
//        file.setAuthor( fileAuthor );
//        file.setCustomTag( customTag );
//        file.setTags( scmTags );
//        file.save();
//    }
//}