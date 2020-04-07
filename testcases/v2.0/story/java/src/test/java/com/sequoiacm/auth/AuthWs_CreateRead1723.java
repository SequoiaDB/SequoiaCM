package com.sequoiacm.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmOutputStream;
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
 * @Description:SCM-1723:有单个工作区权限(Create)，对“当前版本权限列表.xlsx”中的各个接口进行覆盖测试,
 * @Date:2018年6月5日
 * @version:1.0
 */
public class AuthWs_CreateRead1723 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmSession sessionC;
    private ScmSession sessionCR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsC;
    private ScmWorkspace wsCR;
    private String[] usernameArr = { "AuthWs_Create1723_C",
            "AuthWs_Create1723_CR" };
    private String[] rolenameArr = { "Role_1723_C", "Role_1723_CR" };
    private String passwd = "1723";
    private List< ScmUser > userList = new ArrayList< ScmUser >();
    private List< ScmRole > roleList = new ArrayList< ScmRole >();
    private ScmResource rs;
    private String author = "AuthWs_Create1723";
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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateFile() throws ScmException {
        ScmId fileId = null;
        ScmFile expfile = null;
        try {
            expfile = ScmFactory.File.createInstance( wsCR );
            expfile.setFileName( author + "_" + UUID.randomUUID() );
            expfile.setAuthor( author );
            fileId = expfile.save();
            // scmIdList.

            // check
            ScmFile actfile = ScmFactory.File.getInstance( wsA, fileId );
            Assert.assertEquals( actfile.getAuthor(), expfile.getAuthor() );
            Assert.assertEquals( actfile.getSize(), expfile.getSize() );
            Assert.assertEquals( actfile.getUser(), expfile.getUser() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateFileByOutputStream()
            throws ScmException, IOException {
        ScmId fileId = null;
        ScmFile expfile = null;
        ScmOutputStream sos = null;
        try {
            expfile = ScmFactory.File.createInstance( wsCR );
            expfile.setFileName( author + "_" + UUID.randomUUID() );
            expfile.setAuthor( author );
            sos = ScmFactory.File.createOutputStream( expfile );
            byte[] buffer = TestTools.getBuffer( filePath );
            sos.write( buffer );
            sos.commit();
            fileId = expfile.getFileId();

            // check
            ScmFile actfile = ScmFactory.File.getInstance( wsA, fileId );
            Assert.assertEquals( actfile.getAuthor(), expfile.getAuthor() );
            Assert.assertEquals( actfile.getSize(), expfile.getSize() );
            Assert.assertEquals( actfile.getUser(), expfile.getUser() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateBatch() throws ScmException {
        ScmId batchId = null;
        ScmBatch expBatch = null;
        try {
            expBatch = ScmFactory.Batch.createInstance( wsC );
            expBatch.setName( author );
            batchId = expBatch.save();

            // check
            ScmBatch actBatch = ScmFactory.Batch.getInstance( wsA, batchId );
            Assert.assertEquals( actBatch.getCreateUser(),
                    expBatch.getCreateUser() );
            Assert.assertEquals( actBatch.getWorkspaceName(),
                    expBatch.getWorkspaceName() );
            Assert.assertEquals( actBatch.getName(), expBatch.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( batchId != null ) {
                ScmFactory.Batch.deleteInstance( wsA, batchId );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateBreakPointFileByName()
            throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        String fileName = author + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile
                    .createInstance( wsC, fileName );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();

            // check
            ScmBreakpointFile actBreakpointFile = ScmFactory.BreakpointFile
                    .getInstance( wsA, fileName );
            Assert.assertEquals( actBreakpointFile.getFileName(),
                    expBreakpointFile.getFileName() );
            Assert.assertEquals( actBreakpointFile.getSiteName(),
                    expBreakpointFile.getSiteName() );
            Assert.assertEquals( actBreakpointFile.getChecksumType(),
                    expBreakpointFile.getChecksumType() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expBreakpointFile != null ) {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateBreakPointFileByChecksumType()
            throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        String fileName = author + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile
                    .createInstance( wsC, fileName, ScmChecksumType.CRC32 );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ) );
            expBreakpointFile.incrementalUpload( inputStream, false );
            inputStream.close();
            // check
            ScmBreakpointFile actBreakpointFile = ScmFactory.BreakpointFile
                    .getInstance( wsA, fileName );
            Assert.assertEquals( actBreakpointFile.getFileName(),
                    expBreakpointFile.getFileName() );
            Assert.assertEquals( actBreakpointFile.getSiteName(),
                    expBreakpointFile.getSiteName() );
            Assert.assertEquals( actBreakpointFile.getChecksumType(),
                    expBreakpointFile.getChecksumType() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expBreakpointFile != null ) {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateBreakPointFileBySize()
            throws ScmException, IOException {
        BreakpointUtil.checkDBDataSource();
        String fileName = author + UUID.randomUUID();
        ScmBreakpointFile expBreakpointFile = null;
        try {
            expBreakpointFile = ScmFactory.BreakpointFile
                    .createInstance( wsC, fileName, ScmChecksumType.ADLER32,
                            0 );
            InputStream inputStream = new BreakpointInputStream(
                    new FileInputStream( new File( filePath ) ), 1 );
            expBreakpointFile.incrementalUpload( inputStream, true );
            inputStream.close();
            // check
            ScmBreakpointFile actBreakpointFile = ScmFactory.BreakpointFile
                    .getInstance( wsA, fileName );
            Assert.assertEquals( actBreakpointFile.getFileName(),
                    expBreakpointFile.getFileName() );
            Assert.assertEquals( actBreakpointFile.getSiteName(),
                    expBreakpointFile.getSiteName() );
            Assert.assertEquals( actBreakpointFile.getChecksumType(),
                    expBreakpointFile.getChecksumType() );
            Assert.assertEquals( actBreakpointFile.getUploadSize(),
                    expBreakpointFile.getUploadSize() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expBreakpointFile != null ) {
                ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCreateScmDir() throws ScmException {
        String path = "/" + author + "_" + UUID.randomUUID();
        ScmDirectory expDir = null;
        try {
            expDir = ScmFactory.Directory.createInstance( wsCR, path );

            // check
            ScmDirectory actDir = ScmFactory.Directory.getInstance( wsA, path );
            Assert.assertEquals( actDir.getName(), expDir.getName() );
            Assert.assertEquals( actDir.getPath(), expDir.getPath() );
            Assert.assertEquals( actDir.getUser(), expDir.getUser() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expDir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, path );
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
                    .create( sessionC, wsp.getName(), ScheduleType.COPY_FILE,
                            scheName, null,
                            content, crond );

            // check
            ScmSchedule actSche = ScmSystem.Schedule
                    .get( sessionA, expSche.getId() );
            Assert.assertEquals( expSche.getCreaateUser(),
                    actSche.getCreaateUser() );
            Assert.assertEquals( expSche.getCron(), actSche.getCron() );
            Assert.assertEquals( expSche.getName(), actSche.getName() );
            Assert.assertEquals( expSche.getDesc(), actSche.getDesc() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( expSche != null ) {
                ScmSystem.Schedule.delete( sessionA, expSche.getId() );
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
            ScmFactory.Role.revokePrivilege( sessionA, roleList.get( 0 ), rs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, roleList.get( 1 ), rs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, roleList.get( 1 ), rs,
                    ScmPrivilegeType.READ );
            // ScmFactory.Role.revokePrivilege(sessionA, roleList.get(0), rs,
            // ScmPrivilegeDefine.CREATE);
            // ScmFactory.Role.revokePrivilege(sessionA, roleList.get(1), rs,
            // ScmPrivilegeDefine.CREATE + "|" + ScmPrivilegeDefine.READ);
            ScmFactory.Role
                    .deleteRole( sessionA, roleList.get( 0 ).getRoleName() );
            ScmFactory.Role
                    .deleteRole( sessionA, roleList.get( 1 ).getRoleName() );
            ScmFactory.User.deleteUser( sessionA, userList.get( 0 ) );
            ScmFactory.User.deleteUser( sessionA, userList.get( 1 ) );
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
        for ( String rolename : rolenameArr ) {
            try {
                ScmFactory.Role.deleteRole( sessionA, rolename );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
        for ( String username : usernameArr ) {
            try {
                ScmFactory.User.deleteUser( sessionA, username );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private void prepare() throws Exception {
        try {
            ScmUser user = ScmFactory.User
                    .createUser( sessionA, usernameArr[ 0 ],
                            ScmUserPasswordType.LOCAL, passwd );
            userList.add( user );
            ScmRole role = ScmFactory.Role
                    .createRole( sessionA, rolenameArr[ 0 ], null );
            roleList.add( role );

            ScmUser user1 = ScmFactory.User
                    .createUser( sessionA, usernameArr[ 1 ],
                            ScmUserPasswordType.LOCAL, passwd );
            userList.add( user1 );
            ScmRole role1 = ScmFactory.Role
                    .createRole( sessionA, rolenameArr[ 1 ], null );
            roleList.add( role1 );

            rs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );

            // grantPriAndAttachRole(sessionA, rs, user, role,
            // ScmPrivilegeDefine.CREATE);
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, rs, user1, role1,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, rs, user1, role1,
                    ScmPrivilegeType.READ );

            ScmAuthUtils.checkPriority( site, usernameArr[ 0 ], passwd, role,
                    wsp.getName() );
            ScmAuthUtils.checkPriority( site, usernameArr[ 1 ], passwd, role1,
                    wsp.getName() );

            sessionC = TestScmTools
                    .createSession( site, usernameArr[ 0 ], passwd );
            wsC = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionC );

            sessionCR = TestScmTools
                    .createSession( site, usernameArr[ 1 ], passwd );
            wsCR = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), sessionCR );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
