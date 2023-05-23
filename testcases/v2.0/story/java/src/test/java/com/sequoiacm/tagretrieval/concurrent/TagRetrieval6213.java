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
//import java.util.Map;
//import java.util.TreeMap;
//
///**
// * @Descreption SCM-6213:并发删除单个自由标签和更新文件标签字段
// * @Author yangjianbo
// * @CreateDate 2023/5/18
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version 1.0
// */
//public class TagRetrieval6213 extends TestScmBase {
//    private ScmSession session = null;
//    private SiteWrapper rootSite = null;
//    private WsWrapper wsp = null;
//    private BSONObject queryCond = null;
//    private String fileAuthor = "auth6213";
//    private int fileSize = 1024 * 1024;
//    private String filePath = null;
//    private File localPath = null;
//    private ScmWorkspace ws = null;
//    private String fileName = "file6213_";
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
//
//        // 创建SCM文件
//        ScmId scmId = ScmFileUtils.create( ws, fileName + "1", filePath,
//                fileAuthor, null, null, null );
//
//        // 启动两个并发线程，删除单个标签和更新文件标签字段，如线程a删除标签tag1=value1；线程b更新文件标签字段为[tag1=value1,tag2=value2,tag3=value3]
//        ThreadExecutor threadExec = new ThreadExecutor();
//        Map< String, String > treeMap1 = new TreeMap<>();
//        treeMap1.put( "tag1", "value1" );
//        Map< String, String > treeMap2 = new TreeMap<>();
//        treeMap2.put( "tag1", "value1" );
//        treeMap2.put( "tag2", "value2" );
//        treeMap2.put( "tag3", "value3" );
//
//        threadExec.addWorker( new DeleteTag( scmId, "tag1", "value1" ) );
//        threadExec.addWorker( new UpdateTag( scmId, treeMap2 ) );
//        threadExec.run();
//
//        // 查看线程执行结果、查看文件元数据标签字段
//        ScmFile instance = ScmFactory.File.getInstance( ws, scmId );
//        Map< String, String > customTag = instance.getCustomTag();
//        if ( customTag.size() == 2 ) {
//            treeMap2.remove( "tag1" );
//            Assert.assertEquals( customTag.toString(), treeMap2.toString() );
//        } else {
//            Assert.assertEquals( customTag.toString(), treeMap2.toString() );
//        }
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
//
//    public class UpdateTag {
//        ScmId scmId;
//        Map< String, String > customTag;
//
//        public UpdateTag( ScmId scmId, Map< String, String > customTag ) {
//            this.scmId = scmId;
//            this.customTag = customTag;
//        }
//
//        @ExecuteOrder(step = 1)
//        public void exec() throws Exception {
//            try ( ScmSession session = ScmSessionUtils
//                    .createSession( rootSite )) {
//                ScmWorkspace workspace = ScmFactory.Workspace
//                        .getWorkspace( ws.getName(), session );
//                ScmFile instance = ScmFactory.File.getInstance( workspace,
//                        scmId );
//                instance.setCustomTag( customTag );
//            }
//        }
//    }
//
//}
