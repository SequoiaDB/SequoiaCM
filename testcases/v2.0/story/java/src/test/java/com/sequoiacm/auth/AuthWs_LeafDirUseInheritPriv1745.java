package com.sequoiacm.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
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
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @author fanyu
 * @Description: SCM-1745 :: 子目录继承父目录的权限，用继承的权限操作业务
 * @Date:2018年6月8日
 * @version:1.0
 */
public class AuthWs_LeafDirUseInheritPriv1745 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "AuthWs_LeafDirUseInheritPriv1745";
    private String rolename = "1745_0";
    private String[] privileges = { ScmPrivilegeDefine.CREATE,
            ScmPrivilegeDefine.READ, ScmPrivilegeDefine.UPDATE,
            ScmPrivilegeDefine.DELETE + "|" + ScmPrivilegeDefine.CREATE + "|"
                    + ScmPrivilegeDefine.READ + "|"
                    + ScmPrivilegeDefine.UPDATE };
    private String passwd = "1744";
    private ScmUser user = null;
    private ScmRole role = null;
    private WsWrapper wsp;
    private String path = "/1745_A/1745_B/1745_C/1745_D/1745_E/1745_F";
    private List< ScmResource > rsList = new ArrayList<>();
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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testLeafDir() {
        String testpath = path;
        ScmSession session = null;
        ScmWorkspace ws = null;
        String fileName = "1745_1";
        String newFileName = "1745_2";
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession( site, username, passwd );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            ScmDirectory dir = ScmFactory.Directory.getInstance( wsA,
                    testpath );

            // create file C+R
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setAuthor( fileName );
            file.setFileName( fileName );
            file.setDirectory( dir );
            fileId = file.save();

            // update file R+U
            ScmFile updateFile = ScmFactory.File.getInstanceByPath( ws,
                    path + "/" + fileName );
            updateFile.setFileName( newFileName );

            // read file R
            ScmFile readFile = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( readFile.getFileName(), newFileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileId != null ) {
                // delete File
                try {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                } catch ( ScmException e ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            try {
                for ( int i = 0; i < rsList.size(); i++ ) {
                    ScmFactory.Role.revokePrivilege( sessionA, role,
                            rsList.get( i ), privileges[ i ] );
                }
                ScmFactory.Role.deleteRole( sessionA, role );
                ScmFactory.User.deleteUser( sessionA, user );
                deleteDir( wsA, path );
                TestTools.LocalFile.removeFile( localPath );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role, String privileges )
            throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
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
        deleteDir( wsA, path );
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
        createDir( wsA, path );
        user = ScmFactory.User.createUser( sessionA, username,
                ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( sessionA, rolename, null );
        List< String > pathList = getSubPaths( path );
        for ( int i = 0; i < pathList.size() - 2; i++ ) {
            ScmResource rs = ScmResourceFactory.createDirectoryResource(
                    wsp.getName(), pathList.get( i ) );
            grantPriAndAttachRole( sessionA, rs, user, role, privileges[ i ] );
            rsList.add( rs );
        }
        ScmAuthUtils.checkPriority( site, username, passwd, role,
                wsp.getName() );
    }
}
