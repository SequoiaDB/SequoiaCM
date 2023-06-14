// package com.sequoiacm.tagretrieval.concurrent;
//
// import java.io.File;
//
// import org.testng.annotations.AfterClass;
// import org.testng.annotations.BeforeClass;
// import org.testng.annotations.Test;
//
// import com.sequoiacm.client.common.ScmType;
// import com.sequoiacm.client.core.ScmCursor;
// import com.sequoiacm.client.core.ScmFactory;
// import com.sequoiacm.client.core.ScmFile;
// import com.sequoiacm.client.core.ScmSession;
// import com.sequoiacm.client.core.ScmWorkspace;
// import com.sequoiacm.client.element.ScmFileBasicInfo;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.ScmTags;
// import com.sequoiacm.client.element.tag.ScmTagCondition;
// import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
// import com.sequoiacm.exception.ScmError;
// import com.sequoiacm.testcommon.ScmInfo;
// import com.sequoiacm.testcommon.ScmSessionUtils;
// import com.sequoiacm.testcommon.SiteWrapper;
// import com.sequoiacm.testcommon.TestScmBase;
// import com.sequoiacm.testcommon.TestTools;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
// import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
// import com.sequoiadb.threadexecutor.ThreadExecutor;
// import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
//
/// **
// * @Descreption SCM-6241 :: 版本: 1 :: 并发开启标签检索
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/19
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6241 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6241";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "scmfile6241";
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
// @Test
// private void test() throws Exception {
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
// ThreadExecutor threadExec = new ThreadExecutor();
// threadExec.addWorker( new setEnableTagRetrieval() );
// threadExec.addWorker( new setEnableTagRetrieval() );
// threadExec.run();
// TagRetrievalUtils.waitForTagRetrievalStatus( session, wsName, 100,
// ScmWorkspaceTagRetrievalStatus.ENABLED );
// String[] expFileList = { fileName };
// ScmTagCondition tagCond = ScmTagConditionBuilder.builder()
// .or( ScmTagConditionBuilder.builder().tags().contains( "tag1" )
// .build() )
// .build();
// TagRetrievalUtils.waitFileTagBuild( ws, tagCond, expFileList.length );
// ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag.searchFile( ws,
// ScmType.ScopeType.SCOPE_ALL, tagCond, null, null, 0, -1 );
// TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
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
// private class setEnableTagRetrieval {
// @ExecuteOrder(step = 1)
// public void execute() throws ScmException {
// try ( ScmSession session1 = ScmSessionUtils
// .createSession( rootSite )) {
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
// session1 );
// ws.setEnableTagRetrieval( true );
// } catch ( ScmException e ) {
// if ( e.getErrorCode() != ScmError.OPERATION_UNSUPPORTED
// .getErrorCode() ) {
// throw e;
// }
// }
// }
// }
// }
