// package com.sequoiacm.tagretrieval;
//
// import com.sequoiacm.client.core.*;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.ScmTags;
// import com.sequoiacm.client.element.tag.ScmTag;
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
// * @Descreption 列取字符串标签，覆盖如下场景 a.精确匹配，如标签为a的标签 b.模糊匹配，如标签为?a*的标签
// * c.带特殊符号的标签文件，如使用转义符列取标签为\*?的标签
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/19
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6251 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6251";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "scmfile6251";
// private int fileSize = 10;
// private String filePath = null;
// private File localPath = null;
// private final static int fileNum = 10;
// private Random random = new Random();
//
// private List< ScmId > fileIdList = new ArrayList< ScmId >();
// List< String > tagList = Arrays.asList( "aba", "bac", "*c" );
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
// // a.精确匹配，如标签为aba的标签
// ScmCursor< ScmTag > scmCursor1 = ScmFactory.Tag.listTags( ws, "aba",
// null, 0, 10 );
// while ( scmCursor1.hasNext() ) {
// ScmTag scmTag = scmCursor1.getNext();
// Assert.assertEquals( scmTag.getName(), "aba" );
// }
//
// // b.模糊匹配，如标签为*a*的标签
// ScmCursor< ScmTag > scmCursor2 = ScmFactory.Tag.listTags( ws, "?a*",
// null, 0, 10 );
// while ( scmCursor2.hasNext() ) {
// ScmTag scmTag = scmCursor2.getNext();
// Assert.assertEquals( scmTag.getName(), "bac" );
// }
// // c.带特殊符号的标签文件，如使用转义符列取标签为\*?的标签
// ScmCursor< ScmTag > scmCursor3 = ScmFactory.Tag.listTags( ws, "\\*?",
// null, 0, 10 );
// while ( scmCursor3.hasNext() ) {
// ScmTag scmTag = scmCursor3.getNext();
// Assert.assertEquals( scmTag.getName(), "*c" );
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
// int tagIndex = random.nextInt( tagList.size() );
// ScmTags tags = new ScmTags();
// tags.addTag( tagList.get( tagIndex ) );
// scmfile.setTags( tags );
// scmfile.setMimeType( fileName );
// scmfile.setContent( filePath );
// ScmId fileId = scmfile.save();
// fileIdList.add( fileId );
// }
// }
// }
