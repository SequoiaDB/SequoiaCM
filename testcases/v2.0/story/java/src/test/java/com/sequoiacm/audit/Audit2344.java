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
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-2344 :: 指定的userType和user的用户类型不同，审计类型相同
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2344 extends TestScmBase {
    private String wsName1 = "2344A";
    private String wsName2 = "2344B";
    private String name = "2344";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( name, name );
        ConfUtil.createUser( wsp, name, ScmUserPasswordType.TOKEN,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = ScmSessionUtils.createSession( site );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LOCAL.name(), "WS_DML|WS_DQL" );
        confMap.put( ConfigCommonDefind.scm_audit_user + name,
                "WS_DQL|WS_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        // check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword, wsName1,
                true, true );
        checkAudit( name, name, wsName2, true, true );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole( name, name );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password, String wsName,
            boolean isLogged1, boolean isLogged2 )
            throws ScmException, InterruptedException {
        ScmWorkspace ws1 = null;
        try {
            ws1 = craeteAndQueryWs( username, password, wsName );
            BSONObject bson1 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_WS" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            BSONObject bson2 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "WS_DQL" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            Assert.assertEquals( ConfUtil.checkAudit( session, bson1, wsName ),
                    isLogged1,
                    "Has the configuration been updated? wsName = " + wsName );
            Assert.assertEquals( ConfUtil.checkAudit( session, bson2, wsName ),
                    isLogged2,
                    "Has the configuration been updated? wsName = " + wsName );
        } finally {
            if ( ws1 != null ) {
                ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
            }
        }
    }

    private ScmWorkspace craeteAndQueryWs( String username, String password,
            String wsName ) throws ScmException, InterruptedException {
        ScmSession session = null;
        ScmWorkspace ws;
        try {
            session = ScmSessionUtils.createSession( site, username, password );
            // create
            ws = ScmWorkspaceUtil.createWS( session, wsName,
                    ScmInfo.getSiteNum() );
            // query
            ScmFactory.Workspace.getWorkspace( wsName, session );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return ws;
    }
}
