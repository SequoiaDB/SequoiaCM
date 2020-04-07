/**
 *
 */
package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONException;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1376.java  不同用户断点续传文件 
 * @author luweikang
 * @date 2018年5月19日
 */
public class BreakpointFile1376 extends TestScmBase {

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session1 = null;
    private static ScmSession session2 = null;
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;

    private String fileName = "scmfile1376";
    private int fileSize = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath = null;
    private String newUsername = "admin1376";
    private String newPassword = "admin1376";
    private String roleName = "role1376";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session1 = TestScmTools.createSession( site );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session1 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws JSONException, ScmException, IOException,
            InterruptedException {

        //创建断点文件,大小为1024*512
        BreakpointUtil
                .createBreakpointFile( ws1, filePath, fileName, 1024 * 512,
                        ScmChecksumType.NONE );
        //创建新用户密码
        this.createScmUser();
        //使用新用户上传文件
        this.uploadBreakpointFile();

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.User.deleteUser( session1, newUsername );
            ScmFactory.Role.deleteRole( session1, roleName );
            ScmFactory.BreakpointFile.deleteInstance( ws1, fileName );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
        }
    }

    private void createScmUser() throws ScmException, InterruptedException {
        ScmUser user = ScmFactory.User
                .createUser( session1, newUsername, ScmUserPasswordType.LOCAL,
                        newPassword );
        ScmRole role = ScmFactory.Role.createRole( session1, roleName, null );
        ScmUserModifier modifier = new ScmUserModifier();
        ScmResource rs = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        ScmFactory.Role
                .grantPrivilege( session1, role, rs, ScmPrivilegeDefine.ALL );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session1, user, modifier );
        session2 = TestScmTools.createSession( site, newUsername, newPassword );
        ws2 = ScmFactory.Workspace.getWorkspace( wsp.getName(), session2 );
    }

    private void uploadBreakpointFile()
            throws ScmException, IOException, InterruptedException {
        boolean success = false;
        for ( int i = 0; i < 60; i++ ) {
            try {
                Thread.sleep( 1000 );
                InputStream inputStream = new FileInputStream( filePath );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws2, fileName );
                breakpointFile.upload( inputStream );
                inputStream.close();
                success = true;
                break;
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getErrorCode(),
                        ScmError.OPERATION_UNAUTHORIZED.getErrorCode(),
                        e.getMessage() );
            }
        }
        Assert.assertTrue( success, "getting priority spends over 60s!" );
    }
}
