//package com.sequoiacm.tagretrieval.concurrent;
//
//import java.util.ArrayList;
//import java.util.List;
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
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//import com.sequoiadb.threadexecutor.ThreadExecutor;
//import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
//
///**
// * @Descreption SCM-6236:并发标签检索文件
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6236 extends TestScmBase {
//    private String fileName = "file6236";
//    private String fileAuthor = "author6236";
//    private String fileTag = "tag6236";
//    private String wsName = "ws6236";
//    private SiteWrapper rootSite = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private List< String > expFileList = new ArrayList<>();
//
//    @BeforeClass
//    public void setUp() throws Exception {
//        rootSite = ScmInfo.getRootSite();
//        session = ScmSessionUtils.createSession( rootSite );
//        ScmWorkspaceUtil.deleteWs( wsName, session );
//        ScmWorkspaceUtil.createWS( session, wsName, true );
//        ScmWorkspaceUtil.wsSetPriority( session, wsName );
//        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
//        prepareFile();
//    }
//
//    private void prepareFile() throws ScmException {
//        for ( int i = 0; i < 10; i++ ) {
//            ScmTags tags = TagRetrievalUtils.createTag( fileTag );
//            ScmFile file = ScmFactory.File.createInstance( ws );
//            file.setFileName( fileName + i );
//            file.setAuthor( fileAuthor );
//            file.setTags( tags );
//            file.save();
//            expFileList.add( fileName + i );
//        }
//    }
//
//    @Test
//    public void test() throws Exception {
//        ThreadExecutor t = new ThreadExecutor();
//        for ( int i = 0; i < 3; i++ ) {
//            t.addWorker( new SearchFile() );
//        }
//        t.run();
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
//        private void run() throws ScmException, InterruptedException {
//            ScmSession session = ScmSessionUtils.createSession( rootSite );
//            try {
//                ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                        .contains( fileTag, false, false ).build();
//                TagRetrievalUtils.waitFileTagBuild( ws, cond,
//                        expFileList.size() );
//                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.Tag
//                        .searchFile( ws, ScmType.ScopeType.SCOPE_CURRENT, cond,
//                                null, null, 0, -1 );
//                TagRetrievalUtils.checkSearchFileList( cursor,
//                        expFileList.toArray( new String[ 0 ] ) );
//            } finally {
//                session.close();
//            }
//
//        }
//    }
//}