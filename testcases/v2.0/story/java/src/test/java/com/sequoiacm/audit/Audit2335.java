package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
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
 * @Description:SCM-2335:指定单个userType， 审计类型任意
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2335 extends TestScmBase {
    private String fileName = "2335";
    private String name1 = "local2335";
    private String name2 = "token2335";
    private String name3 = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        name3 = TestScmBase.ldapUserName;
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( name1, name1 );
        ConfUtil.deleteUserAndRole( name2, name2 );
        ConfUtil.deleteUserAndRole( name3, name3 );
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        ConfUtil.createUser( wsp, name1, ScmUserPasswordType.LOCAL,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        ConfUtil.createUser( wsp, name2, ScmUserPasswordType.TOKEN,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        ConfUtil.createUser( wsp, name3, ScmUserPasswordType.LDAP,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
    }

    // bug:442
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        // test local
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LOCAL.name(), "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );
        // Verify that audit logs are generated as configured
        checkAudit( name1, name1 );
        // test token
        Map< String, String > confMap1 = new HashMap< String, String >();
        confMap1.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.TOKEN.name(), "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap1 );
        // Verify that audit logs are generated as configured
        checkAudit( name2, name2 );

        // test ldap
        Map< String, String > confMap2 = new HashMap< String, String >();
        confMap2.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LDAP.name(), "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap2 );
        // Verify that audit logs are generated as configured
        checkAudit( TestScmBase.ldapUserName, TestScmBase.ldapPassword );

        // test All
        Map< String, String > confMap3 = new HashMap< String, String >();
        confMap3.put( ConfigCommonDefind.scm_audit_userType + "ALL",
                "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap3 );
        // Verify that audit logs are generated as configured
        checkAudit( name1, name1 );
        checkAudit( name2, name2 );
        checkAudit( TestScmBase.ldapUserName, TestScmBase.ldapPassword );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole( name1, name1 );
        ConfUtil.deleteUserAndRole( name2, name2 );
        ConfUtil.deleteUserAndRole( name3, name3 );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    // create file to generate audit log and check audit
    private void checkAudit( String username, String password )
            throws ScmException {
        ScmId fileId = null;
        try {
            fileId = createFile( username, password,
                    fileName + "_" + UUID.randomUUID() );
            Assert.assertEquals( ConfUtil.checkAudit( session,
                    new BasicBSONObject().append( ScmAttributeName.Audit.TYPE,
                            "CREATE_FILE" ),
                    fileId.get() ), true,
                    "Has the configuration been updated? fileId = "
                            + fileId.get() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }
    }

    private ScmId createFile( String username, String password,
            String fileName ) throws ScmException {
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
}
