package com.sequoiacm.version;

import java.io.IOException;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1648:不同用户更新文件
 * @author wuyan
 * @createDate 2018.06.01
 * @updateUser ZhangYanan
 * @updateDate 2021.12.07
 * @updateRemark
 * @version v1.0
 */
public class UpdateContent1648 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session1 = null;
    private static ScmSession session2 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;
    private ScmId fileId = null;
    private String fileName = "file1648";
    private String newUsername = "admin1648";
    private String newPassword = "admin1648";
    private String roleName = "role1648";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session1 = TestScmTools.createSession( site );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        createScmUser();
        fileId = VersionUtils.createFileByStream( ws1, fileName, filedata );
        updateContent( ws2, updatedata );

        // check result
        int currentVersion = 2;
        int historyVersion = 1;
        VersionUtils.CheckFileContentByStream( ws1, fileName, currentVersion,
                updatedata );
        VersionUtils.CheckFileContentByStream( ws1, fileName, historyVersion,
                filedata );
        checkFileAtrributes();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws1, fileId, true );
                ScmFactory.User.deleteUser( session1, newUsername );
                ScmFactory.Role.deleteRole( session1, roleName );
            }
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
        }
    }

    private void createScmUser() throws ScmException {
        ScmUser user = ScmFactory.User.createUser( session1, newUsername,
                ScmUserPasswordType.LOCAL, newPassword );
        ScmRole role = ScmFactory.Role.createRole( session1, roleName, null );
        ScmUserModifier modifier = new ScmUserModifier();
        ScmResource rs = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        ScmFactory.Role.grantPrivilege( session1, role, rs,
                ScmPrivilegeType.ALL );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session1, user, modifier );
        session2 = TestScmTools.createSession( site, newUsername, newPassword );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session2 );
    }

    private void updateContent( ScmWorkspace ws, byte[] updateData )
            throws InterruptedException {
        boolean success = false;
        for ( int i = 0; i < 60; i++ ) {
            try {
                Thread.sleep( 1000 );
                VersionUtils.updateContentByStream( ws, fileId, updateData );
                success = true;
                break;
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(),
                        ScmError.OPERATION_UNAUTHORIZED, e.getMessage() );
            }
        }
        Assert.assertTrue( success, "getting priority spends over 60s!" );
    }

    private void checkFileAtrributes() throws ScmException {
        // check current version file user and size
        ScmFile curVersionFile = ScmFactory.File.getInstance( ws1, fileId );
        Assert.assertEquals( curVersionFile.getUpdateUser(), newUsername );
        Assert.assertEquals( curVersionFile.getSize(), updatedata.length );
        // check history version file user and size
        int histroyVersion = 1;
        ScmFile hisVersionFile = ScmFactory.File.getInstance( ws1, fileId,
                histroyVersion, 0 );
        Assert.assertEquals( hisVersionFile.getUser(),
                TestScmTools.scmUserName );
        Assert.assertEquals( hisVersionFile.getSize(), filedata.length );
    }
}