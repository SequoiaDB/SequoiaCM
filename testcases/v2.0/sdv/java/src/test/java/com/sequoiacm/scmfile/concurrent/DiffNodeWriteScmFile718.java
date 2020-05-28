package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * @Description:SCM-718 : 本地中心不同节点并发写文件 1、同一个中心的不同节点并发写文件；
 *                      2、检查写入后文件站点列表、文件内容正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class DiffNodeWriteScmFile718 extends TestScmBase {
    private static final String author = "DiffNodeWriteScmFile718";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private int fileSize = 1024 * 20;
    private List< ScmId > fileIdList = Collections
            .synchronizedList( new ArrayList< ScmId >() );
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();

            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            WriteScmFile wThread = new WriteScmFile();
            wThread.start( 20 );

            if ( !( wThread.isSuccess() ) ) {
                Assert.fail( wThread.getErrorMsg() );
            }

            checkResult();
            checkResult();

        } catch ( Exception e ) {
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
                ScmFileUtils.cleanFile( wsp, cond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {

        }
    }

    private void checkResult() {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            for ( ScmId fileId : fileIdList ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                file.getContent( downloadPath );
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );

                this.checkFileAttributes( file );

                SiteWrapper[] expSites = { site };
                ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                        filePath );
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

    private void checkFileAttributes( ScmFile file ) {
        try {
            Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private class WriteScmFile extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // write
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                fileIdList.add( file.save() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
