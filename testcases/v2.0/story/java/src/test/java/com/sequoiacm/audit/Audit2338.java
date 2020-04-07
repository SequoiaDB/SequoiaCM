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
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
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
public class Audit2338 extends TestScmBase {
    private String batchName = "2338";
    private String name1 = "local2338";
    private String name2 = "token2338";
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

    //bug:442
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        Map< String, String > confMap = new HashMap< String, String >();
        //test local
        confMap.put( ConfigCommonDefind.scm_audit_user + name1, "BATCH_DML" );
        //test token
        confMap.put( ConfigCommonDefind.scm_audit_user + name2, "BATCH_DQL" );
        //test ldap
        confMap.put( ConfigCommonDefind.scm_audit_user + name3,
                "BATCH_DQL|BATCH_DML" );
        //update configuration
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );
        //Verify that audit logs are generated as configured
        checkAudit( name1, name1, true, false );
        checkAudit( name2, name2, false, true );
        checkAudit( TestScmBase.ldapUserName, TestScmBase.ldapPassword, true,
                true );
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

    //create and query batch to generate audit log  and check audit
    private void checkAudit( String username, String password,
            boolean isLogged1, boolean isLogged2 ) throws ScmException {
        ScmId batchId = null;
        try {
            batchId = createAndQueryBatch( username, password, batchName );

            BSONObject bson1 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_BATCH" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            BSONObject bson2 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "BATCH_DQL" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            Assert.assertEquals( ConfUtil.checkAudit( session, bson1
                    , batchId.get() ), isLogged1,
                    "Has the configuration been updated? batchId = " +
                            batchId.get() );
            Assert.assertEquals( ConfUtil.checkAudit( session, bson2
                    , batchId.get() ), isLogged2,
                    "Has not the configuration been updated? batchId = " +
                            batchId.get() );
        } finally {
            if ( batchId != null ) {
                ScmFactory.Batch.deleteInstance( ws, batchId );
            }
        }
    }

    private ScmId createAndQueryBatch( String username, String password,
            String batchName ) throws ScmException {
        ScmSession session = null;
        ScmId batchId = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );
            //create batch
            ScmBatch batch = ScmFactory.Batch.createInstance( ws );
            batch.setName( batchName );
            batchId = batch.save();
            //query batch
            ScmFactory.Batch.getInstance( ws, batchId );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return batchId;
    }
}
