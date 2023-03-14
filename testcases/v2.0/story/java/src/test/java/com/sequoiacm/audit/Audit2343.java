package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @descreption SCM-2343:指定userType和user的用户类型相同，审计类型有空字符串
 * @author fanyu
 * @date 2022/4/12
 * @updateUser YiPan
 * @updateDate 2022/4/12
 * @updateRemark
 * @version 1.0
 */
public class Audit2343 extends TestScmBase {
    private String fileNameBase = "file2343_";
    private String dirNameBase = "/2343_";
    private String name = "2343";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( name, name );
        ConfUtil.createUser( wsp, name, ScmUserPasswordType.LOCAL,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = ScmSessionUtils.createSession( site );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        test1();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );

        test2();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );

        test3();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole( name, name );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    // scm.audit.userType.LOCAL="" scm.audit.user.test=META_ATTR_DML
    private void test1() throws ScmException {
        Map< String, String > scmUserConf = new HashMap< String, String >();
        scmUserConf.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LOCAL.name(), "" );
        ConfUtil.updateConf( site.getSiteServiceName(), scmUserConf );

        Map< String, String > newUserConf = new HashMap< String, String >();
        newUserConf.put( ConfigCommonDefind.scm_audit_user + name, "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), newUserConf );

        // check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword, false,
                false );
        checkAudit( name, name, true, false );
    }

    // scm.audit.userType.LOCAL=META_ATTR_DML scm.audit.user.test=""
    private void test2() throws ScmException {
        Map< String, String > scmUserConf = new HashMap< String, String >();
        scmUserConf.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LOCAL.name(), "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), scmUserConf );

        Map< String, String > newUserConf = new HashMap< String, String >();
        newUserConf.put( ConfigCommonDefind.scm_audit_user + name, "" );
        ConfUtil.updateConf( site.getSiteServiceName(), newUserConf );

        // check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword, true,
                false );
        checkAudit( name, name, false, false );
    }

    // scm.audit.userType.LOCAL="" scm.audit.user.test=""
    private void test3() throws ScmException {
        Map< String, String > scmUserConf = new HashMap< String, String >();
        scmUserConf.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LOCAL.name(), "" );
        ConfUtil.updateConf( site.getSiteServiceName(), scmUserConf );

        Map< String, String > newUserConf = new HashMap< String, String >();
        newUserConf.put( ConfigCommonDefind.scm_audit_user + name, "" );
        ConfUtil.updateConf( site.getSiteServiceName(), newUserConf );

        // check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword, false,
                false );
        checkAudit( name, name, false, false );
    }

    private void checkAudit( String username, String password,
            boolean isLoggedFile, boolean isLoggedDir ) throws ScmException {
        String dirName = dirNameBase + UUID.randomUUID();
        String fileName = fileNameBase + UUID.randomUUID();
        createFileAndDir( username, password, dirName, fileName );
        BSONObject file_bson = new BasicBSONObject()
                .append( ScmAttributeName.Audit.TYPE, "CREATE_FILE" )
                .append( ScmAttributeName.Audit.USERNAME, username );
        BSONObject dir_bson = new BasicBSONObject()
                .append( ScmAttributeName.Audit.TYPE, "CREATE_DIR" )
                .append( ScmAttributeName.Audit.USERNAME, username );
        Assert.assertEquals(
                ConfUtil.checkAudit( session, file_bson, fileName ),
                isLoggedFile );
        Assert.assertEquals( ConfUtil.checkAudit( session, dir_bson, dirName ),
                isLoggedDir );
    }

    private void createFileAndDir( String username, String password,
            String dirName, String fileName ) throws ScmException {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site, username, password );
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
