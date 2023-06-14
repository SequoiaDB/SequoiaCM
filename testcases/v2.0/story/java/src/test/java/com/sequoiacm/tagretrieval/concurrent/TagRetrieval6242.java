// package com.sequoiacm.tagretrieval.concurrent;
//
// import com.sequoiacm.client.core.ScmFactory;
// import com.sequoiacm.client.core.ScmSession;
// import com.sequoiacm.client.core.ScmWorkspace;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
// import com.sequoiacm.exception.ScmError;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
// import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
// import com.sequoiadb.threadexecutor.ThreadExecutor;
// import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
// import org.testng.Assert;
// import org.testng.annotations.AfterClass;
// import org.testng.annotations.BeforeClass;
// import org.testng.annotations.Test;
//
/// **
// * @Descreption SCM-6242 :: 版本: 1 :: 并发关闭标签检索
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/22
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6242 extends TestScmBase {
//
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6242";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
//
// @BeforeClass
// private void setUp() throws Exception {
// rootSite = ScmInfo.getRootSite();
// session = ScmSessionUtils.createSession( rootSite );
// ScmWorkspaceUtil.deleteWs( wsName, session );
// }
//
// @Test
// private void test() throws Exception {
// int siteNum = ScmInfo.getSiteNum();
// ws = ScmWorkspaceUtil.createWS( session, wsName, true, siteNum );
// ScmWorkspaceUtil.wsSetPriority( session, wsName );
// ThreadExecutor threadExec = new ThreadExecutor();
// threadExec.addWorker( new setDisableTagRetrieval() );
// threadExec.addWorker( new setDisableTagRetrieval() );
// threadExec.run();
// TagRetrievalUtils.waitForTagRetrievalStatus( session, wsName, 10,
// ScmWorkspaceTagRetrievalStatus.DISABLED );
// ws = ScmFactory.Workspace.getWorkspace( wsName, session );
// Assert.assertEquals( ws.getTagRetrievalStatus(),
// ScmWorkspaceTagRetrievalStatus.DISABLED );
// runSuccess = true;
// }
//
// @AfterClass
// private void tearDown() throws Exception {
// try {
// if ( runSuccess || TestScmBase.forceClear ) {
// ScmWorkspaceUtil.deleteWs( wsName, session );
// }
// } finally {
// if ( session != null ) {
// session.close();
// }
// }
// }
//
// private class setDisableTagRetrieval {
// @ExecuteOrder(step = 1)
// public void execute() throws ScmException {
// try ( ScmSession session = ScmSessionUtils
// .createSession( rootSite )) {
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
// session );
// ws.setEnableTagRetrieval( false );
// }catch (ScmException e) {
// if (e.getErrorCode() != ScmError.OPERATION_UNSUPPORTED.getErrorCode()) {
// throw e;
// }
// }
// }
// }
// }
