package com.sequoiacm.auth.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @author fanyu
 * @Description:SCM-1730 ::有工作区的CREATE/READ权限和目录的UPDATE/DELETE权限，对表格中的各个接口进行覆盖测试
 * @Date:2018年6月12日
 * @version:1.0
 */
public class AuthWsDir_UpdateRead1730 extends TestScmBase {
    private SiteWrapper branchsite;
    private SiteWrapper rootsite;
    private ScmSession sessionA;
    private ScmSession sessionUR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsUR;
    private String username = "AuthWsDir_UpdateRead1730";
    private String rolename = "Role_1730_UR";
    private String passwd = "1730";
    private ScmUser user;
    private ScmRole role;
    private ScmResource wsrs;
    private ScmResource dirrs;
    private String basepath = "/AuthWsDir_UpdateRead1730";
    private String path = basepath + "/1730_A";
    private String path1 = basepath + "/1730_B";
    private String author = "AuthWsDir_UpdateRead1730";
    private WsWrapper wsp;
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

            branchsite = ScmInfo.getBranchSite();
            rootsite = ScmInfo.getRootSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( rootsite );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testUpdateFile() throws ScmException {
        String fileName = author + "_" + UUID.randomUUID();
        String newfileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            // getdir
            dir = ScmFactory.Directory.getInstance( wsA, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            fileId = expfile.save();

            ScmFile actfile = ScmFactory.File.getInstance( wsUR, fileId );
            actfile.setFileName( newfileName );

            ScmFile expFile1 = ScmFactory.File.getInstance( wsUR, fileId );
            Assert.assertEquals( actfile.getFileName(),
                    expFile1.getFileName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testMoveFile() throws ScmException {
        String dirpath = path;
        String newpath = path1;
        String fileName = author + UUID.randomUUID();
        ScmId fileId = null;
        try {
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsA, dirpath );
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setAuthor( author );
            expfile.setDirectory( dir );
            fileId = expfile.save();

            // move file
            ScmFile file = ScmFactory.File.getInstanceByPath( wsUR,
                    dirpath + "/" + fileName );
            ScmDirectory newDir = ScmFactory.Directory.getInstance( wsUR,
                    newpath );
            file.setDirectory( newDir );

            // check
            ScmFile actfile = ScmFactory.File.getInstanceByPath( wsUR,
                    newpath + "/" + fileName );
            Assert.assertEquals( actfile.getFileName(), expfile.getFileName() );
            Assert.assertEquals( actfile.getDirectory().getPath(),
                    newpath + "/" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynCaheFile() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            // get dir
            dir = ScmFactory.Directory.getInstance( wsA, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( wsA );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            fileId = expfile.save();

            ScmFactory.File.asyncCache( wsUR, fileId );

            // check
            SiteWrapper[] expSiteList = { rootsite, branchsite };
            ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynCaheFileByVersion() throws Exception {
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            // get dir
            dir = ScmFactory.Directory.getInstance( wsA, dirPath );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionA );
            ScmFile expfile = ScmFactory.File.createInstance( ws );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            expfile.setContent( filePath );
            fileId = expfile.save();

            ScmFactory.File.asyncCache( wsUR, fileId, 1, 0 );

            // check
            SiteWrapper[] expSiteList = { rootsite, branchsite };
            ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynTransfer() throws Exception {
        ScmSession session = null;
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            session = TestScmTools.createSession( branchsite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            // get dir
            dir = ScmFactory.Directory.getInstance( ws, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( ws );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            expfile.setContent( filePath );
            fileId = expfile.save();

            ScmFactory.File.asyncTransfer( wsUR, fileId,
                    rootsite.getSiteName() );

            // check
            SiteWrapper[] expSiteList = { rootsite, branchsite };
            ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynTransferByVersion() throws Exception {
        ScmSession session = null;
        String fileName = author + "_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmDirectory dir = null;
        String dirPath = path;
        try {
            session = TestScmTools.createSession( branchsite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            // get dir
            dir = ScmFactory.Directory.getInstance( ws, dirPath );
            ScmFile expfile = ScmFactory.File.createInstance( ws );
            expfile.setFileName( fileName );
            expfile.setDirectory( dir );
            expfile.setContent( filePath );
            fileId = expfile.save();

            ScmFactory.File.asyncTransfer( wsUR, fileId, 1, 0,
                    rootsite.getSiteName() );

            // check
            SiteWrapper[] expSiteList = { rootsite, branchsite };
            ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId,
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSiteList, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.CREATE );

            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.DELETE );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.UPDATE );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            deleteDir( wsA, path );
            deleteDir( wsA, path1 );
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

    private ScmDirectory createDir( ScmWorkspace ws, String dirPath )
            throws ScmException {
        List< String > pathList = getSubPaths( dirPath );
        for ( String path : pathList ) {
            try {
                ScmFactory.Directory.createInstance( ws, path );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_EXIST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
        return ScmFactory.Directory.getInstance( ws,
                pathList.get( pathList.size() - 1 ) );
    }

    private void deleteDir( ScmWorkspace ws, String dirPath ) {
        List< String > pathList = getSubPaths( dirPath );
        for ( int i = pathList.size() - 1; i >= 0; i-- ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, pathList.get( i ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DIR_NOT_FOUND
                        && e.getError() != ScmError.DIR_NOT_EMPTY ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private List< String > getSubPaths( String path ) {
        String ele = "/";
        String[] arry = path.split( "/" );
        List< String > pathList = new ArrayList<>();
        for ( int i = 1; i < arry.length; i++ ) {
            ele = ele + arry[ i ];
            pathList.add( ele );
            ele = ele + "/";
        }
        return pathList;
    }

    private void cleanEnv() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        deleteDir( wsA, path );
        deleteDir( wsA, path1 );
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

            createDir( wsA, path );
            createDir( wsA, path1 );

            wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            dirrs = ScmResourceFactory.createDirectoryResource( wsp.getName(),
                    path );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.DELETE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.UPDATE );

            ScmAuthUtils.checkPriority( rootsite, username, passwd, role, wsp );
            sessionUR = TestScmTools.createSession( branchsite, username,
                    passwd );
            wsUR = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionUR );

        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
