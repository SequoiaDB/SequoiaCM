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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2347 :: 指定的userType与指定的userMask相同，审计类型有重叠
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2347 extends TestScmBase {
    private String fileName = "2347";
    private String dirName = "/2347A";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
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
        confMap.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        // check
        checkAudit( true, false, true );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( boolean isLogged1, boolean isLogged2,
            boolean isLogged3 ) throws ScmException, InterruptedException {
        ScmId fileId = null;
        ScmDirectory dir = null;
        try {
            fileId = createAndQueryFile();
            dir = createDir();
            BSONObject bson1 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_FILE" )
                    .append( ScmAttributeName.Audit.USERNAME,
                            TestScmBase.scmUserName );
            BSONObject bson2 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "FILE_DQL" )
                    .append( ScmAttributeName.Audit.USERNAME,
                            TestScmBase.scmUserName );
            BSONObject bson3 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_DIR" )
                    .append( ScmAttributeName.Audit.USERNAME,
                            TestScmBase.scmUserName );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bson1, fileId.get() ),
                    isLogged1, "Has the configuration been updated? fileId = "
                            + fileId.get() );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bson2, fileId.get() ),
                    isLogged2, "Has the configuration been updated? fileId = "
                            + fileId.get() );
            Assert.assertEquals( ConfUtil.checkAudit( session, bson3, dirName ),
                    isLogged3, "Has the configuration been updated? dirPath = "
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

    private ScmId createAndQueryFile() throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession( site );
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

    private ScmDirectory createDir() throws ScmException {
        ScmSession session = null;
        ScmDirectory dir = null;
        try {
            session = TestScmTools.createSession( site );
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
