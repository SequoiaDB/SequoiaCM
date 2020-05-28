package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2346 :: 指定的userType和user的用户类型不同，审计类型有重叠
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2346 extends TestScmBase {
    private String fileName = "2346";
    private String dirName = "/2346A";
    private String username = "2346";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( username, username );
        ConfUtil.createUser( wsp, username, ScmUserPasswordType.TOKEN,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put(
                ConfigCommonDefind.scm_audit_userType
                        + ScmUserPasswordType.LOCAL.name(),
                "FILE_DML|DIR_DML" );
        confMap.put( ConfigCommonDefind.scm_audit_user + username, "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        // check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword, true,
                true );
        checkAudit( username, username, true, false );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole( username, username );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password,
            boolean isLogged1, boolean isLogged2 )
            throws ScmException, InterruptedException {
        ScmId fileId = null;
        ScmDirectory dir = null;
        try {
            fileId = createFile( username, password );
            dir = createDir( username, password );
            BSONObject bson1 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_FILE" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            BSONObject bson2 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_DIR" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bson1, fileId.get() ),
                    isLogged1, "Has the configuration been updated? fileId = "
                            + fileId.get() );
            Assert.assertEquals( ConfUtil.checkAudit( session, bson2, dirName ),
                    isLogged2, "Has the configuration been updated? dirPath = "
                            + dirName );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            if ( dir != null ) {
                ScmFactory.Directory.deleteInstance( ws, dirName );
            }
        }
    }

    private ScmId createFile( String username, String password )
            throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            fileId = file.save();
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return fileId;
    }

    private ScmDirectory createDir( String username, String password )
            throws ScmException {
        ScmSession session = null;
        ScmDirectory dir = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            dir = ScmFactory.Directory.createInstance( ws, dirName );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return dir;
    }
}
