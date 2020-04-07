package com.sequoiacm.auth.serial;

import java.io.File;
import java.io.IOException;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1731 :: 无工作区和目录的权限，对表格中无权限要求各个接口进行覆盖测试
 * @author fanyu
 * @Date:2018年6月6日
 * @version:1.0
 */
public class AuthWsDir_None1731 extends TestScmBase {
    private SiteWrapper rootsite;
    private SiteWrapper branchsite;
    private WsWrapper wsp;
    private ScmSession sessionM;
    private ScmSession sessionB;
    private ScmSession sessionUM;
    private ScmSession sessionUB;
    private ScmWorkspace wsM;
    private ScmWorkspace wsUM;
    private String username = "AuthWs_None1723";
    private String passwd = "1723";
    private ScmUser user;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws InterruptedException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator +
                    TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize +
                    ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootsite = ScmInfo.getRootSite();
            branchsite = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            sessionM = TestScmTools.createSession( rootsite );
            sessionB = TestScmTools.createSession( branchsite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
            try {
                ScmFactory.User.deleteUser( sessionM, username );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
        }
        try {
            user = ScmFactory.User
                    .createUser( sessionM, username, ScmUserPasswordType.LOCAL,
                            passwd );
            sessionUM = TestScmTools
                    .createSession( rootsite, username, passwd );
            sessionUB = TestScmTools
                    .createSession( branchsite, username, passwd );
            wsUM = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), sessionUM );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListDir() {
        ScmCursor< ScmDirectory > cursor = null;
        try {
            cursor = ScmFactory.Directory
                    .listInstance( wsUM, new BasicBSONObject() );
            Assert.assertNotNull( cursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListSche() {
        ScmCursor< ScmScheduleBasicInfo > cursor = null;
        try {
            cursor = ScmSystem.Schedule
                    .list( sessionUB, new BasicBSONObject() );
            Assert.assertNotNull( cursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListFile() {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            cursor = ScmFactory.File.listInstance( wsM, ScopeType.SCOPE_ALL,
                    new BasicBSONObject() );
            Assert.assertNotNull( cursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListTask() {
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        try {
            cursor = ScmSystem.Task
                    .listTask( sessionUB, new BasicBSONObject() );
            Assert.assertNotNull( cursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListSite() {
        ScmCursor< ScmSiteInfo > cursor = null;
        try {
            cursor = ScmFactory.Site.listSite( sessionUB );
            Assert.assertNotNull( cursor );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.User.deleteUser( sessionM, user );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }
}
