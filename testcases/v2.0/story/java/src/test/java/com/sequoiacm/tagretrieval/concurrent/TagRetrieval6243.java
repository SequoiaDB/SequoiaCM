// package com.sequoiacm.tagretrieval.concurrent;
//
// import com.sequoiacm.client.common.ScmType;
// import com.sequoiacm.client.core.ScmFactory;
// import com.sequoiacm.client.core.ScmFile;
// import com.sequoiacm.client.core.ScmSession;
// import com.sequoiacm.client.core.ScmWorkspace;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.tag.ScmTagCondition;
// import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
// import com.sequoiacm.exception.ScmError;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
// import org.testng.Assert;
// import org.testng.annotations.AfterClass;
// import org.testng.annotations.BeforeClass;
// import org.testng.annotations.Test;
//
// import java.io.File;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
/// **
// * @Descreption SCM-6243 :: 版本: 1 :: 并发开启、关闭标签检索
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/19
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6243 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6243";
// private String fileName = "scmfile6243";
// private int fileNum = 10;
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String filePath = null;
// private File localPath = null;
// private int fileSize = 10;
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
// }
//
// @Test
// private void test() throws Exception {
// // 1.创建工作区
// int siteNum = ScmInfo.getSiteNum();
// ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
// // 2.工作区内创建多个标签文件
// ScmWorkspaceUtil.wsSetPriority( session, wsName );
// writeScmFile();
//
// // 3.并发开启和关闭工作区标签检索(工作区状态为索引创建中时关闭)
// ws.setEnableTagRetrieval( true );
// ws.setEnableTagRetrieval( false );
// TagRetrievalUtils.waitForTagRetrievalStatus( session, wsName, 10,
// ScmWorkspaceTagRetrievalStatus.DISABLED );
// // 4.查看执行结果
// ScmTagCondition tagCond = ScmTagConditionBuilder.builder().customTag()
// // 设置自由标签值不忽略大小写，启用通配
// .contains( "aaa", "a1", false, true ).build();
// try {
// ScmFactory.Tag.searchFile( ws, ScmType.ScopeType.SCOPE_ALL, tagCond,
// null, null, 0, -1 );
// //预期失败 如果执行成功,抛出异常
// Assert.fail(
// "Expected failure but the code executed successfully." );
// } catch ( ScmException e ) {
// if ( e.getErrorCode() != ScmError.OPERATION_UNSUPPORTED
// .getErrorCode() ) {
// throw e;
// }
// }
// // 5.再次开启工作区标签检索
// ws.setEnableTagRetrieval( true );
// TagRetrievalUtils.waitForTagRetrievalStatus( session, wsName, 100,
// ScmWorkspaceTagRetrievalStatus.ENABLED );
// // 6.按标签检索文件
// long countFile = ScmFactory.Tag.countFile( ws,
// ScmType.ScopeType.SCOPE_ALL, tagCond, null );
// Assert.assertEquals( countFile, 1 );
// runSuccess = true;
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
// Map< String, String > customTag = new HashMap<>();
// customTag.put( "aaa", "a" + i );
// scmfile.setCustomTag( customTag );
// scmfile.setMimeType( fileName );
// scmfile.setContent( filePath );
// ScmId fileId = scmfile.save();
// fileIdList.add( fileId );
// }
// }
//
// }
