package com.sequoiacm.auth;

import java.io.File;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmDirectory;
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
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
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
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description:SCM-1725:有工作区CREATE|UPDATE|READ的权限和目录的DELETE权限，对表格中的各个接口进行覆盖测试
 * @author fanyu
 * @Date:2018年6月6日
 * @version:1.0
 */
public class AuhtWs_NoDelete1725 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmSession sessionND;
    private ScmWorkspace wsA;
    private ScmWorkspace wsND;
    private String username = "AuhtWs_NoDelete1725";
    private String rolename = "1725_ND";
    private String passwd = "1725";
    private String author = "AuhtWs_NoDelete1725";
    private String path = "/AuhtWs_NoDelete1725";
    private ScmUser user;
    private ScmRole role;
    private ScmResource dirrs;
    private ScmResource wsrs;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession( site );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        cleanEnv();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testDeleteBatch() throws ScmException {
        String batchName = author;
        ScmId batchId = null;
        try {
            ScmBatch expBatch = ScmFactory.Batch.createInstance( wsA );
            expBatch.setName( batchName );
            batchId = expBatch.save();
            ScmFactory.Batch.deleteInstance( wsND, batchId );
            Assert.fail( "the user has not privilege to do something" );
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
    private void testDeleteFile() throws ScmException {
        String fileName = "AuthWs_Update1723_file" + "_" + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            fileId = expfile.save();
            ScmFactory.File.deleteInstance( wsND, fileId, true );
            Assert.fail( "the user does not have priority to do something" );
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

//    @Test(groups = {"twoSite", "fourSite"})
//    private void testDeleteBreakPointFile() throws ScmException, IOException {
//        BreakpointUtil.checkDBDataSource();
//        String fileName = author + UUID.randomUUID();
//        ScmBreakpointFile expBreakpointFile = null;
//        try {
//            expBreakpointFile = ScmFactory.BreakpointFile.createInstance
// (wsA, fileName);
//            InputStream inputStream = new BreakpointInputStream(new
// FileInputStream(new File(filePath)));
//            expBreakpointFile.incrementalUpload(inputStream, false);
//            inputStream.close();
//
//            ScmFactory.BreakpointFile.deleteInstance(wsND, fileName);
//            Assert.fail("the user does not have priority to do something");
//        } catch (ScmException e) {
//            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
//                e.printStackTrace();
//                Assert.fail(e.getMessage());
//            }
//        } finally {
//            if (expBreakpointFile != null) {
//                ScmFactory.BreakpointFile.deleteInstance(wsA, fileName);
//            }
//        }
//    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testDeleteScmDir() throws ScmException {
        String path = "/" + author + "_" + UUID.randomUUID();
        ScmDirectory expDir = null;
        try {
            expDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmFactory.Directory.deleteInstance( wsND, path );
            expDir = null;
            Assert.fail(
                    "the user does not have priority to do something,path = " +
                            path );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            System.out.println( "expDir = " + expDir );
            if ( expDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, expDir.getPath() );
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
        ScmSchedule expSche = null;
        try {
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( author ).get();
            ScmScheduleContent content = new ScmScheduleCopyFileContent(
                    branchSite.getSiteName(),
                    rootSite.getSiteName(), maxStayTime, queryCond );
            String crond = "* * * * * ? 2029";
            expSche = ScmSystem.Schedule
                    .create( sessionA, wsp.getName(), ScheduleType.COPY_FILE,
                            scheName, null,
                            content, crond );
            ScmSystem.Schedule.delete( sessionND, expSche.getId() );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( expSche != null ) {
                ScmSystem.Schedule.delete( sessionA, expSche.getId() );
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role,
            ScmPrivilegeType privileges ) throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.UPDATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.DELETE );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            ScmFactory.Directory.deleteInstance( wsA, path );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void cleanEnv() {
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
            user = ScmFactory.User
                    .createUser( sessionA, username, ScmUserPasswordType.LOCAL,
                            passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
            ScmDirectory dir = ScmFactory.Directory.createInstance( wsA, path );
            wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            dirrs = ScmResourceFactory
                    .createDirectoryResource( wsp.getName(), path );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.UPDATE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.DELETE );
            ScmAuthUtils.checkPriority( site, username, passwd, role,
                    wsp.getName() );
            sessionND = TestScmTools.createSession( site, username, passwd );
            wsND = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), sessionND );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
