//package com.sequoiacm.tagretrieval.concurrent;
//
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.common.ScmType;
//import com.sequoiacm.client.core.*;
//import com.sequoiacm.client.element.ScmFileBasicInfo;
//import com.sequoiacm.client.element.ScmId;
//import com.sequoiacm.client.element.ScmTags;
//import com.sequoiacm.client.element.tag.ScmTagCondition;
//import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.testcommon.*;
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//import com.sequoiadb.threadexecutor.ThreadExecutor;
//import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
//
///**
// * @Descreption SCM-6234:按标签检索文件时，并发删除标签文件
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6234 extends TestScmBase {
//    private boolean runSuccess = false;
//    private String fileName = "file6234";
//    private String fileAuthor = "author6234";
//    private String fileTag = "tag6234";
//    private String wsName = "ws6234";
//    private SiteWrapper rootSite = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private ScmId fileId = null;
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
//    private void prepareData() throws ScmException {
//        ScmTags tags = TagRetrievalUtils.createTag( fileTag );
//        ScmFile file = ScmFactory.File.createInstance( ws );
//        file.setFileName( fileName );
//        file.setAuthor( fileAuthor );
//        file.setTags( tags );
//        fileId = file.save();
//    }
//
//    @Test
//    public void test() throws Exception {
//        ThreadExecutor t = new ThreadExecutor();
//        t.addWorker( new SearchFile() );
//        t.addWorker( new DeleteFile() );
//        t.run();
//        ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                .contains( fileTag, false, false ).build();
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, 0 );
//        runSuccess = true;
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
//    private class SearchFile {
//
//        @ExecuteOrder(step = 1)
//        private void run() throws ScmException {
//            ScmSession session = ScmSessionUtils.createSession( rootSite );
//            try {
//                ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                        .contains( fileTag, false, false ).build();
//                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.Tag
//                        .searchFile( ws, ScmType.ScopeType.SCOPE_CURRENT, cond,
//                                null, null, 0, -1 );
//                while ( cursor.hasNext() ) {
//                    ScmFileBasicInfo fileInfo = cursor.getNext();
//                    Assert.assertEquals( fileInfo.getFileName(), fileName,
//                            "fileInfo" + fileInfo );
//                }
//
//            } finally {
//                session.close();
//            }
//
//        }
//    }
//
//    private class DeleteFile {
//        @ExecuteOrder(step = 1)
//        private void run() throws ScmException {
//            ScmSession session = ScmSessionUtils.createSession( rootSite );
//            try {
//                ScmWorkspace ws = ScmFactory.Workspace
//                        .getWorkspace( wsName, session );
//                ScmFactory.File.deleteInstance( ws, fileId, true );
//            } finally {
//                session.close();
//            }
//        }
//    }
//
//}