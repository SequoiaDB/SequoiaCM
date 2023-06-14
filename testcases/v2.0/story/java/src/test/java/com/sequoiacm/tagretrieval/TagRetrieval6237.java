// package com.sequoiacm.tagretrieval;
//
// import com.sequoiacm.client.common.ScmType;
// import com.sequoiacm.client.core.*;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.ScmTags;
// import com.sequoiacm.client.element.tag.ScmTagCondition;
// import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
// import com.sequoiacm.exception.ScmError;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.listener.GroupTags;
// import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
// import org.testng.Assert;
// import org.testng.annotations.AfterClass;
// import org.testng.annotations.BeforeClass;
// import org.testng.annotations.Test;
//
// import java.io.File;
// import java.util.concurrent.TimeoutException;
//
/// **
// * @Descreption SCM-6237 :: 工作区开启/关闭标签检索
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/18
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6237 extends TestScmBase {
//
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6237";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "scmfile6237";
// private ScmId fileId = null;
// private int fileSize = 10;
// private File localPath = null;
// private String filePath = null;
//
// @BeforeClass
// private void setUp() throws Exception {
// rootSite = ScmInfo.getRootSite();
// session = ScmSessionUtils.createSession( rootSite );
// ScmWorkspaceUtil.deleteWs( wsName, session );
// localPath = new File( TestScmBase.dataDirectory + File.separator
// + TestTools.getClassName() );
// filePath = localPath + File.separator + "localFile_" + fileSize
// + ".txt";
// TestTools.LocalFile.removeFile( localPath );
// TestTools.LocalFile.createDir( localPath.toString() );
// TestTools.LocalFile.createFile( filePath, fileSize );
// }
//
// @Test(groups = {GroupTags.base})
// private void test()
// throws ScmException, InterruptedException, TimeoutException {
// int siteNum = ScmInfo.getSiteNum();
// ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
// ScmWorkspaceUtil.wsSetPriority( session, wsName );
// ScmFile file = ScmFactory.File.createInstance( ws );
// file.setContent( filePath );
// ScmTags tags = new ScmTags();
// tags.addTag( "tag1" );
// file.setTags( tags );
// file.setFileName( fileName );
// fileId = file.save();
// // 修改标签检索开启
// ws.setEnableTagRetrieval( true );
//
// // 等待标签检索生效
// TagRetrievalUtils.waitForTagRetrievalStatus( session, wsName, 300,
// ScmWorkspaceTagRetrievalStatus.ENABLED );
//
// ScmTagCondition tagCond = ScmTagConditionBuilder.builder()
// .or( ScmTagConditionBuilder.builder().tags().contains( "tag1" )
// .build() )
// .build();
// long countfile = ScmFactory.Tag.countFile( ws,
// ScmType.ScopeType.SCOPE_ALL, tagCond, null );
// Assert.assertEquals( countfile, 1 );
// // 修改标签检索为false
// ws.setEnableTagRetrieval( false );
// TagRetrievalUtils.waitForTagRetrievalStatus( session, wsName, 100,
// ScmWorkspaceTagRetrievalStatus.DISABLED );
// try {
// ScmFactory.Tag.countFile( ws, ScmType.ScopeType.SCOPE_ALL, tagCond,
// null );
// } catch ( ScmException e ) {
// if ( e.getErrorCode() != ScmError.OPERATION_UNSUPPORTED
// .getErrorCode() ) {
// throw e;
// }
// }
// runSuccess = true;
// }
//
// @AfterClass
// private void tearDown() throws Exception {
// try {
// if ( runSuccess || TestScmBase.forceClear ) {
// ScmFactory.File.deleteInstance( ws, fileId, true );
// TestTools.LocalFile.removeFile( localPath );
// ScmWorkspaceUtil.deleteWs( wsName, session );
// }
// } finally {
// if ( session != null ) {
// session.close();
// }
// }
// }
//
// }
