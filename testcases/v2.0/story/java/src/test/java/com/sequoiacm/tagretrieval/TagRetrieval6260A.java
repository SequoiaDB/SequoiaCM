//package com.sequoiacm.tagretrieval;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
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
//import com.sequoiacm.testcommon.ScmInfo;
//import com.sequoiacm.testcommon.ScmSessionUtils;
//import com.sequoiacm.testcommon.SiteWrapper;
//import com.sequoiacm.testcommon.TestScmBase;
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//
///**
// * @Descreption SCM-6260:ScmFactory.Tag驱动测试
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6260A extends TestScmBase {
//    private String fileNameBase = "file6260A_";
//    private String fileAuthor = "author6260A";
//    private String wsName = "ws6260A";
//    private String deleteWsName = "ws6260A_delete";
//    private SiteWrapper rootSite = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private ScmWorkspace deleteWs = null;
//    private List< String > expFileList = new ArrayList<>();
//    private int fileSize = 10;
//
//    @BeforeClass
//    public void setUp() throws Exception {
//        rootSite = ScmInfo.getRootSite();
//        session = ScmSessionUtils.createSession( rootSite );
//        ScmWorkspaceUtil.deleteWs( wsName, session );
//        ScmWorkspaceUtil.createWS( session, wsName, true );
//        ScmWorkspaceUtil.wsSetPriority( session, wsName );
//        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
//        prepareData();
//    }
//
//    private void prepareData() throws Exception {
//        for ( int i = 0; i < fileSize; i++ ) {
//            createFileWithTag( ws, fileNameBase + i, "tag" + i );
//            expFileList.add( fileNameBase + i );
//        }
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//        deleteWs = ScmWorkspaceUtil.createWS( session, deleteWsName, true );
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//    }
//
//    @Test
//    public void test() throws ScmException, InterruptedException {
//        testSearchFile();
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
//    private void testSearchFile() throws ScmException, InterruptedException {
//        // 指定不存在工作区
//        ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                .contains( "tag*", true, true ).build();
//        try {
//            ScmFactory.Tag.searchFile( deleteWs,
//                    ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//            Assert.fail( "指定不存在工作区，应该抛出异常" );
//        } catch ( ScmException e ) {
//            if ( !e.getError().equals( ScmError.WORKSPACE_NOT_EXIST ) ) {
//                throw e;
//            }
//        }
//
//        // 不指定ScmTagCondition
//        try {
//            ScmFactory.Tag.searchFile( ws, ScmType.ScopeType.SCOPE_CURRENT,
//                    null, null, null, 0, -1 );
//            Assert.fail( "不指定ScmTagCondition，应该抛出异常" );
//        } catch ( ScmException e ) {
//            if ( !e.getError().equals( ScmError.INVALID_ARGUMENT ) ) {
//                throw e;
//            }
//        }
//
//        // 指定skip,limit限制返回结果
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, fileSize );
//        ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 5, 3 );
//        List< String > exp = this.expFileList.subList( 5, 5 + 3 );
//        TagRetrievalUtils.checkSearchFileList( scmCursor,
//                exp.toArray( new String[ 0 ] ) );
//
//        // 指定按文件属性createTime排序,降序
//        BSONObject orderBy = ScmQueryBuilder
//                .start( ScmAttributeName.File.CREATE_TIME ).is( -1 ).get();
//        scmCursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, orderBy, 0, -1 );
//        Collections.sort( expFileList, Collections.< String > reverseOrder() );
//        TagRetrievalUtils.checkSearchFileList( scmCursor,
//                expFileList.toArray( new String[ 0 ] ) );
//
//        // 指定文件按标签排序
//        orderBy = ScmQueryBuilder.start( ScmAttributeName.File.TAGS ).is( 1 )
//                .get();
//        try {
//            ScmFactory.Tag.searchFile( deleteWs,
//                    ScmType.ScopeType.SCOPE_CURRENT, cond, null, orderBy, 0,
//                    -1 );
//            Assert.fail( "指定文件按标签排序，应该抛出异常" );
//        } catch ( ScmException e ) {
//            if ( !e.getError().equals( ScmError.OPERATION_UNSUPPORTED ) ) {
//                throw e;
//            }
//        }
//    }
//
//    private void createFileWithTag( ScmWorkspace ws, String fileName,
//            String... tags ) throws ScmException {
//        ScmTags scmTags = TagRetrievalUtils.createTag( tags );
//        ScmFile file = ScmFactory.File.createInstance( ws );
//        file.setFileName( fileName );
//        file.setAuthor( fileAuthor );
//        file.setTags( scmTags );
//        file.save();
//    }
//}