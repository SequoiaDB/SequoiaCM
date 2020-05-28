package com.sequoiacm.readcachefile.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
 * @Testcase: SCM-725:所有中心系统时间不在同一月份，并发在不同中心写入文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */
// TODO:添加修改系统时间的操作
public class TD725_WriteFileFromDiffSite extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String author = "TD725";
    private List< ScmId > fileIdList1 = Collections
            .synchronizedList( new ArrayList< ScmId >() );
    private List< ScmId > fileIdList2 = Collections
            .synchronizedList( new ArrayList< ScmId >() );
    private List< ScmId > fileIdList3 = Collections
            .synchronizedList( new ArrayList< ScmId >() );
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;

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

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( rootSite );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" }, enabled = false)
    private void test() {
        try {
            // modify host's system time
            TestTools.setSystemTime( rootSite.getNode().getHost(),
                    new Date().getTime() - 1000 * 60 * 5 ); // 5min
            TestTools.setSystemTime( branSites.get( 0 ).getNode().getHost(),
                    new Date().getTime() - 1000 * 60 * 10 ); // 10min
            TestTools.setSystemTime( branSites.get( 1 ).getNode().getHost(),
                    new Date().getTime() - 1000 * 60 * 15 ); // 15min

            // write scmfile
            WriteFromM WriteFromM = new WriteFromM();
            WriteFromM.start( 10 );

            WriteFromA WriteFromA = new WriteFromA();
            WriteFromA.start( 10 );

            WriteFromB WriteFromB = new WriteFromB();
            WriteFromB.start( 10 );

            if ( !( WriteFromM.isSuccess() && WriteFromA.isSuccess()
                    && WriteFromB.isSuccess() ) ) {
                Assert.fail( WriteFromM.getErrorMsg() + WriteFromA.getErrorMsg()
                        + WriteFromB.getErrorMsg() );
            }

            // check results
            SiteWrapper[] expSites1 = { rootSite };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList1, expSites1,
                    localPath, filePath );

            SiteWrapper[] expSites2 = { branSites.get( 0 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList2, expSites2,
                    localPath, filePath );

            SiteWrapper[] expSites3 = { branSites.get( 1 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileIdList3, expSites3,
                    localPath, filePath );

            // recovery host's system time
            TestTools.restoreSystemTime( rootSite.getNode().getHost() );
            TestTools.restoreSystemTime(
                    branSites.get( 0 ).getNode().getHost() );
            TestTools.restoreSystemTime(
                    branSites.get( 1 ).getNode().getHost() );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }

                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }

                for ( ScmId fileId : fileIdList3 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }

                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class WriteFromM extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // write scmfile
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                ScmId fileId = file.save();

                fileIdList1.add( fileId );

                // check attribute
                ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
                long createTime = file2.getCreateTime().getTime();
                Assert.assertEquals( file2.getUpdateTime().getTime(),
                        createTime );
                Assert.assertEquals(
                        file2.getLocationList().get( 0 ).getDate().getTime(),
                        createTime );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class WriteFromA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // write scmfile
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                ScmId fileId = file.save();

                fileIdList2.add( fileId );

                // check attribute
                ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
                long createTime = file2.getCreateTime().getTime();
                Assert.assertEquals( file2.getUpdateTime().getTime(),
                        createTime );
                Assert.assertEquals(
                        file2.getLocationList().get( 0 ).getDate().getTime(),
                        createTime );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class WriteFromB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // write scmfile
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                ScmId fileId = file.save();

                fileIdList3.add( fileId );

                // check attribute
                ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
                long createTime = file2.getCreateTime().getTime();
                Assert.assertEquals( file2.getUpdateTime().getTime(),
                        createTime );
                Assert.assertEquals(
                        file2.getLocationList().get( 0 ).getDate().getTime(),
                        createTime );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    // private void modifyHostSystemTime(String host, Long date) throws
    // Exception {
    // Ssh ssh = null;
    // try {
    // ssh = new Ssh(host);
    //
    // // modify system time
    // SimpleDateFormat dateFm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // String modifyDate = dateFm.format(date);
    // ssh.exec("date -s \"" + modifyDate + "\"");
    //
    // // print local date after set date
    // ssh.exec("date");
    // System.out.println("after modify system time, host="+ host +",
    // localDate=" + ssh.getStdout());
    // } finally {
    // if (null != ssh) {
    // ssh.disconnect();
    // }
    // }
    // }
    //
    // private void recoveryHostSystemTime(String host) throws Exception {
    // Ssh ssh = null;
    // try {
    // ssh = new Ssh(TestScmBase.mainHostName);
    // ssh.exec("sudo ntpdate " + TestScmBase.timeServerHost);
    //
    // // print local date after set date
    // ssh.exec("date");
    // System.out.println("after restore system time, host="+ host +",
    // localDate=" + ssh.getStdout());
    // } finally {
    // if (null != ssh) {
    // ssh.disconnect();
    // }
    // }
    // }

}
