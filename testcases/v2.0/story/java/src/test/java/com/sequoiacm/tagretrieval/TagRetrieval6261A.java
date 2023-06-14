//package com.sequoiacm.tagretrieval;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//
//import org.bson.BSONObject;
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.core.*;
//import com.sequoiacm.client.element.tag.ScmCustomTag;
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
// * @Descreption SCM-6261:ScmFactory.CustomTag驱动测试
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6261A extends TestScmBase {
//    private String fileNameBase = "file6261A_";
//    private String fileAuthor = "author6261A";
//    private String wsName = "ws6261A";
//    private String deleteWsName = "ws6261A_delete";
//    private SiteWrapper rootSite = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private ScmWorkspace deleteWs = null;
//    private List< String > expCustomTags = new ArrayList<>();
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
//            createFileWithTag( ws, fileNameBase + i,
//                    "{'key" + i + "':'value" + i + "'}" );
//            expCustomTags.add( "key" + i + ":" + "value" + i );
//        }
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//        deleteWs = ScmWorkspaceUtil.createWS( session, deleteWsName, true );
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//    }
//
//    @Test
//    public void test() throws ScmException, InterruptedException {
//        testListCustomTag();
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
//    private void testListCustomTag() throws ScmException, InterruptedException {
//        // 指定不存在工作区
//        try {
//            ScmFactory.CustomTag.listCustomTag( deleteWs, "key*", "value*",
//                    null, 0, -1 );
//            Assert.fail( "指定不存在工作区，应该抛出异常" );
//        } catch ( ScmException e ) {
//            if ( !e.getError().equals( ScmError.WORKSPACE_NOT_EXIST ) ) {
//                throw e;
//            }
//        }
//
//        // 不指定keyMatcher和valueMatcher,预期列取所有,默认排序
//        ScmCursor< ScmCustomTag > cursor = ScmFactory.CustomTag
//                .listCustomTag( ws, null, null, null, 0, -1 );
//        checkListCustomTags( cursor, expCustomTags );
//
//        // 指定skip,limit限制返回结果
//        cursor = ScmFactory.CustomTag.listCustomTag( ws, "key*", "value*", null,
//                5, 3 );
//        List< String > exp = expCustomTags.subList( 5, 5 + 3 );
//        checkListCustomTags( cursor, exp );
//
//        // 按key降序排序
//        BSONObject orderBy = ScmQueryBuilder
//                .start( ScmAttributeName.TagLib.CUSTOM_TAG_KEY ).is( -1 ).get();
//        cursor = ScmFactory.CustomTag.listCustomTag( ws, "key*", "value*",
//                orderBy, 0, -1 );
//        Collections.sort( expCustomTags, Collections.reverseOrder() );
//        checkListCustomTags( cursor, expCustomTags );
//
//        // 按value降序
//        orderBy = ScmQueryBuilder
//                .start( ScmAttributeName.TagLib.CUSTOM_TAG_VALUE ).is( -1 )
//                .get();
//        cursor = ScmFactory.CustomTag.listCustomTag( ws, "key*", "value*",
//                orderBy, 0, -1 );
//        Collections.sort( expCustomTags, Collections.reverseOrder() );
//        checkListCustomTags( cursor, expCustomTags );
//
//        // 按id升序
//        orderBy = ScmQueryBuilder.start( ScmAttributeName.TagLib.TAG_ID )
//                .is( 1 ).get();
//        cursor = ScmFactory.CustomTag.listCustomTag( ws, "key*", "value*",
//                orderBy, 0, -1 );
//        Collections.sort( expCustomTags );
//        checkListCustomTags( cursor, expCustomTags );
//    }
//
//    private void createFileWithTag( ScmWorkspace ws, String fileName,
//            String str ) throws ScmException {
//        HashMap< String, String > customTag = TagRetrievalUtils
//                .createCustomTag( str );
//        ScmFile file = ScmFactory.File.createInstance( ws );
//        file.setFileName( fileName );
//        file.setAuthor( fileAuthor );
//        file.setCustomTag( customTag );
//        file.save();
//    }
//
//    private static void checkListCustomTags(
//            ScmCursor< ScmCustomTag > scmCustomTagScmCursor,
//            List< String > expTagsList ) throws ScmException {
//        List< String > actTagsList = new ArrayList<>();
//        while ( scmCustomTagScmCursor.hasNext() ) {
//            ScmCustomTag customTag = scmCustomTagScmCursor.getNext();
//            actTagsList.add( customTag.getKey() + ":" + customTag.getValue() );
//        }
//        scmCustomTagScmCursor.close();
//        Assert.assertEquals( actTagsList, expTagsList );
//    }
//}