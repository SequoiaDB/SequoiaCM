// package com.sequoiacm.tagretrieval;
//
// import java.io.File;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
//
// import org.testng.Assert;
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
// import com.sequoiacm.testcommon.ScmInfo;
// import com.sequoiacm.testcommon.ScmSessionUtils;
// import com.sequoiacm.testcommon.SiteWrapper;
// import com.sequoiacm.testcommon.TestScmBase;
// import com.sequoiacm.testcommon.TestTools;
// import com.sequoiacm.testcommon.listener.GroupTags;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
// import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
//
/// **
// * @Descreption SCM-6323:使用忽略大小写来按标签检索文件
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/26
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6323 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private String wsName = "ws6323";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName1 = "scmfile6223_1";
// private String fileName2 = "scmfile6223_2";
// private String fileName3 = "scmfile6223_3";
// private String fileName4 = "scmfile6223_4";
// private int fileSize = 100;
// private String filePath = null;
// private File localPath = null;
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
// @Test(groups = { GroupTags.base })
// private void test() throws ScmException, InterruptedException {
// ws = ScmFactory.Workspace.getWorkspace( wsName, session );
// String[] expFileList = { fileName1, fileName2, fileName3 };
// ScmTagCondition condition = ScmTagConditionBuilder.builder().tags()
// .contains( "abc", true, true ).build();
// TagRetrievalUtils.waitFileTagBuild( ws, condition, expFileList.length );
// ScmCursor< ScmFileBasicInfo > scmCursor = ScmFactory.Tag.searchFile( ws,
// ScmType.ScopeType.SCOPE_CURRENT, condition, null, null, 0, -1 );
// TagRetrievalUtils.checkSearchFileList( scmCursor, expFileList );
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
// }
// }
//
// private ScmTags createTag( String... tags ) throws ScmException {
// ScmTags scmTags = new ScmTags();
// for ( String tag : tags ) {
// scmTags.addTag( tag );
// }
// return scmTags;
// }
//
// private void writeScmFile() throws ScmException, IOException {
// String[] fileNames = { fileName1, fileName2, fileName3, fileName4 };
// ScmTags[] fileTags = { createTag( "tag1", "tag2", "aBc" ),
// createTag( "tag2", "ABC" ), createTag( "tag2", "abc" ),
// createTag( "tag1" ) };
// for ( int i = 0; i < 4; i++ ) {
// ScmFile scmFile = ScmFactory.File.createInstance( ws );
// TestTools.LocalFile.createFile( filePath, fileSize );
// scmFile.setFileName( fileNames[ i ] );
// scmFile.setMimeType( fileNames[ i ] );
// scmFile.setTags( fileTags[ i ] );
// scmFile.setContent( filePath );
// ScmId fileId = scmFile.save();
// fileIdList.add( fileId );
// }
// }
// }
