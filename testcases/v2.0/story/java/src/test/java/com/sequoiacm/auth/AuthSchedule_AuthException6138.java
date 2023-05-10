package com.sequoiacm.auth;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleMoveFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-6138:创建调度任务(用户拥有 read 权限、用户拥有 all
 *              权限、删除用户不存在的角色、删除不存在的用户、创建同名用户依次赋予无权限,read 权限,all 权限 5种场景)
 * @Author yangjianbo
 * @CreateDate 2023/4/10
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class AuthSchedule_AuthException6138 extends TestScmBase {
    private ScmSession session = null;
    private ScmSession userSession;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmWorkspace ws;
    private ScmUser user;
    private ScmRole role;
    private ScmResource resource;
    private BSONObject queryCond;
    private String userName = "AuthSchedule6138UserName";
    private String passwd = "AuthSchedule6138Pwd";
    private String roleName = "AuthSchedule6138RoleName";
    private String wsName = "AuthSchedule6138WsName";
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;
    private final static int fileNum = 10;
    private ScmSchedule sche;
    private ScmScheduleContent content = null;
    private List< String > fileNameList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = ScmSessionUtils.createSession( rootSite );
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        cleanEnv();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        testReadPrivilege();
        testAllPrivilege();
        testDeleteUserNotExistRole();
        testDeleteNotExistUser();
        testReCreateUser();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                cleanEnv();
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( userSession != null ) {
                userSession.close();
            }
        }
    }

    private void cleanEnv() throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmAuthUtils.deleteUser( session, userName );
        try {
            ScmFactory.Role.deleteRole( session, roleName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }
    private void testReCreateUser() throws Exception {
        ScmFactory.User.deleteUser( session, userName );
        user = ScmFactory.User.createUser( session, userName,
                ScmUserPasswordType.LOCAL, passwd );
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
        reCreateUserNoPrivilege();

        ScmFactory.Role.revokePrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        reCreateUserReadPrivilege();
        reCreateUserAllPrivilege();
    }

    private void reCreateUserAllPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        createSchedule( "reCreateUserAllPrivilege" );
    }

    private void reCreateUserReadPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.READ );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        try {
            createSchedule( "reCreateUserReadPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void reCreateUserNoPrivilege() throws Exception {
        try {
            createSchedule( "reCreateUserNoPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteNotExistUser() throws ScmException {
        try {
            ScmFactory.User.deleteUser( session, userName + "_NotExist" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_NOT_FOUND
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void testDeleteUserNotExistRole() throws ScmException {
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().delRole( role + "_NotExist" ) );
    }

    private void testAllPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        createSchedule( "testAllPrivilege" );
    }

    private void testReadPrivilege() throws Exception {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.READ );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        try {
            createSchedule( "testReadPrivilege" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException exception ) {
            if ( exception.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw exception;
            }
        }
    }

    private void createSchedule( String name ) throws Exception {
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        // 创建迁移并清理任务
        content = new ScmScheduleMoveFileContent( rootSite.getSiteName(),
                branchSite.getSiteName(), "0d", queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, 120000, ScmDataCheckLevel.WEEK,
                false, true );

        // 启动迁移并清理调度任务
        String cron = "0/1 * * * * ?";
        sche = ScmSystem.Schedule.create( userSession, wsName,
                ScheduleType.MOVE_FILE, name, "", content, cron );
        checkResult();
    }

    public void checkResult() throws Exception {
        ScmScheduleUtils.waitForTask( sche, 2 );
        // 校验统计记录
        List< ScmTask > successTasks = ScmScheduleUtils.getSuccessTasks( sche );
        ScmTask task = successTasks.get( 0 );
        Assert.assertEquals( task.getEstimateCount(), -1 );
        Assert.assertEquals( task.getActualCount(), fileNum );
        Assert.assertEquals( task.getSuccessCount(), fileNum );
        Assert.assertEquals( task.getFailCount(), 0 );
        Assert.assertEquals( task.getProgress(), 100 );
    }

    private void prepareFile() throws ScmException {
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        if ( userSession != null ) {
            userSession.close();
        }
        userSession = ScmSessionUtils.createSession( rootSite, userName,
                passwd );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        ws = ScmFactory.Workspace.getWorkspace( ws.getName(), userSession );

        for ( String fileName : fileNameList ) {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + i );
                file.setAuthor( fileName );
                file.setContent( filePath );
                file.save();
            }
        }

        ScmFactory.Role.revokePrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        user = ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().delRole( role ) );
    }

    private void prepare() throws Exception {
        fileNameList.add( "testReadPrivilege" );
        fileNameList.add( "testAllPrivilege" );
        fileNameList.add( "testDeleteUserNotExistRole" );
        fileNameList.add( "testDeleteNotExistUser" );
        fileNameList.add( "reCreateUserNoPrivilege" );
        fileNameList.add( "reCreateUserReadPrivilege" );
        fileNameList.add( "reCreateUserAllPrivilege" );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< ScmDataLocation > siteLocations = ScmWorkspaceUtil
                .getDataLocationList( rootSite.getSiteId() );
        List< ScmDataLocation > locationList = ScmWorkspaceUtil
                .getDataLocationList( ScmInfo.getAllSites().size() );
        for ( ScmDataLocation dataLocation : locationList ) {
            if ( branchSite.getSiteName()
                    .equals( dataLocation.getSiteName() ) ) {
                siteLocations.add( dataLocation );
            }
        }

        ScmWorkspaceConf conf = new ScmWorkspaceConf( wsName,
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ),
                siteLocations );
        ws = ScmWorkspaceUtil.createWS( session, conf );
        resource = ScmResourceFactory.createWorkspaceResource( wsName );
        user = ScmAuthUtils.createUser( session, userName, passwd );
        role = ScmAuthUtils.createRole( session, roleName );

        prepareFile();
    }

}