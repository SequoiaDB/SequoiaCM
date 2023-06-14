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
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
/// **
// * @Descreption SCM-6250 :: 版本: 1 :: 标签库按value列取标签 a.精确匹配，如value为a的标签
// * b.模糊匹配，如value为?a*的标签 c.带特殊符号的标签文件，如使用转义符列取value为\*?的标签
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/19
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6250 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6250";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "scmfile6250";
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
// ws = ScmWorkspaceUtil.createWS( session, wsName, siteNum );
// ScmWorkspaceUtil.wsSetPriority( session, wsName );
// writeScmFile();
// }
//
// @Test(groups = { GroupTags.base })
// private void test() throws ScmException, InterruptedException {
// // a.精确匹配，如value为aba的标签
// ScmCursor< ScmCustomTag > scmCursor1 = ScmFactory.CustomTag
// .listCustomTag( ws, null, "aba", null, 0, 10 );
// while ( scmCursor1.hasNext() ) {
// ScmCustomTag scmCustomTag = scmCursor1.getNext();
// Assert.assertEquals( scmCustomTag.getValue(), "aba" );
// }
// // b.模糊匹配，如value为?a*的标签
// ScmCursor< ScmCustomTag > scmCursor2 = ScmFactory.CustomTag
// .listCustomTag( ws, null, "?a*", null, 0, 10 );
// while ( scmCursor2.hasNext() ) {
// ScmCustomTag scmCustomTag = scmCursor2.getNext();
// Assert.assertEquals( scmCustomTag.getValue(), "bac" );
// }
//
// // c.带特殊符号的标签文件，如使用转义符列取value为\*?的标签
// ScmCursor< ScmCustomTag > scmCursor3 = ScmFactory.CustomTag
// .listCustomTag( ws, null, "\\*?", null, 0, 10 );
// while ( scmCursor3.hasNext() ) {
// ScmCustomTag scmCustomTag = scmCursor3.getNext();
// Assert.assertEquals( scmCustomTag.getValue(), "*c" );
// }
// scmCursor1.close();
// scmCursor2.close();
// scmCursor3.close();
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
// throw e;
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
// customTag.put( "a" + i, "aba" );
// customTag.put( "b" + i, "bac" );
// customTag.put( "c" + i, "*c" );
// scmfile.setCustomTag( customTag );
// scmfile.setMimeType( fileName );
// scmfile.setContent( filePath );
// ScmId fileId = scmfile.save();
// fileIdList.add( fileId );
// }
// }
// }
