package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
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
 * @Description:SCM-716: 多个ws下并发读文件 1、创建多个ws； 2、并发在多个ws下写文件； 3、并发读取多个ws下的文件；
 *                       4、检查并发写和并发读文件结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class WriteAndReadInMutiWs716 extends TestScmBase {
    private static final int fileNum = 10;
    private static final String author = "WriteAndReadInMutiWs716";
    private final int wsNum = 3;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private List< WsWrapper > wsList = null;
    private int fileSize = 1024 * 100;
    private List< ScmId > fileIdList0 = new ArrayList<>();
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            site = ScmInfo.getSite();
            wsList = ScmInfo.getWss( wsNum );

            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            // write
            WriteScmFile wThread1 = new WriteScmFile( wsList.get( 0 ),
                    fileIdList0 );
            wThread1.start();

            WriteScmFile wThread2 = new WriteScmFile( wsList.get( 1 ),
                    fileIdList1 );
            wThread2.start();

            WriteScmFile wThread3 = new WriteScmFile( wsList.get( 2 ),
                    fileIdList2 );
            wThread3.start();

            if ( !( wThread1.isSuccess() && wThread2.isSuccess() &&
                    wThread3.isSuccess() ) ) {
                Assert.fail( wThread1.getErrorMsg() + wThread2.getErrorMsg() +
                        wThread3.getErrorMsg() );
            }

            // read
            ReadScmFile rThread1 = new ReadScmFile( wsList.get( 0 ),
                    fileIdList0 );
            rThread1.start();

            ReadScmFile rThread2 = new ReadScmFile( wsList.get( 1 ),
                    fileIdList1 );
            rThread2.start();

            ReadScmFile rThread3 = new ReadScmFile( wsList.get( 2 ),
                    fileIdList2 );
            rThread3.start();

            if ( !( rThread1.isSuccess() && rThread2.isSuccess() &&
                    rThread3.isSuccess() ) ) {
                Assert.fail( rThread1.getErrorMsg() + rThread2.getErrorMsg() +
                        rThread3.getErrorMsg() );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( wsList.get( 0 ), cond );
                ScmFileUtils.cleanFile( wsList.get( 1 ), cond );
                ScmFileUtils.cleanFile( wsList.get( 2 ), cond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {

        }
    }

    private class WriteScmFile extends TestThreadBase {
        private WsWrapper wsp = null;
        private List< ScmId > fileIdList = null;

        public WriteScmFile( WsWrapper wsp, List< ScmId > fileIdList ) {
            this.wsp = wsp;
            this.fileIdList = fileIdList;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                for ( int i = 0; i < fileNum; i++ ) {
                    ScmFile file = ScmFactory.File.createInstance( ws );
                    file.setContent( filePath );
                    file.setFileName( author + "_" + UUID.randomUUID() );
                    file.setAuthor( author );
                    fileIdList.add( file.save() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ReadScmFile extends TestThreadBase {
        private WsWrapper wsp = null;
        private List< ScmId > fileIdList = null;

        public ReadScmFile( WsWrapper wsp, List< ScmId > fileIdList ) {
            this.wsp = wsp;
            this.fileIdList = fileIdList;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                for ( ScmId fileId : fileIdList ) {
                    ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                    String downloadPath = TestTools.LocalFile
                            .initDownloadPath( localPath,
                                    TestTools.getMethodName(),
                                    Thread.currentThread().getId() );
                    file.getContent( downloadPath );
                    Assert.assertEquals( TestTools.getMD5( filePath ),
                            TestTools.getMD5( downloadPath ) );
                }
            } catch ( Exception e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( null != session ) {
                    session.close();
                }
            }
        }
    }
}
