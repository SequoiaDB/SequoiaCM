package com.sequoiacm.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BreakpointInputStream;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @author fanyu
 * @Description:SCM-1725:有工作区UPDATE|READ的权限和目录的CREATE|DELETE权限，对表格中的各个接口进行覆盖测试
 * @Date:2018年6月5日
 * @version:1.0
 */
public class AuthWs_NoCreateRead1725 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmSession sessionCR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsCR;
    private String dirpath = "/AuthWs_NoCreateRead1725";
    private String username = "AuthWs_NoCreateRead1725";
    private String rolename = "Role_1725_NCR";
    private ScmRole role;
    private String passwd = "1725";
    private ScmResource dirrs;
    private ScmResource wsrs;
    private String author = "AuthWs_NoCreateRead1725";
    private WsWrapper wsp;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator +
                    TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize +
                    ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( site );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateFile() throws ScmException {
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsCR );
            expfile.setFileName( author + "_" + UUID.randomUUID() );
            expfile.setAuthor( author );
            fileId = expfile.save();
            Assert.fail( "the user have not privilege to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateBatch() throws ScmException {
        ScmId batchId = null;
        ScmBatch expBatch = null;
        try {
            expBatch = ScmFactory.Batch.createInstance( wsCR );
            expBatch.setName( author );
            batchId = expBatch.save();
            Assert.fail( "the user have not privilege to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( batchId != null ) {
                ScmFactory.Batch.deleteInstance( wsA, batchId );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateBreakPointFileByName()
            throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        String fileName = author + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile
                    .createInstance( wsCR, fileName );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();
            Assert.fail( "the user have not privilege to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateBreakPointFileByChecksumType()
            throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        String fileName = author + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile
                    .createInstance( wsCR, fileName, ScmChecksumType.CRC32 );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();
            expBreakpointFile = null;
            Assert.fail( "the user have not privilege to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateBreakPointFileBySize()
            throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        String fileName = author + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile
                    .createInstance( wsCR, fileName, ScmChecksumType.ADLER32,
                            0 );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ), 1 );
            expBreakpointFile.incrementalUpload( inputStream, true );
            inputStream.close();
            Assert.fail( "the user have not privilege to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateScmDir() throws ScmException {
        String path = "/" + author + "_" + UUID.randomUUID();
        try {
            ScmFactory.Directory.createInstance( wsCR, path );
            Assert.fail( "the user have not privilege to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCreateSche() throws ScmException {
        String scheName = author + "_" + UUID.randomUUID();
        SiteWrapper rootSite = ScmInfo.getRootSite();
        SiteWrapper branchSite = ScmInfo.getBranchSite();
        String maxStayTime = "0d";
        BSONObject queryCond = null;
        try {
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( author ).get();
            ScmScheduleContent content = new ScmScheduleCopyFileContent(
                    rootSite.getSiteName(),
                    branchSite.getSiteName(), maxStayTime, queryCond );
            String crond = "* * * * * 2029";
            ScmSystem.Schedule
                    .create( sessionCR, wsp.getName(), ScheduleType.COPY_FILE,
                            scheName, null, content, crond );
            Assert.fail( "the user have not privilege to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role,
            ScmPrivilegeType privileges ) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
            modifier.addRole( role );
            ScmFactory.User.alterUser( sessionA, user, modifier );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.UPDATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.DELETE );
            ScmFactory.Role.deleteRole( sessionA, rolename );
            ScmFactory.User.deleteUser( sessionA, username );
            ScmFactory.Directory.deleteInstance( wsA, dirpath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void cleanEnv() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        try {
            ScmFactory.Role.deleteRole( sessionA, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( sessionA, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() throws Exception {
        try {
            ScmUser user = ScmFactory.User
                    .createUser( sessionA, username, ScmUserPasswordType.LOCAL,
                            passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );

            ScmFactory.Directory.createInstance( wsA, dirpath );
            wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            dirrs = ScmResourceFactory
                    .createDirectoryResource( wsp.getName(), dirpath );

            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.UPDATE );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.DELETE );

            ScmAuthUtils.checkPriority( site, username, passwd, role,
                    wsp.getName() );

            sessionCR = TestScmTools.createSession( site, username, passwd );
            wsCR = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), sessionCR );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
