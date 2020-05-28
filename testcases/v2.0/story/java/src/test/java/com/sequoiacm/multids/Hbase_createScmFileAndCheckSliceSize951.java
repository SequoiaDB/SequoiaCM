package com.sequoiacm.multids;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
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
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-951:写入到Hbase的文件分片大小检查（文件大于1M）
 * @author huangxiaoni init
 * @date 2017.11.9
 */

public class Hbase_createScmFileAndCheckSliceSize951 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "file951";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 2
            + new Random().nextInt( 1024 * 1024 );
    private File localPath = null;
    private String filePath = null;

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
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            DatasourceType dsType = site.getDataType();
            if ( dsType.equals( DatasourceType.HBASE ) ) {
                this.createScmFile();
                this.getScmFile();
                this.checkHbaseFileSliceSize();

                runSuccess = true;
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
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

    private void createScmFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName + "_" + UUID.randomUUID() );
        fileId = file.save();
    }

    private void getScmFile() throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
    }

    private void checkHbaseFileSliceSize() throws ScmException {
        Connection conn = null;
        try {
            conn = TestSdbTools.getHbaseConnect( site );
            String tableName = TestSdbTools.getDataTableNameInHbase( site,
                    wsp );
            Table table = conn.getTable( TableName.valueOf( tableName ) );

            byte[] buffer = TestTools.getBuffer( filePath );
            int num = ( int ) Math
                    .ceil( Double.valueOf( buffer.length ) / ( 1024 * 1024 ) );
            for ( int i = 0; i < num; i++ ) {
                Get get = new Get( Bytes.toBytes( fileId.get() ) );
                get.addColumn( Bytes.toBytes( "SCM_FILE_DATA" ),
                        Bytes.toBytes( "PIECE_NUM_" + i ) );
                Result result = table.get( get );
                int actSize = result.value().length;

                int expSize = 1024 * 1024;
                if ( i == num - 1 ) {
                    expSize = buffer.length - 1024 * 1024 * i;
                }

                Assert.assertEquals( actSize, expSize,
                        "fileId=" + fileId.get() );
            }
            table.close();
        } catch ( IOException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            try {
                if ( null != conn ) {
                    conn.close();
                }
            } catch ( IOException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

}