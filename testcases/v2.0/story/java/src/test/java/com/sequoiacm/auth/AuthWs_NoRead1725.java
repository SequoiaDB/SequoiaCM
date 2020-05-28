package com.sequoiacm.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
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
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
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

/**
 * @author fanyu
 * @Description:SCM-1725:有工作区UPDATE|CREATE的权限和目录的CREATE|UPDATE| DELETE权限，
 *                                                              对表格中的各个接口进行覆盖测试
 * @Date:2018年6月6日
 * @version:1.0
 */
public class AuthWs_NoRead1725 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmSession sessionR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsR;
    private String dirpath = "/AuthWs_NoRead1725";
    private String username = "AuthWs_NoRead1725";
    private String rolename = "1725_NR";
    private String passwd = "1725";
    private ScmUser user;
    private ScmRole role;
    private ScmResource wsrs;
    private ScmResource dirrs;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getRootSite();
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
    private void testListBatch() {
        BSONObject bson = new BasicBSONObject();
        ScmCursor< ScmBatchInfo > cursor = null;
        try {
            cursor = ScmFactory.Batch.listInstance( wsR, bson );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testGetBatch() throws ScmException {
        String batchName = "AuthWs_Read1723";
        ScmId batchId = null;
        try {
            ScmBatch expBatch = ScmFactory.Batch.createInstance( wsA );
            expBatch.setName( batchName );
            batchId = expBatch.save();
            ScmFactory.Batch.getInstance( wsR, expBatch.getId() );
            Assert.fail( "the user does not have priority to do something" );
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
    private void testListBreakpointFile() {
        BreakpointUtil.checkDBDataSource();
        BSONObject cond = new BasicBSONObject();
        ScmCursor< ScmBreakpointFile > cursor = null;
        try {
            cursor = ScmFactory.BreakpointFile.listInstance( wsR, cond );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListBreakpointFileByWs() {
        BreakpointUtil.checkDBDataSource();
        ScmCursor< ScmBreakpointFile > cursor = null;
        try {
            cursor = ScmFactory.BreakpointFile.listInstance( wsR );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadBreakpointFileByName()
            throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile.createInstance( wsA,
                    fileName );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();

            ScmFactory.BreakpointFile.getInstance( wsR, fileName );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( expBreakpointFile != null ) {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadDir() throws ScmException {
        String path = "/AuthWs_Read1723" + "_" + UUID.randomUUID();
        ScmDirectory expDir = null;
        try {
            expDir = ScmFactory.Directory.createInstance( wsA, path );
            ScmFactory.Directory.getInstance( wsR, path );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( expDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadBreakpointFileBySize()
            throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile.createInstance( wsA,
                    fileName );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();

            ScmFactory.BreakpointFile.getInstance( wsR, fileName, 0 );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( expBreakpointFile != null ) {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadFileById() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFactory.File.getInstance( wsR, fileId );
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

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadFileByPath() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFactory.File.getInstanceByPath( wsR, "/" + fileName );
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

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadFileByVersion() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFactory.File.getInstance( wsR, fileId, 1, 0 );
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

    @Test(groups = { "twoSite", "fourSite" })
    private void testReadFileByPathVersion() throws Exception {
        String fileName = "AuthWs_Read1723" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( fileName );
            fileId = expfile.save();

            ScmFactory.File.getInstanceByPath( wsR, "/" + fileName, 1, 0 );
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

    @Test(groups = { "twoSite", "fourSite" })
    private void testGetSche() throws Exception {
        String scheName = "AuthWs_Read1723" + "_" + UUID.randomUUID();
        SiteWrapper rootSite = ScmInfo.getRootSite();
        SiteWrapper branchSite = ScmInfo.getBranchSite();
        String maxStayTime = "0d";
        BSONObject queryCond = null;
        ScmSchedule expSche = null;
        try {
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                    .is( scheName ).get();
            ScmScheduleContent content = new ScmScheduleCopyFileContent(
                    branchSite.getSiteName(), rootSite.getSiteName(),
                    maxStayTime, queryCond );
            String crond = "* * * * * ? 2029";
            expSche = ScmSystem.Schedule.create( sessionA, wsp.getName(),
                    ScheduleType.COPY_FILE, scheName, null, content, crond );

            ScmSystem.Schedule.get( sessionR, expSche.getId() );
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

    @Test(groups = { "twoSite", "fourSite" })
    private void testListSche() throws Exception {
        ScmCursor< ScmScheduleBasicInfo > cursor = null;
        try {
            cursor = ScmSystem.Schedule.list( sessionR, new BasicBSONObject() );
            // Assert.fail("the user does not have priority to do something");
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role, ScmPrivilegeType privileges ) {
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
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.UPDATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.DELETE );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
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
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );

            ScmFactory.Directory.createInstance( wsA, dirpath );

            wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            dirrs = ScmResourceFactory.createDirectoryResource( wsp.getName(),
                    dirpath );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.UPDATE );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.DELETE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.READ );

            ScmAuthUtils.checkPriority( site, username, passwd, role,
                    wsp.getName() );
            sessionR = TestScmTools.createSession( site, username, passwd );
            wsR = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionR );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
