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
//import com.sequoiacm.client.core.*;
//import com.sequoiacm.client.element.ScmTags;
//import com.sequoiacm.client.element.tag.ScmTag;
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
//public class TagRetrieval6260B extends TestScmBase {
//    private String fileNameBase = "file6260B_";
//    private String fileAuthor = "author6260B";
//    private String wsName = "ws6260B";
//    private String deleteWsName = "ws6260B_delete";
//    private SiteWrapper rootSite = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private ScmWorkspace deleteWs = null;
//    private List< String > expTagsList = new ArrayList<>();
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
//            expTagsList.add( "tag" + i );
//        }
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//        deleteWs = ScmWorkspaceUtil.createWS( session, deleteWsName, true );
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//        ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                .contains( "tag*", true, true ).build();
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, fileSize );
//    }
//
//    @Test
//    public void test() throws ScmException, InterruptedException {
//        testListTag();
//
//        // TODO: SEQUOIACM-1386
//        // testGetTag();
//    }
//
//    private void testGetTag() throws ScmException {
//        ScmTag tag1 = ScmFactory.Tag.getTags( ws, "tag1" );
//        Assert.assertEquals( tag1.getName(), "tag1" );
//        Assert.assertNotNull( tag1.getId() );
//
//        // 获取不存在的tag
//        try {
//            ScmTag tagNotExist = ScmFactory.Tag.getTags( ws, "tagNotExist" );
//            tagNotExist.getName();
//            Assert.fail( "获取不存在的tag，应该抛出异常" );
//        } catch ( ScmException e ) {
//            if ( !e.getError().equals( ScmError.INVALID_ARGUMENT ) ) {
//                throw e;
//            }
//        }
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
//    private void testListTag() throws ScmException {
//        // 指定不存在工作区
//        try {
//            ScmFactory.Tag.listTags( deleteWs, "tag*", null, 0, -1 );
//            Assert.fail( "指定不存在工作区，应该抛出异常" );
//        } catch ( ScmException e ) {
//            if ( !e.getError().equals( ScmError.WORKSPACE_NOT_EXIST ) ) {
//                throw e;
//            }
//        }
//
//        // 不指定ScmTagCondition,默认排序
//        ScmCursor< ScmTag > scmTagScmCursor = ScmFactory.Tag.listTags( ws, null,
//                null, 0, -1 );
//        checkListTags( scmTagScmCursor, expTagsList );
//
//        // 指定skip，limit限制返回结果
//        scmTagScmCursor = ScmFactory.Tag.listTags( ws, "tag*", null, 4, 4 );
//        List< String > subExp = expTagsList.subList( 4, 4 + 4 );
//        checkListTags( scmTagScmCursor, subExp );
//
//        // 指定降序排序
//        BSONObject orderBy = ScmQueryBuilder
//                .start( ScmAttributeName.TagLib.TAG ).is( -1 ).get();
//        scmTagScmCursor = ScmFactory.Tag.listTags( ws, "tag*", orderBy, 0, -1 );
//        Collections.sort( expTagsList, Collections.reverseOrder() );
//        checkListTags( scmTagScmCursor, expTagsList );
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
//
//    private static void checkListTags( ScmCursor< ScmTag > scmTagScmCursor,
//            List< String > expTagsList ) throws ScmException {
//        List< String > actTagsList = new ArrayList<>();
//        while ( scmTagScmCursor.hasNext() ) {
//            ScmTag tag = scmTagScmCursor.getNext();
//            actTagsList.add( tag.getName() );
//        }
//        scmTagScmCursor.close();
//        Assert.assertEquals( actTagsList, expTagsList );
//    }
//}