package com.sequoiacm.s3.version;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;

import java.util.Random;

/**
 * @author wuyan
 * @version 1.00
 * @Description SCM-5014:不开启版本控制，重复多次创建同名文件
 * @Date 2022.07.26
 */
public class ScmFile5014 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5014";
    private String fileName = "scmfile5014";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 0;
    private byte[] filedata = new byte[ fileSize ];
    private String author = "author5014";
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
    }

    @Test
    public void test() throws Exception {
        int createFileCount = 20;
        int updateSize = 0;
        byte[] updatedata = new byte[ 0 ];
        for ( int i = 0; i < createFileCount; i++ ) {
            updateSize = i * 200;
            updatedata = new byte[ updateSize ];
            new Random().nextBytes( updatedata );
            ScmFileUtils.createFile( scmBucket, fileName, updatedata, author );
        }

        int version = -2;
        ScmFile file = scmBucket.getFile( fileName );
        Assert.assertEquals( file.getMajorVersion(), version );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertTrue( file.isNullVersion() );
        Assert.assertEquals( file.getSize(), updateSize );
        Assert.assertEquals( file.getVersionSerial().getMajorSerial(),
                createFileCount + 1 );
        Assert.assertEquals( file.getAuthor(), author );
        S3Utils.checkFileContent( file, updatedata );

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
}
