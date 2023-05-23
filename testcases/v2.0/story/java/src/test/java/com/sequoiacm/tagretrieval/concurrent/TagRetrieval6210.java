//package com.sequoiacm.tagretrieval.concurrent;
//
//import com.sequoiacm.client.core.ScmAttributeName;
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmFile;
//import com.sequoiacm.client.core.ScmQueryBuilder;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.element.ScmId;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.testcommon.ScmInfo;
//import com.sequoiacm.testcommon.ScmSessionUtils;
//import com.sequoiacm.testcommon.SiteWrapper;
//import com.sequoiacm.testcommon.TestScmBase;
//import com.sequoiacm.testcommon.TestTools;
//import com.sequoiacm.testcommon.WsWrapper;
//import com.sequoiacm.testcommon.listener.GroupTags;
//import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
//import com.sequoiadb.threadexecutor.ThreadExecutor;
//import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
//import org.bson.BSONObject;
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import java.io.File;
//
///**
// * @Descreption SCM-6210:文件并发删除单个相同自由标签
// * @Author yangjianbo
// * @CreateDate 2023/5/18
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version 1.0
// */
//public class TagRetrieval6210 extends TestScmBase {
//    private ScmSession session = null;
//    private SiteWrapper rootSite = null;
//    private WsWrapper wsp = null;
//    private BSONObject queryCond = null;
//    private String fileAuthor = "auth6210";
//    private int fileSize = 1024 * 1024;
//    private String filePath = null;
//    private File localPath = null;
//    private ScmWorkspace ws = null;
//    private String fileName = "file6210_";
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
//        wsp = ScmInfo.getWs();
//        session = ScmSessionUtils.createSession( rootSite );
//        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
//        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
//                .is( fileAuthor ).get();
//
//        cleanEnv();
//
//        TestTools.LocalFile.createDir( localPath.toString() );
//        TestTools.LocalFile.createFile( filePath, fileSize );
//    }
//
//    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
//    public void test() throws Exception {
//        // 创建SCM文件
//        ScmId scmId = ScmFileUtils.create( ws, fileName + "1", filePath,
//                fileAuthor, null, null, null );
//        ScmFile instance = ScmFactory.File.getInstance( ws, scmId );
//        // 添加自由标签
//        int threadNum = 5;
//        for ( int i = 0; i < threadNum; i++ ) {
//            if ( i % 2 == 0 ) {
//                instance.addCustomTag( "key" + i, "value" + i );
//            }
//        }
//
//        // 启动5个并发线程，并发删除单个自由标签
//        ThreadExecutor threadExec = new ThreadExecutor();
//        for ( int i = 0; i < threadNum; i++ ) {
//            threadExec.addWorker(
//                    new DeleteTag( scmId, "key" + i, "value" + i ) );
//        }
//        threadExec.run();
//
//        // 查看删除结果
//        instance = ScmFactory.File.getInstance( ws, scmId );
//        Assert.assertEquals( instance.getCustomTag().size(), 0 );
//        runSuccess = true;
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
//    private void cleanEnv() throws ScmException {
//        TestTools.LocalFile.removeFile( localPath );
//        ScmFileUtils.cleanFile( ws.getName(), queryCond );
//    }
//
//    public class DeleteTag {
//        ScmId scmId;
//        String tagKey;
//        String tagValue;
//
//        public DeleteTag( ScmId scmId, String tagKey, String tagValue ) {
//            this.scmId = scmId;
//            this.tagKey = tagKey;
//            this.tagValue = tagValue;
//        }
//
//        @ExecuteOrder(step = 1)
//        public void exec() throws Exception {
//
//            try ( ScmSession session = ScmSessionUtils
//                    .createSession( rootSite )) {
//                ScmWorkspace workspace = ScmFactory.Workspace
//                        .getWorkspace( ws.getName(), session );
//                ScmFile instance = ScmFactory.File.getInstance( workspace,
//                        scmId );
//                instance.removeCustomTag( tagKey, tagValue );
//            }
//        }
//    }
//}
