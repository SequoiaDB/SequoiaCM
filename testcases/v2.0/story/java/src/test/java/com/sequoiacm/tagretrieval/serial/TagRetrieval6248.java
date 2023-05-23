//package com.sequoiacm.tagretrieval.serial;
//
//import com.sequoiacm.client.common.ScmType;
//import com.sequoiacm.client.core.ScmCursor;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmQueryBuilder;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmFileBasicInfo;
//import com.sequoiacm.client.element.tag.ScmTagCondition;
//import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
//import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
//import com.sequoiacm.testcommon.ScmInfo;
//import com.sequoiacm.testcommon.ScmSessionUtils;
//import com.sequoiacm.testcommon.SiteWrapper;
//import com.sequoiacm.testcommon.TestScmBase;
//import com.sequoiacm.testcommon.TestSdbTools;
//import com.sequoiacm.testcommon.TestTools;
//import com.sequoiacm.testcommon.listener.GroupTags;
//import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//import org.bson.BSONObject;
//import org.bson.BasicBSONObject;
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import java.io.File;
//
///**
// * @Descreption  SCM-6248:标签检索SDB索引已存在，重复创建索引
// * @Author yangjianbo
// * @CreateDate 2023/5/19
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version 1.0
// */
//public class TagRetrieval6248 extends TestScmBase {
//    private ScmSession session = null;
//    private SiteWrapper rootSite = null;
//    private String wsName = "ws6248";
//    private String fileAuthor = "auth6248";
//    private int fileSize = 100;
//    private String filePath = null;
//    private File localPath = null;
//    private ScmWorkspace ws = null;
//    private String fileName = "file6248_";
//    private int fileNum = 500;
//    private String name = "name";
//    private String tagStatusName = "tag_retrieval_status";
//    private boolean runSuccess = false;
//
//    @BeforeClass
//    private void setUp() throws Exception {
//
//        localPath = new File( TestScmBase.dataDirectory + File.separator
//                + TestTools.getClassName() );
//        filePath = localPath + File.separator + "localFile1_" + fileSize
//                + ".txt";
//
//        rootSite = ScmInfo.getRootSite();
//        session = ScmSessionUtils.createSession( rootSite );
//
//        cleanEnv();
//
//        TestTools.LocalFile.createDir( localPath.toString() );
//        TestTools.LocalFile.createFile( filePath, fileSize );
//        ws = ScmWorkspaceUtil.createWS( session, wsName, rootSite.getSiteId() );
//        ScmWorkspaceUtil.wsSetPriority( session, wsName );
//    }
//
//    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
//    public void test() throws Exception {
//        // 工作区内存在大量标签文件
//        for ( int i = 0; i < fileNum; i++ ) {
//            ScmFileUtils.create( ws, fileName + i, filePath, fileAuthor,
//                    "tar" + i, "Key" + i, "Value" + i );
//        }
//
//        // 工作区开启标签检索,并完成
//        ws.setEnableTagRetrieval( true );
//
//        TagRetrievalUtils.waitWsTagRetrievalStatus( ws,
//                ScmWorkspaceTagRetrievalStatus.ENABLED, 100 );
//
//        // 直连SDB修改工作区索引状态属性为未开启状态
//        BSONObject matcher = ScmQueryBuilder.start( name ).is( ws.getName() )
//                .get();
//        BSONObject modify = new BasicBSONObject( "$set",
//                new BasicBSONObject( tagStatusName,
//                        ScmWorkspaceTagRetrievalStatus.DISABLED.getValue() ) );
//        TestSdbTools.update( TestScmBase.mainSdbUrl, TestScmBase.sdbUserName,
//                TestScmBase.sdbPassword, TestSdbTools.SCM_CS,
//                TestSdbTools.SCM_CL_WORKSPACE, matcher, modify );
//
//        // 使用驱动再次执行开启工作区索引操作
//        ws.setEnableTagRetrieval( true );
//
//        TagRetrievalUtils.waitWsTagRetrievalStatus( ws,
//                ScmWorkspaceTagRetrievalStatus.ENABLED, 100 );
//
//        // 使用标签检索文件成功
//        ScmTagCondition tagCond = ScmTagConditionBuilder.builder().tags()
//                .contains( "tar*", false, true ).build();
//        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, tagCond, null, null, 0, -1 );
//        long countFile = 0;
//        while ( cursor.hasNext() ) {
//            cursor.getNext();
//            countFile++;
//        }
//        Assert.assertEquals( countFile, fileNum );
//        runSuccess = true;
//
//    }
//
//    @AfterClass
//    private void tearDown() throws Exception {
//        try {
//            if ( runSuccess || TestScmBase.forceClear ) {
//                cleanEnv();
//            }
//        } finally {
//            if ( session != null ) {
//                session.close();
//            }
//        }
//    }
//
//    private void cleanEnv() throws Exception {
//        TestTools.LocalFile.removeFile( localPath );
//        ScmWorkspaceUtil.deleteWs( wsName, session );
//    }
//
//}
