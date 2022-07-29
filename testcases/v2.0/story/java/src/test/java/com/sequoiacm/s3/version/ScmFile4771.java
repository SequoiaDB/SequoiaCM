package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @Description SCM-4771 :: 开启版本控制，带版本号删除文件不存在
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4771 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4771";
    private String fileName = "scmfile4771";
    private SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private int fileSize = 1024;
    private byte[] filedata = new byte[ fileSize ];
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
        new Random().nextBytes( filedata );
        S3Utils.createFile( scmBucket, fileName, filedata );
    }

    @Test
    public void test() throws Exception {
        // 指定版本号删除不存在的版本文件
        int currentVersion = 1;
        scmBucket.deleteFileVersion( "noexistFile4771", currentVersion, 0 );

        // 指定版本号不存在
        int errorVersion = 2;
        scmBucket.deleteFileVersion( fileName, errorVersion, 0 );

        // 原版本文件还存在未删除
        ScmFile file = scmBucket.getFile( fileName );
        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertFalse( file.isDeleted() );
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
