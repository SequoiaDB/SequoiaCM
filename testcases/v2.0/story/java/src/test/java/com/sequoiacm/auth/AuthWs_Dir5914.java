package com.sequoiacm.auth;

import java.io.File;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @descreption SCM-5914:工作区和目录权限组合测试
 * @author YiPan
 * @date 2023/2/3
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class AuthWs_Dir5914 extends TestScmBase {
    private boolean runSuccess;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private ScmSession newSession;
    private ScmWorkspace newWs;
    private String username = "user5914";
    private String rolename = "role5914";
    private String passwd = "pwd5914";
    private String dirName = "/dir5914";
    private String fileName = "file5914";
    private BSONObject query;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private ScmUser user;
    private ScmRole role;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        query = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
        if ( ScmFactory.Directory.isInstanceExist( ws, dirName ) ) {
            ScmFactory.Directory.deleteInstance( ws, dirName );
        }

        prepareUser();
    }

    @Test
    private void test() throws Exception {
        // 新用户创建session和ws
        newSession = ScmSessionUtils.createSession( site, username, passwd );
        newWs = ScmFactory.Workspace.getWorkspace( wsp.getName(), newSession );

        // 赋予工作区CREATE、UPDATE、READ
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        ScmResource wsRes = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        ScmFactory.Role.grantPrivilege( session, role, wsRes,
                ScmPrivilegeType.CREATE );
        ScmFactory.Role.grantPrivilege( session, role, wsRes,
                ScmPrivilegeType.UPDATE );
        ScmFactory.Role.grantPrivilege( session, role, wsRes,
                ScmPrivilegeType.READ );

        // 赋予目录READ、DELETE
        ScmDirectory dir = ScmFactory.Directory.createInstance( ws, dirName );
        ScmResource dirRes = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), dirName );
        ScmFactory.Role.grantPrivilege( session, role, dirRes,
                ScmPrivilegeType.READ );
        ScmFactory.Role.grantPrivilege( session, role, dirRes,
                ScmPrivilegeType.DELETE );

        // 校验上传文件到目录
        checkUpdateFile( dir );

        // 再次上传到目录，覆盖文件
        checkUpdateFile( dir );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFileUtils.cleanFile( wsp, query );
                if ( ScmFactory.Directory.isInstanceExist( ws, dirName ) ) {
                    ScmFactory.Directory.deleteInstance( ws, dirName );
                }
                ScmFactory.Role.deleteRole( session, role );
                ScmFactory.User.deleteUser( session, user );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( newSession != null ) {
                newSession.close();
            }
        }
    }

    private void prepareUser() throws ScmException {
        user = ScmAuthUtils.createUser( session, username, passwd );
        role = ScmAuthUtils.createRole( session, rolename );
    }

    private void checkUpdateFile( ScmDirectory dir )
            throws InterruptedException, ScmException {
        boolean flag = false;
        for ( int i = 0; i < 30; i++ ) {
            try {
                ScmFile file = ScmFactory.File.createInstance( newWs );
                file.setFileName( fileName );
                file.setDirectory( dir );
                file.setContent( filePath );
                file.save( new ScmUploadConf( true, false ) );
                flag = true;
                break;
            } catch ( ScmException e ) {
                if ( !e.getError().equals( ScmError.OPERATION_UNAUTHORIZED ) ) {
                    throw e;
                }
                Thread.sleep( 1000 );
            }
        }
        Assert.assertTrue( flag, "check grantPrivilege time out" );
    }
}
