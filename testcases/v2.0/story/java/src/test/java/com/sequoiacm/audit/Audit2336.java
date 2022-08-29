package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Description:SCM-2336 :: 指定多个不重复的userType，审计类型有重叠
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2336 extends TestScmBase {
    private String fileName = "2336";
    private String name = "token2336";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( name, name );
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        ConfUtil.createUser( wsp, name, ScmUserPasswordType.TOKEN,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws ScmException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put(
                ConfigCommonDefind.scm_audit_userType
                        + ScmUserPasswordType.LOCAL.name(),
                "FILE_DML|DIR_DML" );
        confMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.TOKEN.name(), "FILE_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        // the type of TestScmBase.scmUserName is local
        checkAudit( TestScmBase.scmUserName, TestScmBase.scmPassword, true,
                true );
        checkAudit( name, name, true, false );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        ConfUtil.deleteUserAndRole( name, name );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password,
            boolean isLogged1, boolean isLogged2 ) throws ScmException {
        ScmId fileId = null;
        String dirPath = "/2336";
        ScmDirectory dir = null;
        try {
            fileId = createFile( fileName + "_" + UUID.randomUUID(), username,
                    password );
            dir = ScmFactory.Directory.createInstance( ws, dirPath );

            BSONObject bson1 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_FILE" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            BSONObject bson2 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_DIR" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            // check audit is logged by new configuration
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bson1, fileId.get() ),
                    isLogged1, "Has the configuration been updated? fileId = "
                            + fileId.get() );
            Assert.assertEquals( ConfUtil.checkAudit( session, bson2, dirPath ),
                    isLogged2, "Has the configuration been updated? dirId = "
                            + dir.getId() );
        } finally {
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            if ( dir != null ) {
                ScmFactory.Directory.deleteInstance( ws, dirPath );
            }
        }
    }

    private ScmId createFile( String fileName, String username,
            String password ) throws ScmException {
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
