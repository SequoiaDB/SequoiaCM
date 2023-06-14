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
// * @Descreption SCM-6226:使用contains、notcontains按自由标签模糊匹配检索文件
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6226 extends TestScmBase {
//    private String fileNameBase = "file6226_";
//    private String fileAuthor = "author6226";
//    private String wsName = "ws6226";
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
//        HashMap file1Tag = TagRetrievalUtils
//                .createCustomTag( "{'abc':'vaabbcc'}" );
//        HashMap file2Tag = TagRetrievalUtils
//                .createCustomTag( "{'test':'vtest','abc':'vABC'}" );
//        HashMap file3Tag = TagRetrievalUtils.createCustomTag(
//                "{'test':'vtest*','vtest':'vtest\\\\','abc':'vabc'}" );
//        HashMap file4Tag = TagRetrievalUtils
//                .createCustomTag( "{'test':'vtest?'}" );
//
//        createFileWithTag( ws, fileNameBase + 1, file1Tag );
//        createFileWithTag( ws, fileNameBase + 2, file2Tag );
//        createFileWithTag( ws, fileNameBase + 3, file3Tag );
//        createFileWithTag( ws, fileNameBase + 4, file4Tag );
//    }
//
//    @Test(groups = { GroupTags.base })
//    public void test() throws ScmException, InterruptedException {
//        // 1. 包含abc=*b*，忽略大小写的文件
//        ScmTagCondition cond = ScmTagConditionBuilder.builder().customTag()
//                .contains( "abc", "*b*", true, true ).build();
//        String[] expFileList = { fileNameBase + 1, fileNameBase + 2,
//                fileNameBase + 3 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 2. 不包含abc=va*c，不忽略大小写的文件
//        cond = ScmTagConditionBuilder.builder().customTag()
//                .notContains( "abc", "va*c", false, true ).build();
//        expFileList = new String[] { fileNameBase + 2, fileNameBase + 4 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 3. 包含abc=v?b?，忽略大小写的文件
//        cond = ScmTagConditionBuilder.builder().customTag()
//                .contains( "abc", "v?b?", true, true ).build();
//        expFileList = new String[] { fileNameBase + 2, fileNameBase + 3 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // 4. 不包含abc=va?c，不忽略大小写的文件
//        cond = ScmTagConditionBuilder.builder().customTag()
//                .notContains( "abc", "va?c", false, true ).build();
//        expFileList = new String[] { fileNameBase + 1, fileNameBase + 2,
//                fileNameBase + 4 };
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//
//        // TODO: SEQUOIACM-1382
//        // // 5. 包含test=vtest\*、vtest=vtest\\的文件
//        // cond = ScmTagConditionBuilder.builder().customTag()
//        // .contains( "test", "vtest\\*", false, true )
//        // .contains( "vtest", "vtest\\\\", false, true ).build();
//        // scmCursor = ScmFactory.Tag.searchFile( ws,
//        // ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        // expFileList = new String[] { fileNameBase + 3 };
//        // TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        // scmCursor = ScmFactory.Tag.searchFile( ws,
//        // ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        // TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
//        //
//        // // 6. 不包含test=vtest\？的文件
//        // cond = ScmTagConditionBuilder.builder().customTag()
//        // .notContains( "test", "vtest\\?", false, true ).build();
//        // expFileList = new String[] { fileNameBase + 1, fileNameBase + 2,
//        // fileNameBase + 3 };
//        // TagRetrievalUtils.waitFileTagBuild( ws, cond, expFileList.length );
//        // scmCursor = ScmFactory.Tag.searchFile( ws,
//        // ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        // TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
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
//            HashMap customTag ) throws ScmException {
//        ScmFile file = ScmFactory.File.createInstance( ws );
//        file.setFileName( fileName );
//        file.setAuthor( fileAuthor );
//        file.setCustomTag( customTag );
//        file.save();
//    }
//
//    public static void main( String[] args ) {
//        System.err.println( "vtest\\\\" );
//    }
//}