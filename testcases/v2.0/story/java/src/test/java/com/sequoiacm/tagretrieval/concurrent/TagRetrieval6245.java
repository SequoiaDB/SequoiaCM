// package com.sequoiacm.tagretrieval.concurrent;
//
// import com.sequoiacm.client.common.ScmType;
// import com.sequoiacm.client.core.ScmFactory;
// import com.sequoiacm.client.core.ScmFile;
// import com.sequoiacm.client.core.ScmSession;
// import com.sequoiacm.client.core.ScmWorkspace;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.ScmTags;
// import com.sequoiacm.client.element.tag.ScmTagCondition;
// import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
// import com.sequoiacm.exception.ScmError;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
// import com.sequoiadb.threadexecutor.ThreadExecutor;
// import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
// import org.testng.Assert;
// import org.testng.annotations.AfterClass;
// import org.testng.annotations.BeforeClass;
// import org.testng.annotations.Test;
//
// import java.io.File;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
//
/// **
// * @Descreption SCM-6245 :: 版本: 1 :: 关闭标签检索时，执行标签操作
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/22
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6245 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6245";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "scmfile6245";
//
// private int fileSize = 10;
// private String filePath = null;
// private File localPath = null;
// private final static int fileNum = 10;
// private List< ScmId > fileIdList = new ArrayList< ScmId >();
//
// @BeforeClass
// private void setUp() throws Exception {
// localPath = new File( TestScmBase.dataDirectory + File.separator
// + TestTools.getClassName() );
// filePath = localPath + File.separator + "localFile_" + fileSize
// + ".txt";
// TestTools.LocalFile.removeFile( localPath );
// TestTools.LocalFile.createDir( localPath.toString() );
// TestTools.LocalFile.createFile( filePath, fileSize );
// rootSite = ScmInfo.getRootSite();
// session = ScmSessionUtils.createSession( rootSite );
// ScmWorkspaceUtil.deleteWs( wsName, session );
// int siteNum = ScmInfo.getSiteNum();
// ws = ScmWorkspaceUtil.createWS( session, wsName,true,
// siteNum );
// ScmWorkspaceUtil.wsSetPriority( session, wsName );
// writeScmFile();
// }
//
// @Test
// private void test() throws Exception {
// ThreadExecutor threadExec = new ThreadExecutor();
// threadExec.addWorker( new setDisEnableTagRetrieval() );
// threadExec.addWorker( new addTag() );
// threadExec.addWorker( new labelSearch() );
// threadExec.run();
// Thread.sleep( 1000 );
// ws = ScmFactory.Workspace.getWorkspace( wsName, session );
// Assert.assertEquals( ws.getTagRetrievalStatus(),
// ScmWorkspaceTagRetrievalStatus.DISABLED );
// runSuccess = true;
//
// }
//
// @AfterClass
// private void tearDown() throws Exception {
// try {
// if ( runSuccess || TestScmBase.forceClear ) {
// for ( ScmId fileId : fileIdList ) {
// ScmFactory.File.deleteInstance( ws, fileId, true );
// }
// TestTools.LocalFile.removeFile( localPath );
// ScmWorkspaceUtil.deleteWs( wsName, session );
// }
// } catch ( Exception e ) {
// Assert.fail( e.getMessage() );
// } finally {
// if ( session != null ) {
// session.close();
// }
// }
// }
//
// private void writeScmFile() throws ScmException, IOException {
// for ( int i = 0; i < fileNum; i++ ) {
// ScmFile scmfile = ScmFactory.File.createInstance( ws );
// TestTools.LocalFile.createFile( filePath, fileSize );
// scmfile.setFileName( fileName + "_" + i );
// scmfile.setAuthor( fileName );
// scmfile.setTitle( fileName );
// ScmTags tags = new ScmTags();
// tags.addTag( "tag6245" );
// scmfile.setTags( tags );
// scmfile.setMimeType( fileName );
// scmfile.setContent( filePath );
// ScmId fileId = scmfile.save();
// fileIdList.add( fileId );
// }
// }
//
// private class setDisEnableTagRetrieval {
// @ExecuteOrder(step = 1)
// public void execute() throws ScmException {
// try ( ScmSession session1 = ScmSessionUtils
// .createSession( rootSite )) {
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
// session1 );
// ws.setEnableTagRetrieval( false );
// } catch ( Exception e ) {
// throw e;
// }
// }
// }
//
// private class addTag {
// @ExecuteOrder(step = 1)
// public void execute() throws ScmException, IOException {
// ScmSession session = ScmSessionUtils.createSession( rootSite );
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
// session );
// for ( ScmId file : fileIdList ) {
// ScmFile scmFile = ScmFactory.File.getInstance( ws, file );
// scmFile.addTagV2( "test6245" );
// }
// session.close();
// }
// }
//
// private class labelSearch {
// @ExecuteOrder(step = 1)
// public void execute() throws ScmException, IOException {
// try ( ScmSession session = ScmSessionUtils
// .createSession( rootSite )) {
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
// session );
// ScmTagCondition tagCond = ScmTagConditionBuilder.builder()
// .or( ScmTagConditionBuilder.builder().tags()
// .contains( "tag6245" ).build() )
// .build();
// ScmFactory.Tag.countFile( ws, ScmType.ScopeType.SCOPE_ALL,
// tagCond, null );
// } catch ( ScmException e ) {
// if ( e.getErrorCode() != ScmError.OPERATION_UNSUPPORTED
// .getErrorCode() ) {
// throw e;
// }
// }
// }
// }
// }
