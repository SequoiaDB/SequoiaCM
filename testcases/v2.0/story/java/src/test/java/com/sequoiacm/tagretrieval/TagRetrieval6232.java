// package com.sequoiacm.tagretrieval;
//
// import com.sequoiacm.client.common.ScmType;
// import com.sequoiacm.client.core.*;
// import com.sequoiacm.client.element.ScmId;
// import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
// import com.sequoiacm.client.element.privilege.ScmResource;
// import com.sequoiacm.client.element.privilege.ScmResourceFactory;
// import com.sequoiacm.client.element.tag.ScmTagCondition;
// import com.sequoiacm.client.element.tag.ScmTagConditionBuilder;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.exception.ScmError;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.listener.GroupTags;
// import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
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
// * @Descreption SCM-6232 :: 版本: 1 :: 标签检索权限测试
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/18
// * @UpdateRemark
// * @Version
// */
// public class TagRetrieval6232 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession adminsession = null;
// private ScmSession testsession = null;
// private String wsName = "ws6232";
// private ScmWorkspace ws = null;
// private SiteWrapper rootSite = null;
// private String fileName = "file6232";
// private final int fileNum = 10;
// private List< ScmId > fileIdList = new ArrayList< ScmId >();
// private int fileSize = 10;
// private File localPath = null;
// private String filePath = null;
// private String roleName = "test_6232role";
// private String testUserName = "test6232";
// private String testPassword = "test6232";
//
// @BeforeClass
// private void setUp() throws Exception {
// localPath = new File( TestScmBase.dataDirectory + File.separator
// + TestTools.getClassName() );
// filePath = localPath + File.separator + "localFile_" + fileSize
// + ".txt";
// TestTools.LocalFile.removeFile( localPath );
// TestTools.LocalFile.createDir( localPath.toString() );
// rootSite = ScmInfo.getRootSite();
// adminsession = ScmSessionUtils.createSession( rootSite );
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
// @Test(groups = { GroupTags.base })
// private void test() throws ScmException, InterruptedException, IOException {
// int siteNum = ScmInfo.getSiteNum();
// // 1、工作区内创建多个标签文件
// ws = ScmWorkspaceUtil.createWS( adminsession, wsName, true, siteNum );
// ScmWorkspaceUtil.wsSetPriority( adminsession, wsName );
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
// // 2、创建新用户，仅赋予工作区读权限
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
// testsession = ScmSessionUtils.createSession( rootSite, testUserName,
// testPassword );
// // 3、使用标签检索功能，按标签查询工作区内文件
// ScmTagCondition tagCond = ScmTagConditionBuilder.builder().customTag()
// // 设置自由标签值不忽略大小写，启用通配
// .contains( "aaa", "a1", false, true ).build();
// long countFile = ScmFactory.Tag.countFile( ws,
// ScmType.ScopeType.SCOPE_CURRENT, tagCond, null );
// Assert.assertEquals( countFile, 1 );
//
// // 4、修改新用户权限，取消读权限
// ScmFactory.Role.revokePrivilege( adminsession, role, resource,
// ScmPrivilegeType.READ );
// // 5、使用标签检索功能，按标签查询工作区内文件
// try {
// ScmFactory.Tag.countFile( ws, ScmType.ScopeType.SCOPE_ALL, tagCond,
// null );
// } catch ( ScmException e ) {
// if ( e.getErrorCode() != ScmError.OPERATION_UNSUPPORTED
// .getErrorCode() ) {
// throw e;
// }
// }
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
// ScmWorkspaceUtil.deleteWs( wsName, adminsession );
// ScmAuthUtils.deleteUser( adminsession, testUserName );
// ScmFactory.Role.deleteRole( adminsession, roleName );
// }
// } finally {
// if ( adminsession != null ) {
// adminsession.close();
// }
// if ( testsession != null ) {
// testsession.close();
// }
// }
// }
//
// }
