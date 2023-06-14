// package com.sequoiacm.tagretrieval.concurrent;
//
// import com.sequoiacm.client.common.ScmType;
// import com.sequoiacm.client.core.*;
// import com.sequoiacm.client.element.ScmFileBasicInfo;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.ScmTags;
// import com.sequoiacm.client.element.tag.ScmTagCondition;
// import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
// import com.sequoiacm.client.exception.ScmException;
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
// import java.util.*;
//
/// **
// * @Descreption SCM-6253 :: 版本: 1 :: 标签库并发查询和新增标签文件
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/22
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6253 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6253";
// private String fileName1 = "scmfile6253a";
// private String fileName2 = "scmfile6253b";
// private int fileNum = 5;
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String filePath = null;
// private File localPath = null;
// private int fileSize = 10;
// private Random random = new Random();
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
// ws = ScmWorkspaceUtil.createWS( session, wsName, true, siteNum );
// ScmWorkspaceUtil.wsSetPriority( session, wsName );
// writeScmFile();
// }
//
// @Test
// private void test() throws Exception {
// ThreadExecutor threadExec = new ThreadExecutor();
// threadExec.addWorker( new addFile() );
// threadExec.addWorker( new labelSearch() );
// threadExec.run();
//
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
// } finally {
// if ( session != null ) {
// session.close();
// }
// }
// }
//
// private class addFile {
// @ExecuteOrder(step = 1)
// public void addFile() throws ScmException, IOException {
// try ( ScmSession session = ScmSessionUtils
// .createSession( rootSite )) {
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
// session );
// for ( int i = 0; i < fileNum; i++ ) {
// ScmFile scmfile = ScmFactory.File.createInstance( ws );
// TestTools.LocalFile.createFile( filePath, fileSize );
// scmfile.setFileName( fileName2 + "_" + i );
// scmfile.setAuthor( fileName2 );
// scmfile.setTitle( fileName2 );
// ScmTags tags = new ScmTags();
// tags.addTag( "test" + i );
// scmfile.setTags( tags );
// scmfile.setMimeType( fileName2 );
// scmfile.setContent( filePath );
// ScmId fileId = scmfile.save();
// fileIdList.add( fileId );
// }
// }
// }
// }
//
// private class labelSearch {
// @ExecuteOrder(step = 1)
// public void labelSearch() throws ScmException {
// List< String > fileNames = new ArrayList<>();
// ScmTagCondition tagCond = ScmTagConditionBuilder.builder().tags()
// .contains( "test2" ).build();
// int countFile = 0;
// try ( ScmSession session = ScmSessionUtils
// .createSession( rootSite )) {
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
// session );
// ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag
// .searchFile( ws, ScmType.ScopeType.SCOPE_ALL, tagCond,
// null, null, 0, -1 );
// while ( scmCursor.hasNext() ) {
// String fileName = scmCursor.getNext().getFileName();
// fileNames.add( fileName );
// countFile++;
// }
// Boolean fileResult = ( fileNames
// .contains( fileName1 + "_" + 2 ) )
// || ( fileNames.contains( fileName2 + "_" + 2 ) );
// Boolean result = ( countFile == 1 ) || ( countFile == 2 );
// scmCursor.close();
// Assert.assertTrue( fileResult );
// Assert.assertTrue( result );
// }
// }
// }
//
// private void writeScmFile() throws ScmException, IOException {
// for ( int i = 0; i < fileNum; i++ ) {
// ScmFile scmfile = ScmFactory.File.createInstance( ws );
// TestTools.LocalFile.createFile( filePath, fileSize );
// scmfile.setFileName( fileName1 + "_" + i );
// scmfile.setAuthor( fileName1 );
// scmfile.setTitle( fileName1 );
// ScmTags tags = new ScmTags();
// tags.addTag( "test" + i );
// scmfile.setTags( tags );
// scmfile.setMimeType( fileName1 );
// scmfile.setContent( filePath );
// ScmId fileId = scmfile.save();
// fileIdList.add( fileId );
// }
// }
// }
