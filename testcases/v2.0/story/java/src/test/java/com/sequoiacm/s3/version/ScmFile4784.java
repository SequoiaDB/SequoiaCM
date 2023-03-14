package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description SCM-4784 ::桶开启版本控制，指定文件物理删除
 * @author wuyan
 * @Date 2022.07.12
 * @version 1.00
 */
public class ScmFile4784 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4784";
    private String fileName = "scmfile4784";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 8;
    private byte[] updatedata = new byte[ updateSize ];
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
        scmBucket.enableVersionControl();
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
        ScmFileUtils.createFile( scmBucket, fileName, updatedata );
    }

    @Test
    public void test() throws Exception {
        ScmFactory.File.deleteInstance( ws, fileId, true );
        Assert.assertEquals( scmBucket.countFile( null ), 0 );
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
