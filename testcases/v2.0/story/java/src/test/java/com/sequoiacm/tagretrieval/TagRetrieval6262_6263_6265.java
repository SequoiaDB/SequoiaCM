//package com.sequoiacm.tagretrieval;
//
//import com.sequoiacm.client.element.bizconf.ScmTagLibMetaOption;
//import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
//import com.sequoiacm.client.element.tag.ScmTagCondition;
//import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
//import com.sequoiacm.client.exception.ScmInvalidArgumentException;
//import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
//import org.bson.BSONObject;
//import org.bson.BasicBSONObject;
//import org.testng.Assert;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import com.sequoiacm.client.core.ScmFactory;
//import com.sequoiacm.client.core.ScmSession;
//import com.sequoiacm.client.core.ScmWorkspace;
//import com.sequoiacm.client.exception.ScmException;
//import com.sequoiacm.testcommon.ScmInfo;
//import com.sequoiacm.testcommon.ScmSessionUtils;
//import com.sequoiacm.testcommon.TestScmBase;
//import com.sequoiacm.testcommon.WsWrapper;
//
///**
// * @Descreption SCM-6262:ScmTagCondition驱动测试 SCM-6263:ScmWorkspaceConf驱动测试
// *              SCM-6365:ScmWorkSpace.getTagLibIndexErrorMsg()驱动测试
// * @Author YiPan
// * @CreateDate 2023/5/17
// * @UpdateUser
// * @UpdateDate
// * @UpdateRemark
// * @Version
// */
//public class TagRetrieval6262_6263_6265 extends TestScmBase {
//    private ScmSession session;
//    private ScmWorkspace ws;
//    private String wsName = "ws6265";
//
//    @BeforeClass
//    public void setUp() throws ScmException {
//        WsWrapper wsp = ScmInfo.getWs();
//        session = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
//        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
//    }
//
//    @Test
//    public void test() throws Exception {
//        // 6262
//        testScmTagCondition();
//
//        // 6263
//        testScmWorkspaceConf();
//
//        // 6365
//        testTagLibIndexErrorMsg();
//    }
//
//    private void testTagLibIndexErrorMsg() throws Exception {
//        ScmWorkspaceUtil.deleteWs( wsName, session );
//        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName, true );
//        ScmWorkspaceUtil.wsSetPriority( session, wsName );
//        String tagLibIndexErrorMsg = ws.getTagLibIndexErrorMsg();
//        Assert.assertEquals( tagLibIndexErrorMsg, "" );
//    }
//
//    private void testScmWorkspaceConf() {
//        ScmWorkspaceConf conf = new ScmWorkspaceConf();
//
//        // 默认为关闭
//        Assert.assertFalse( conf.isEnableTagRetrieval() );
//
//        // 设置开启
//        conf.setEnableTagRetrieval( true );
//
//        // 获取开启状态
//        Assert.assertTrue( conf.isEnableTagRetrieval() );
//
//        // 设置domain
//        ScmTagLibMetaOption scmTagLibMetaOption = new ScmTagLibMetaOption(
//                "test" );
//        conf.setTagLibMetaOption( scmTagLibMetaOption );
//
//        // 获取domain
//        String tagLibDomain = conf.getTagLibMetaOption().getTagLibDomain();
//        Assert.assertEquals( tagLibDomain, "test" );
//    }
//
//    private void testScmTagCondition() throws ScmInvalidArgumentException {
//        ScmTagCondition cond = ScmTagConditionBuilder.builder().tags()
//                .contains( "abc", true, false ).build();
//        BSONObject actBsonObject = cond.getBsonObject();
//
//        BSONObject expBsonObject = new BasicBSONObject();
//        BSONObject obj1 = new BasicBSONObject();
//        obj1.put( "$contains", "abc" );
//        obj1.put( "$ignore_case", true );
//        obj1.put( "$enable_wildcard", false );
//        expBsonObject.put( "tags", obj1 );
//
//        Assert.assertEquals( actBsonObject, expBsonObject );
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
//}