package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * @Description SCM-4757 :: 不指定版本获取文件
 * @author wuyan
 * @Date 2022.07.05
 * @version 1.00
 */
public class ScmFile4757 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4757";
    private String fileName = "scmfile4757";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 12;
    private byte[] filedata = new byte[ fileSize ];
    private byte[] updatedata = new byte[ updateSize ];
    private ScmWorkspace ws = null;
    private long bucketId;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        bucketId = scmBucket.getId();
        new Random().nextBytes( filedata );
        new Random().nextBytes( updatedata );
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
        S3Utils.createFile( scmBucket, fileName, updatedata );
    }

    @Test
    public void test() throws Exception {
        ScmFile file = scmBucket.getFile( fileName );
        checkFileAttributes( file );
        checkFileContent( file );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileContent( ScmFile file ) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] downloadData = outputStream.toByteArray();
        VersionUtils.assertByteArrayEqual( downloadData, updatedata );
    }

    private void checkFileAttributes( ScmFile file ) {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getTitle(), "sequoiacm" );
        Assert.assertEquals( file.getSize(), updateSize );
        Assert.assertEquals( file.getBucketId().longValue(), bucketId );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), 2 );
        Assert.assertFalse( file.isNullVersion() );
    }
}
