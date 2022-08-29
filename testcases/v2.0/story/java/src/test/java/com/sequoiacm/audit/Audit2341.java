package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Description:SCM-2341 :: 指定userType和user的用户类型相同，审计类型相同
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2341 extends TestScmBase {
    private String fileName = "2341";
    private String username = "2341";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.createUser( wsp, username, ScmUserPasswordType.LOCAL,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws ScmException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.LOCAL.name(), "FILE_DML" );
        confMap.put( ConfigCommonDefind.scm_audit_user + username, "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        // check
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword );
        checkAudit( username, username );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole( username, username );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password )
            throws ScmException {
        ScmId fileId = null;
        try {
            fileId = createAndQueryFile( username, password );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session,
                            new BasicBSONObject()
                                    .append( ScmAttributeName.Audit.TYPE,
                                            "CREATE_FILE" )
                                    .append( ScmAttributeName.Audit.USERNAME,
                                            username ),
                            fileId.get() ),
                    true, "Has the configuration been updated?fileId = "
                            + fileId.get() );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session,
                            new BasicBSONObject()
                                    .append( ScmAttributeName.Audit.TYPE,
                                            "FILE_DQL" )
                                    .append( ScmAttributeName.Audit.USERNAME,
                                            username ),
                            fileId.get() ),
                    false, "Has the configuration been updated?fileId = "
                            + fileId.get() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }
    }

    // Audit2345 has same method
    private ScmId createAndQueryFile( String username, String password )
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

            ScmFactory.File.getInstance( ws, fileId );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return fileId;
    }
}
