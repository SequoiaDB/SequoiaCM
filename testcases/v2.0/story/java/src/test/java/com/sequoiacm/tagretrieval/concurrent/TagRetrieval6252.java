// package com.sequoiacm.tagretrieval.concurrent;
//
// import com.sequoiacm.client.core.*;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.listener.GroupTags;
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
// * @Descreption SCM-6252 :: 版本: 1 :: 标签库并发列取和新增标签
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/22
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6252 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6252";
// private String fileName = "scmfile6252";
// private int fileNum = 10;
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
// threadExec.addWorker( new addTag() );
// threadExec.addWorker( new ListTags() );
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
// private class addTag {
// @ExecuteOrder(step = 1)
// public void addTag() throws ScmException {
// int fileId = random.nextInt( fileIdList.size() );
// ScmSession session = ScmSessionUtils.createSession(rootSite);
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
// ScmFile scmFile = ScmFactory.File.getInstance( ws,
// fileIdList.get( fileId ) );
// scmFile.addCustomTag( "aa", "test6252" );
// session.close();
// }
// }
//
// private class ListTags {
// @ExecuteOrder(step = 1)
// public void listTags() throws ScmException {
// ScmSession session = ScmSessionUtils.createSession(rootSite);
// ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
// ScmCursor< String > scmCursor = ScmFactory.CustomTag
// .listCustomTagKey( ws, "a*", true, 0, 15 );
// int tagNum = 0;
// while ( scmCursor.hasNext() ) {
// scmCursor.getNext();
// tagNum++;
// }
// Boolean result = ( tagNum == 1 ) || ( tagNum == 2 );
// session.close();
// Assert.assertTrue( result );
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
// }
