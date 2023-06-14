//package com.sequoiacm.tagretrieval;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//
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
//public class TagRetrieval6261B extends TestScmBase {
//    private String fileNameBase = "file6261B_";
//    private String fileAuthor = "author6261B";
//    private String wsName = "ws6261B";
//    private String deleteWsName = "ws6261B_delete";
//    private SiteWrapper rootSite = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private ScmWorkspace deleteWs = null;
//    private List< String > expCustomTagKeys = new ArrayList<>();
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
//            expCustomTagKeys.add( "key" + i );
//        }
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//        deleteWs = ScmWorkspaceUtil.createWS( session, deleteWsName, true );
//        ScmWorkspaceUtil.deleteWs( deleteWsName, session );
//    }
//
//    @Test
//    public void test() throws ScmException, InterruptedException {
//        testListCustomTagKey();
//
//        // TODO:SEQUOIACM-1386
//        // testGetCustomTag();
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
//    private void testGetCustomTag() throws ScmException {
//        // 获取存在标签
//        String tag0key = "key0";
//        String tag0value = "value0";
//        ScmCustomTag customTag0 = ScmFactory.CustomTag.getCustomTag( ws,
//                tag0key, tag0value );
//        Assert.assertEquals( customTag0.getKey(), tag0key );
//        Assert.assertEquals( customTag0.getValue(), tag0value );
//        Assert.assertNotNull( customTag0.getId() );
//
//        // 获取不存在标签
//        ScmCustomTag tagNotExist = ScmFactory.CustomTag.getCustomTag( ws,
//                "tagNotExist", "tagNotExist" );
//    }
//
//    private void testListCustomTagKey() throws ScmException {
//        // 指定不存在工作区
//        try {
//            ScmFactory.CustomTag.listCustomTagKey( deleteWs, "key*", true, 0,
//                    -1 );
//            Assert.fail( "指定不存在工作区，应该抛出异常" );
//        } catch ( ScmException e ) {
//            if ( !e.getError().equals( ScmError.WORKSPACE_NOT_EXIST ) ) {
//                throw e;
//            }
//        }
//
//        // 不指定keyMatcher,指定升序
//        ScmCursor< String > cursor = ScmFactory.CustomTag.listCustomTagKey( ws,
//                null, true, 0, -1 );
//        checkListCustomTags( cursor, expCustomTagKeys );
//
//        // 指定skip,limit限制返回结果
//        cursor = ScmFactory.CustomTag.listCustomTagKey( ws, null, true, 5, 3 );
//        List< String > exp = expCustomTagKeys.subList( 5, 5 + 3 );
//        checkListCustomTags( cursor, exp );
//
//        // 按key降序排序
//        cursor = ScmFactory.CustomTag.listCustomTagKey( ws, null, false, 0,
//                -1 );
//        Collections.sort( expCustomTagKeys, Collections.reverseOrder() );
//        checkListCustomTags( cursor, expCustomTagKeys );
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
//            ScmCursor< String > scmCustomTagScmCursor,
//            List< String > expTagKeysList ) throws ScmException {
//        List< String > actTagKeysList = new ArrayList<>();
//        while ( scmCustomTagScmCursor.hasNext() ) {
//            actTagKeysList.add( scmCustomTagScmCursor.getNext() );
//        }
//        scmCustomTagScmCursor.close();
//        Assert.assertEquals( actTagKeysList, expTagKeysList );
//    }
//}