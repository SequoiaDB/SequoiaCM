package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;

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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2340 ::指定重复的username，审计类型有重叠
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2340 extends TestScmBase {
    private String dirName = "/2340";
    private String fileName = "2340";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put(
                ConfigCommonDefind.scm_audit_user + TestScmBase.scmUserName,
                "ALL" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        Map< String, String > confMap1 = new HashMap< String, String >();
        confMap1.put(
                ConfigCommonDefind.scm_audit_user + TestScmBase.scmUserName,
                "DIR_DML" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap1 );
        checkAudit();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit() throws ScmException {
        String dirId = null;
        ScmId fileId = null;
        try {
            dirId = createAndQueryDir();
            fileId = createFile();
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, new BasicBSONObject()
                            .append( ScmAttributeName.Audit.TYPE, "CREATE_DIR" )
                            .append( ScmAttributeName.Audit.USERNAME,
                                    TestScmBase.scmUserName ),
                            dirName ),
                    true,
                    "Has the configuration been updated?dirName = " + dirName );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session,
                            new BasicBSONObject()
                                    .append( ScmAttributeName.Audit.TYPE,
                                            "CREATE_FILE" )
                                    .append( ScmAttributeName.Audit.USERNAME,
                                            TestScmBase.scmUserName ),
                            fileId.get() ),
                    false, "Has the configuration been updated?fileId = "
                            + fileId.get() );
        } finally {
            if ( dirId != null ) {
                ScmFactory.Directory.deleteInstance( ws, dirName );
            }
            if ( fileId != null ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        }
    }

    private String createAndQueryDir() throws ScmException {
        ScmSession session = null;
        String dirId = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            // create dir
            ScmDirectory dir = ScmFactory.Directory.createInstance( ws,
                    dirName );
            dirId = dir.getId();
            // query dir
            ScmFactory.Directory.getInstance( ws, dirName );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return dirId;
    }

    private ScmId createFile() throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = ScmSessionUtils.createSession( site );
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
