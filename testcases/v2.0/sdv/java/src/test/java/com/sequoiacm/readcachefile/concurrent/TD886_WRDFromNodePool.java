package com.sequoiacm.readcachefile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-886:scm登入连接包含多个节点，并发做数据操作
 * @author huangxiaoni init
 * @date 2017.8.14
 */
// TODO：netstat统计节点随机获取地址是否均匀
public class TD886_WRDFromNodePool extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;

    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private String author = "TD886";
    private int fileSize = 10;
    private int fileNum = 2;
    private File localPath = null;
    private String filePath = null;
    private List< String > gateWayList;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
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
            sessionM = TestScmTools.createSession( site );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
            gateWayList = TestScmBase.gateWayList;

            BSONObject cond = ScmQueryBuilder.start()
                    .put( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            WRDFile WRDFile = new WRDFile();
            WRDFile.start( 50 );

            if ( !( WRDFile.isSuccess() ) ) {
                Assert.fail( WRDFile.getErrorMsg() );
            }

            // check results
            BSONObject cond = ScmQueryBuilder.start()
                    .put( ScmAttributeName.File.AUTHOR ).is( author ).get();
            long cnt = ScmFactory.File.countInstance( wsM,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( cnt, 0 );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }

        }
    }

    private class WRDFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                ScmConfigOption opt = new ScmConfigOption();
                // List<NodeWrapper> nodes = site.getNodes(2);
                // opt.addUrl(nodes.get(0).getHost()+":"+nodes.get(0).getPort());
                // opt.addUrl(nodes.get(1).getHost()+":"+nodes.get(1).getPort());
                for ( String gateWay : gateWayList ) {
                    opt.addUrl( gateWay + "/" + site.getSiteServiceName() );
                }
                opt.setUser( scmUserName );
                opt.setPasswd( scmPassword );
                session = ScmFactory.Session
                        .createSession( SessionType.AUTH_SESSION, opt );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // bizOper
                for ( int i = 0; i < fileNum; i++ ) {
                    // write
                    ScmFile file = ScmFactory.File.createInstance( ws );
                    file.setContent( filePath );
                    file.setFileName( author + "_" + UUID.randomUUID() );
                    file.setAuthor( author );
                    ScmId fileId = file.save();

                    // read
                    String downloadPath = TestTools.LocalFile.initDownloadPath(
                            localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
                    ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
                    file2.getContent( downloadPath );
                    Assert.assertEquals( TestTools.getMD5( filePath ),
                            TestTools.getMD5( downloadPath ) );

                    // delete
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
