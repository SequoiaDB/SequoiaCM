package com.sequoiacm.auth;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-6153:用户角色数量过多，超过Http请求头大小限制
 * @author yangjianbo
 * @date 2023/05/05
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class AuthServer_MultiRole6153 extends TestScmBase {
    private String wsName = "ws6153";
    private String userName = "user6153";
    private String pwd = "admin";
    private String roleName = "role6153";
    private int roleNum = 60;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmSession userSession = null;
    private ScmUser user = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024;
    private boolean runSuccess = false;

    @BeforeClass()
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        StringBuilder builder = new StringBuilder( roleName );
        for ( int i = 0; i < 50; i++ ) {
            builder.append( roleName );
        }
        roleName = builder.toString();
        Assert.assertTrue( roleName.getBytes().length > 200 );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        cleanEnv();
        prePare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws ScmException {
        createRoleAndGrantPrivilege();
        userSession = ScmSessionUtils.createSession( rootSite, userName, pwd );
        uploadFileAndDownload( wsName );
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
        for ( int i = 0; i < roleNum; i++ ) {
            try {
                ScmFactory.Role.deleteRole( session, roleName + i );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    throw e;
                }
            }
        }
    }

    private void prePare() throws Exception {
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        user = ScmAuthUtils.createUser( session, userName, pwd );
        ScmWorkspaceUtil.createWS( session, wsName, rootSite.getSiteId() );
    }

    private void createRoleAndGrantPrivilege() throws ScmException {
        ScmResource resource = ScmResourceFactory
                .createWorkspaceResource( wsName );
        for ( int i = 0; i < roleNum; i++ ) {
            ScmRole role = ScmFactory.Role.createRole( session, roleName + i,
                    roleName );
            ScmFactory.Role.grantPrivilege( session, role, resource,
                    ScmPrivilegeType.ALL );
            ScmFactory.User.alterUser( session, user,
                    new ScmUserModifier().addRole( role ) );
        }
    }

    private void uploadFileAndDownload( String fileName ) throws ScmException {
        ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace( wsName,
                userSession );
        ScmFile file = ScmFactory.File.createInstance( workspace );
        file.setFileName( fileName );
        file.setContent( filePath );
        ScmId fileID = file.save();
        ScmFile downloadFile = ScmFactory.File.getInstance( workspace, fileID );
        Assert.assertEquals( fileName, downloadFile.getFileName() );
    }

}
