package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.client.core.*;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
 * @descreption SCM-2342 :: 指定userType和user的用户类型相同，审计类型不同
 * @author  fanyu
 * @date 2018年12月25日
 * @updateUser Yipan
 * @updateDate 2022/4/12
 * @updateRemark
 * @version 1.0
 */
public class Audit2342 extends TestScmBase {
    private String dirName = "/dir2342";
    private String fileName = "file2342";
    private String user = "user2342";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( user, user );
        ConfUtil.createUser( wsp, user, ScmUserPasswordType.LOCAL,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        Map< String, String > dirConf = new HashMap<>();
        dirConf.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LOCAL.name(), "DIR_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), dirConf );
        Map< String, String > fileConf = new HashMap<>();
        fileConf.put( ConfigCommonDefind.scm_audit_user + user, "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), fileConf );

        // check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword );
        checkAudit( user, user );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole( user, user );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password )
            throws ScmException {
        createFileAndDir( username, password );
        BSONObject file_bson = new BasicBSONObject()
                .append( ScmAttributeName.Audit.TYPE, "CREATE_FILE" )
                .append( ScmAttributeName.Audit.USERNAME, username );
        BSONObject dir_bson = new BasicBSONObject()
                .append( ScmAttributeName.Audit.TYPE, "CREATE_DIR" )
                .append( ScmAttributeName.Audit.USERNAME, username );
        if ( username.equals( TestScmBase.scmUserName ) ) {
            Assert.assertFalse(
                    ConfUtil.checkAudit( session, file_bson, fileName ) );
            Assert.assertTrue(
                    ConfUtil.checkAudit( session, dir_bson, dirName ) );
        } else {
            Assert.assertTrue(
                    ConfUtil.checkAudit( session, file_bson, fileName ) );
            Assert.assertFalse(
                    ConfUtil.checkAudit( session, dir_bson, dirName ) );
        }
    }

    private void createFileAndDir( String username, String password )
            throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFactory.Directory.createInstance( ws, dirName );
            ScmFactory.Directory.deleteInstance( ws, dirName );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            ScmId fileId = file.save();
            ScmFactory.File.deleteInstance( ws, fileId, true );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
