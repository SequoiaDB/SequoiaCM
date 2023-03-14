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
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @Description SCM-4773 :: 开启版本控制，带版本号删除文件，该文件无历史版本
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4773 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4773";
    private String fileName = "scmfile4773";
    private ScmId fileId = null;
    private byte[] filedata = new byte[ 1024 * 300 ];
    private SiteWrapper site = null;
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
        new Random().nextBytes( filedata );
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        int currentVersion = 1;
        scmBucket.deleteFileVersion( fileName, currentVersion, 0 );
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
