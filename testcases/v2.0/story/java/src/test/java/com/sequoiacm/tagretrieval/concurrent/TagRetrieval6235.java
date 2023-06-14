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
//import com.sequoiacm.client.element.ScmId;
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
// * @Descreption SCM-6235:按标签检索文件时，更新文件标签
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6235 extends TestScmBase {
//    private String fileName = "file6235";
//    private String fileAuthor = "author6235";
//    private String fileTagA = "tagA6235";
//    private String fileTagB = "tagB6235";
//    private String wsName = "ws6235";
//    private SiteWrapper rootSite = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//    private List< ScmId > tagAFile = new ArrayList<>();
//    private List< ScmId > tagBFile = new ArrayList<>();
//    private List< String > expFinallyFileList = new ArrayList<>();
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
//    @Test
//    public void test() throws Exception {
//        ThreadExecutor t = new ThreadExecutor();
//        t.addWorker( new SearchFile() );
//        t.addWorker( new UpdateFileTagAtoTagB() );
//        t.addWorker( new UpdateFileTagBtoTagA() );
//        t.run();
//        ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                .contains( fileTagA, false, false ).build();
//        TagRetrievalUtils.waitFileTagBuild( ws, cond, 10 );
//        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.Tag.searchFile( ws,
//                ScmType.ScopeType.SCOPE_CURRENT, cond, null, null, 0, -1 );
//        TagRetrievalUtils.checkSearchFileList( cursor,
//                expFinallyFileList.toArray( new String[ 0 ] ) );
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
//    private void prepareData() throws ScmException {
//        for ( int i = 0; i < 10; i++ ) {
//            ScmTags tags = TagRetrievalUtils.createTag( fileTagA );
//            ScmFile file = ScmFactory.File.createInstance( ws );
//            file.setFileName( fileName + i );
//            file.setAuthor( fileAuthor );
//            file.setTags( tags );
//            tagAFile.add( file.save() );
//        }
//        for ( int i = 10; i < 20; i++ ) {
//            ScmTags tags = TagRetrievalUtils.createTag( fileTagB );
//            ScmFile file = ScmFactory.File.createInstance( ws );
//            file.setFileName( fileName + i );
//            file.setAuthor( fileAuthor );
//            file.setTags( tags );
//            tagBFile.add( file.save() );
//            expFinallyFileList.add( fileName + i );
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
//                        .contains( fileTagA, false, false ).build();
//                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.Tag
//                        .searchFile( ws, ScmType.ScopeType.SCOPE_CURRENT, cond,
//                                null, null, 0, -1 );
//                cursor.close();
//            } finally {
//                session.close();
//            }
//
//        }
//    }
//
//    private class UpdateFileTagAtoTagB {
//
//        @ExecuteOrder(step = 1)
//        private void run() throws ScmException {
//            ScmSession session = ScmSessionUtils.createSession( rootSite );
//            try {
//                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
//                        session );
//                for ( ScmId id : tagAFile ) {
//                    ScmFile file = ScmFactory.File.getInstance( ws, id );
//                    ScmTags tags = TagRetrievalUtils.createTag( fileTagB );
//                    file.setTags( tags );
//                }
//            } finally {
//                session.close();
//            }
//        }
//    }
//
//    private class UpdateFileTagBtoTagA {
//        @ExecuteOrder(step = 1)
//        private void run() throws ScmException {
//            ScmSession session = ScmSessionUtils.createSession( rootSite );
//            try {
//                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
//                        session );
//                for ( ScmId id : tagBFile ) {
//                    ScmFile file = ScmFactory.File.getInstance( ws, id );
//                    ScmTags tags = TagRetrievalUtils.createTag( fileTagA );
//                    file.setTags( tags );
//                }
//            } finally {
//                session.close();
//            }
//        }
//    }
//
//}