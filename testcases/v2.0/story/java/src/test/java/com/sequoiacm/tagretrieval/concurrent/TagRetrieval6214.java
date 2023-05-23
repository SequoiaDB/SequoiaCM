//package com.sequoiacm.tagretrieval.concurrent;
//
//import com.sequoiacm.client.core.ScmAttributeName;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmFile;
//import com.sequoiacm.client.core.ScmQueryBuilder;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmId;
//import com.sequoiacm.client.element.ScmTags;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.testcommon.ScmInfo;
//import com.sequoiacm.testcommon.ScmSessionUtils;
//import com.sequoiacm.testcommon.SiteWrapper;
//import com.sequoiacm.testcommon.TestScmBase;
//import com.sequoiacm.testcommon.WsWrapper;
//import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
//import com.sequoiadb.threadexecutor.ThreadExecutor;
//import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
//import org.bson.BSONObject;
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * @Descreption SCM-6214:并发更新文件标签列表 (原 SCM-1620：多线程并发添加/相同相同标签)
// * @Author yangjianbo
// * @CreateDate 2023/5/22
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version 1.0
// */
//public class TagRetrieval6214 extends TestScmBase {
//    private boolean runSuccess = false;
//
//    private SiteWrapper site = null;
//    private WsWrapper wsp = null;
//    private ScmSession session = null;
//    private ScmWorkspace ws = null;
//
//    private String name = "definemeta6214";
//    private ScmId fileId = null;
//
//    @BeforeClass(alwaysRun = true)
//    private void setUp() throws ScmException {
//        site = ScmInfo.getSite();
//        wsp = ScmInfo.getWs();
//        session = ScmSessionUtils.createSession( site );
//        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
//        BSONObject cond = ScmQueryBuilder
//                .start( ScmAttributeName.File.FILE_NAME ).is( name ).get();
//        ScmFileUtils.cleanFile( wsp, cond );
//        this.prepareScmFile();
//    }
//
//    @Test(groups = { "twoSite", "fourSite" })
//    private void test() throws Exception {
//        // Random random = new Random();
//        ThreadExecutor threadExec = new ThreadExecutor();
//        for ( int i = 0; i < 5; i++ ) {
//            threadExec.addWorker( new AddTags() );
//            threadExec.addWorker( new SetTags() );
//        }
//        threadExec.run();
//
//        // check results
//        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
//        ScmTags tags = file.getTags();
//        Set< String > actSet = tags.toSet();
//
//        Set< String > expSet1 = new HashSet<>();
//        expSet1.add( "5" );
//        expSet1.add( "6" );
//
//        Set< String > expSet4 = new HashSet<>();
//        expSet4.add( "3" );
//        expSet4.add( "5" );
//        expSet4.add( "6" );
//        if ( !actSet.equals( expSet1 ) && !actSet.equals( expSet4 ) ) {
//            Assert.fail(
//                    "check results failed. actMap = " + actSet.toString() );
//        }
//        runSuccess = true;
//    }
//
//    @AfterClass
//    private void tearDown() throws ScmException {
//        try {
//            if ( runSuccess || TestScmBase.forceClear ) {
//                ScmFactory.File.deleteInstance( ws, fileId, true );
//            }
//        } finally {
//            if ( null != session ) {
//                session.close();
//            }
//        }
//    }
//
//    private void prepareScmFile() throws ScmException {
//        // upload file
//        ScmFile file = ScmFactory.File.createInstance( ws );
//        file.setFileName( name );
//        fileId = file.save();
//    }
//
//    private class AddTags {
//        @ExecuteOrder(step = 1)
//        public void exec() throws Exception {
//            ScmSession session = null;
//            try {
//                session = ScmSessionUtils.createSession( site );
//                ScmWorkspace ws = ScmFactory.Workspace
//                        .getWorkspace( wsp.getName(), session );
//                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
//                file.addTagV2( "3" );
//            } finally {
//                if ( session != null ) {
//                    session.close();
//                }
//            }
//        }
//    }
//
//    private class SetTags {
//        @ExecuteOrder(step = 1)
//        public void exec() throws Exception {
//            ScmSession session = null;
//            try {
//                session = ScmSessionUtils.createSession( site );
//                ScmWorkspace ws = ScmFactory.Workspace
//                        .getWorkspace( wsp.getName(), session );
//                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
//                ScmTags tags = new ScmTags();
//                tags.addTag( "5" );
//                tags.addTag( "6" );
//                file.setTags( tags );
//            }finally {
//                if ( session != null ) {
//                    session.close();
//                }
//            }
//        }
//    }
//}