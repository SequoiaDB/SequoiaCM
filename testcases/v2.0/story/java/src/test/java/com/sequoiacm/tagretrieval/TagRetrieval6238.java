// package com.sequoiacm.tagretrieval;
//
// import com.sequoiacm.client.common.ScmType;
// import com.sequoiacm.client.core.*;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.ScmTags;
// import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
// import com.sequoiacm.client.element.privilege.ScmResource;
// import com.sequoiacm.client.element.privilege.ScmResourceFactory;
// import com.sequoiacm.client.element.tag.ScmTagCondition;
// import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
// import com.sequoiacm.exception.ScmError;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
// import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
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
// * @Descreption SCM-6238 :: 非管理员、非ALL权限修改工作区标签检索状态
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/18
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6238 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession adminsession = null;
// private ScmSession test6238session = null;
// private String wsName = "ws6238";
// private String roleName = "test_6238role";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "scmfile6238";
// private ScmId fileId = null;
// private int fileSize = 1;
// private File localPath = null;
// private String filePath = null;
// private String testUserName = "test6238";
// private String testPassword = "test6238";
//
// @BeforeClass
// private void setUp() throws Exception {
// rootSite = ScmInfo.getRootSite();
// adminsession = ScmSessionUtils.createSession( rootSite );
// ScmWorkspaceUtil.deleteWs( wsName, adminsession );
//
// localPath = new File( TestScmBase.dataDirectory + File.separator
// + TestTools.getClassName() );
// filePath = localPath + File.separator + "localFile_" + fileSize
// + ".txt";
// TestTools.LocalFile.removeFile( localPath );
// TestTools.LocalFile.createDir( localPath.toString() );
// TestTools.LocalFile.createFile( filePath, fileSize );
// try {
// ScmWorkspaceUtil.deleteWs( wsName, adminsession );
// ScmFactory.Role.getRole( adminsession, roleName );
// ScmFactory.Role.deleteRole( adminsession, roleName );
// ScmAuthUtils.deleteUser( adminsession, testUserName );
// } catch ( ScmException e ) {
// if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
// throw e;
// }
// }
// }
//
// @Test
// private void test()
// throws ScmException, InterruptedException, TimeoutException {
// int siteNum = ScmInfo.getSiteNum();
// // 创建工作区 默认关闭标签检索
// ws = ScmWorkspaceUtil.createWS( adminsession, wsName, siteNum );
// ScmWorkspaceUtil.wsSetPriority( adminsession, wsName );
// ScmFile file = ScmFactory.File.createInstance( ws );
// file.setContent( filePath );
// ScmTags scmTags = new ScmTags();
// scmTags.addTag( "tag1" );
// file.setTags( scmTags );
// file.setFileName( fileName );
// fileId = file.save();
// // 创建新用户并授权
// ScmRole role = ScmFactory.Role.createRole( adminsession, roleName,
// "desc" );
// ScmResource resource = ScmResourceFactory
// .createWorkspaceResource( wsName );
// ScmFactory.Role.grantPrivilege( adminsession, role, resource,
// ScmPrivilegeType.READ );
// ScmUser user = ScmAuthUtils.createUser( adminsession, testUserName,
// testPassword );
// ScmFactory.User.alterUser( adminsession, user,
// new ScmUserModifier().addRole( role ) );
// test6238session = ScmSessionUtils.createSession( rootSite, testUserName,
// testPassword );
//
// ScmTagCondition tagCond = ScmTagConditionBuilder.builder()
// .or( ScmTagConditionBuilder.builder().tags().contains( "tag1" )
// .build() )
// .build();
// // 新用户进行标签检索
// try {
// ScmFactory.Tag.countFile( ws, ScmType.ScopeType.SCOPE_ALL, tagCond,
// null );
// } catch ( ScmException e ) {
// if ( e.getErrorCode() != ScmError.OPERATION_UNSUPPORTED
// .getErrorCode() ) {
// throw e;
// }
// }
// // 使用管理员开启标签检索
// ws.setEnableTagRetrieval( true );
// TagRetrievalUtils.waitForTagRetrievalStatus( adminsession, wsName, 300,
// ScmWorkspaceTagRetrievalStatus.ENABLED );
// try {
// // 按标签检索文件
// long countFile = ScmFactory.Tag.countFile( ws,
// ScmType.ScopeType.SCOPE_ALL, tagCond, null );
// Assert.assertEquals( countFile, 1 );
// runSuccess = true;
// } catch ( ScmException e ) {
// e.printStackTrace();
// }
// }
//
// @AfterClass
// private void tearDown() throws Exception {
// try {
// if ( runSuccess || TestScmBase.forceClear ) {
// ScmFactory.File.deleteInstance( ws, fileId, true );
// TestTools.LocalFile.removeFile( localPath );
// ScmWorkspaceUtil.deleteWs( wsName, adminsession );
// ScmAuthUtils.deleteUser( adminsession, testUserName );
// ScmFactory.Role.deleteRole( adminsession, roleName );
// }
// } finally {
// if ( adminsession != null ) {
// adminsession.close();
// }
// if ( test6238session != null ) {
// test6238session.close();
// }
// }
// }
//
// }
