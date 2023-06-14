// package com.sequoiacm.tagretrieval;
//
// import com.sequoiacm.client.core.*;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.tag.ScmCustomTag;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.listener.GroupTags;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
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
// * @Descreption SCM-6249 :: 版本: 1 :: 标签库按key列取自由标签 按key匹配列取自由标签，覆盖如下场景
// * a.精确匹配，如key为a的标签 b.模糊匹配，如key为?a*的标签
// * c.带特殊符号的标签文件，如使用转义符列取key为\*?的标签
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/18
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6249 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6249";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "scmfile6249";
// private List< ScmId > fileIdList = new ArrayList< ScmId >();
// private int fileSize = 10;
// private File localPath = null;
// private final static int fileNum = 10;
// private String filePath = null;
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
// ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
// ScmWorkspaceUtil.wsSetPriority( session, wsName );
// writeScmFile();
// }
//
// @Test(groups = { GroupTags.base })
// private void test() throws ScmException, InterruptedException {
//
// // 1.精确匹配，如key为aba的标签
// ScmCursor< ScmCustomTag > scmCursor1 = ScmFactory.CustomTag
// .listCustomTag( ws, "aba", null, null, 0, 10 );
//
// while ( scmCursor1.hasNext() ) {
// ScmCustomTag scmCustomTag = scmCursor1.getNext();
// Assert.assertEquals( scmCustomTag.getKey(), "aba" );
// }
// // 2.模糊匹配，如key为?a*的标签
// ScmCursor< ScmCustomTag > scmCursor2 = ScmFactory.CustomTag
// .listCustomTag( ws, "?a*", null, null, 0, 10 );
//
// while ( scmCursor2.hasNext() ) {
// ScmCustomTag scmCustomTag = scmCursor2.getNext();
// Assert.assertEquals( scmCustomTag.getKey(), "bac" );
// }
//
// // 3.带特殊符号的标签文件，如使用转义符列取key为\*?的标签
// ScmCursor< ScmCustomTag > scmCursor3 = ScmFactory.CustomTag
// .listCustomTag( ws, "\\*?", null, null, 0, 10 );
// while ( scmCursor3.hasNext() ) {
// ScmCustomTag scmCustomTag = scmCursor3.getNext();
// Assert.assertEquals( scmCustomTag.getKey(), "*c" );
// }
// scmCursor1.close();
// scmCursor2.close();
// scmCursor3.close();
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
// } catch ( Exception e ) {
// Assert.fail( e.getMessage() );
// } finally {
// if ( session != null ) {
// session.close();
// }
//
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
// customTag.put( "aba", "a" + i );
// customTag.put( "bac", "b" + i );
// customTag.put( "*c", "b" + i );
// scmfile.setCustomTag( customTag );
// scmfile.setMimeType( fileName );
// scmfile.setContent( filePath );
// ScmId fileId = scmfile.save();
// fileIdList.add( fileId );
// }
// }
// }
