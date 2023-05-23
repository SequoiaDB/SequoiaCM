//package com.sequoiacm.tagretrieval;
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
// * @Descreption SCM-6206:文件更新标签，增加、删除单个自由标签
// * @Author yangjianbo
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version 1.0
// */
//public class TagRetrieval6206 extends TestScmBase {
//    private ScmSession session = null;
//    private SiteWrapper rootSite = null;
//    private WsWrapper wsp = null;
//    private BSONObject queryCond = null;
//    private String fileAuthor = "auth6206";
//    private int fileSize = 1024 * 1024;
//    private String filePath = null;
//    private File localPath = null;
//    private ScmWorkspace ws = null;
//    private String fileName = "file6206_";
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
//    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite, GroupTags.base })
//    public void test() throws Exception {
//        Map< String, String > instanceCustomTag = null;
//        Map< String, String > expectedCustomTag = new TreeMap< String, String >();
//
//        // 创建SCM文件，不设置自由标签
//        ScmId scmId = ScmFileUtils.create( ws, fileName + "1", filePath,
//                fileAuthor, null, null, null );
//        ScmFile instance = ScmFactory.File.getInstance( ws, scmId );
//
//        // 更新文件自由标签字段，增加单个标签如a=1
//        instance.addCustomTag( "a", "1" );
//        instanceCustomTag = instance.getCustomTag();
//
//        expectedCustomTag.put( "a", "1" );
//        Assert.assertEquals( instanceCustomTag.toString(),
//                expectedCustomTag.toString() );
//
//        // 更新文件自由标签字段，增加key重复的标签，如a=2
//        instance.addCustomTag( "a", "2" );
//        instanceCustomTag = instance.getCustomTag();
//
//        expectedCustomTag.put( "a", "2" );
//        
//        // SEQUOIACM-1380
//        Assert.assertEquals( instanceCustomTag.toString(),
//                expectedCustomTag.toString() );
//
//        // 更新文件自由标签字段，删除单个标签，如删除a=2
//        instance.removeCustomTag( "a", "2" );
//        instanceCustomTag = instance.getCustomTag();
//        expectedCustomTag = new TreeMap< String, String >();
//
//        Assert.assertEquals( instanceCustomTag.toString(),
//                expectedCustomTag.toString() );
//
//        // 更新文件自由标签字段，删除不存在的标签，如删除b=2
//        instance.removeCustomTag( "b", "2" );
//        instanceCustomTag = instance.getCustomTag();
//
//        Assert.assertEquals( instanceCustomTag.toString(),
//                expectedCustomTag.toString() );
//
//        // 6、文件多次更新自由标签字段，增加、删除key相同、value不同的单个标签
//        instance.addCustomTag( "a", "1" );
//        instance.addCustomTag( "a", "1" );
//        instance.addCustomTag( "a", "2" );
//        instance.removeCustomTag( "a", "1" );
//        expectedCustomTag.put( "a", "2" );
//        Assert.assertEquals( instanceCustomTag.toString(),
//                expectedCustomTag.toString() );
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
//}
