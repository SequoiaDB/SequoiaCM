package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2342 :: 指定userType和user的用户类型相同，审计类型不同
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2342 extends TestScmBase {
    private String attrName = "2342";
    private String name = "2342";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( name, name );
        ConfUtil.createUser( wsp, name, ScmUserPasswordType.LOCAL,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put( ConfigCommonDefind.scm_audit_userType +
                ScmUserPasswordType.LOCAL.name(), "META_ATTR_DQL" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        Map< String, String > confMap1 = new HashMap< String, String >();
        confMap1.put( ConfigCommonDefind.scm_audit_user + name,
                "META_ATTR_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap1 );

        //check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword );
        checkAudit( name, name );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole( name, name );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password )
            throws ScmException {
        ScmId attrId = null;
        try {
            attrId = craeteAndQueryAttr( username, password );
            BSONObject bson1 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_META_ATTR" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            BSONObject bson2 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "META_ATTR_DQL" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            if ( username.equals( TestScmBase.scmUserName ) ) {
                Assert.assertEquals(
                        ConfUtil.checkAudit( session, bson1, attrName ), false,
                        "Has the configuration been updated? attrId = " +
                                attrId.get() );
                Assert.assertEquals(
                        ConfUtil.checkAudit( session, bson2, attrId.get() ),
                        true, "Has the configuration been updated? attrId = " +
                                attrId.get() );
            } else {
                Assert.assertEquals(
                        ConfUtil.checkAudit( session, bson1, attrName ), true,
                        "Has the configuration been updated? ,attrId = " +
                                attrId.get() );
                Assert.assertEquals(
                        ConfUtil.checkAudit( session, bson2, attrId.get() ),
                        false,
                        "Has the configuration been updated? ,attrId = " +
                                attrId.get() );
            }
        } finally {
            if ( attrId != null ) {
                ScmFactory.Attribute.deleteInstance( ws, attrId );
            }
        }
    }

    private ScmId craeteAndQueryAttr( String username, String password )
            throws ScmException {
        ScmSession session = null;
        ScmId attrId = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );
            //create
            ScmAttribute attr = ScmFactory.Attribute
                    .createInstance( ws, new ScmAttributeConf()
                            .setName( attrName )
                            .setType( AttributeType.STRING ) );
            attrId = attr.getId();
            //query
            ScmFactory.Attribute.getInstance( ws, attr.getId() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return attrId;
    }
}
