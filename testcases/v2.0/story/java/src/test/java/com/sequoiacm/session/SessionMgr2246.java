package com.sequoiacm.session;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionMgr;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-2246 :: 关闭ScmSessionMgr，获取session,再次关闭ScmSessionMgr
 * @author fanyu
 * @Date:2018年9月21日
 * @version:1.0
 */
public class SessionMgr2246 extends TestScmBase {
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1;
    private String name = "SessionMgr2245";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private ScmSessionMgr sessionMgr = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
            ScmFileUtils.cleanFile( wsp, cond );

            sessionMgr = createSessionMgr();
            session = sessionMgr.getSession( SessionType.AUTH_SESSION );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    private void testAuth() throws Exception {
        // close sessionMgr
        sessionMgr.close();

        // write file by session
        try {
            write( session );
            // check results
            SiteWrapper[] expSites = { site };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList.get( 0 ), expSites,
                    localPath, filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // use closed sessionMgr to get session
        ScmSession session = null;
        try {
            session = sessionMgr.getSession( SessionType.AUTH_SESSION );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        // close sessionMgr again
        sessionMgr.close();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmSessionMgr createSessionMgr() {
        List< String > urlList = new ArrayList< String >();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + site.getSiteServiceName() );
        }
        ScmConfigOption scOpt;
        ScmSessionMgr sessionMgr = null;
        try {
            scOpt = new ScmConfigOption( urlList, TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            sessionMgr = ScmFactory.Session.createSessionMgr( scOpt, 1000 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return sessionMgr;
    }

    private ScmId write( ScmSession session ) throws ScmException {
        ScmId fileId = null;
        // create file
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( name + "_" + UUID.randomUUID() );
        file.setAuthor( name );
        file.setTitle( "sequoiacm" );
        fileId = file.save();
        fileIdList.add( fileId );
        return fileId;
    }
}
