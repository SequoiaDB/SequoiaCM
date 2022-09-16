package com.sequoiacm.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;

/**
 * @description SCM-5161:角色有工作区的多种权限
 * @author ZhangYanan
 * @createDate 2022.08.31
 * @updateUser ZhangYanan
 * @updateDate 2022.08.31
 * @updateRemark
 * @version v1.0
 */

public class AuthWs5161 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private String username = "user5161";
    private String rolename = "Role5161";
    private String passwd = "5161";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;
    private WsWrapper wsp;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;
    private List< ScmPrivilegeType > expPrivilegeType = new ArrayList<>();
    private String expOldPrivilegeType = null;
    private List< ScmPrivilegeType > actPrivilegeType = new ArrayList<>();
    private String actOldPrivilegeType = null;
    private boolean runSuccess = false;

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
        session = TestScmTools.createSession( site );
        cleanEnv();
        prepare();
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // 验证单个权限,覆盖getPrivilegeType().getPriv()接口
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( session, role, rs,
                ScmPrivilegeType.READ );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );

        expPrivilegeType.add( ScmPrivilegeType.READ );
        expOldPrivilegeType = "READ";
        getRolesPrivilege();
        Assert.assertEquals( expPrivilegeType, actPrivilegeType );
        Assert.assertEquals( expOldPrivilegeType, actOldPrivilegeType );

        // 验证多个权限，覆盖getPrivilegeType().getPriv()接口
        ScmFactory.Role.grantPrivilege( session, role, rs,
                ScmPrivilegeType.DELETE );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );
        expPrivilegeType.add( ScmPrivilegeType.DELETE );
        expOldPrivilegeType = "UNKNOWN";
        getRolesPrivilege();
        Assert.assertEquals( expPrivilegeType, actPrivilegeType );
        Assert.assertEquals( expOldPrivilegeType, actOldPrivilegeType );
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                cleanEnv();
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void cleanEnv() throws ScmException {
        try {
            ScmFactory.Role.deleteRole( session, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }

        try {
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }

    private void prepare() throws Exception {
        user = ScmFactory.User.createUser( session, username,
                ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( session, rolename, null );
        rs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
    }

    private void getRolesPrivilege() throws Exception {
        ScmCursor< ScmPrivilege > scmPrivilegeScmCursor = null;
        try {
            ArrayList< ScmRole > roles = new ArrayList<>(
                    ScmFactory.User.getUser( session, username ).getRoles() );
            scmPrivilegeScmCursor = ScmFactory.Privilege
                    .listPrivileges( session, roles.get( 0 ) );
            while ( scmPrivilegeScmCursor.hasNext() ) {
                ScmPrivilege privilege = scmPrivilegeScmCursor.getNext();
                actOldPrivilegeType = privilege.getPrivilegeType().getPriv();
                actPrivilegeType = privilege.getPrivilegeTypes();
            }
        } finally {
            if ( scmPrivilegeScmCursor != null ) {
                scmPrivilegeScmCursor.close();
            }
        }
    }
}
