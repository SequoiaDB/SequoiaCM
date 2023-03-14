package com.sequoiacm.s3.bucket;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @descreption SCM-4253 :: 使用SCM API删除不存在桶
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4253 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String bucketName = "bucket4253";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException, IOException {
        ScmFactory.Bucket.createBucket( ws, bucketName );

        ScmFactory.Bucket.deleteBucket( session, bucketName );

        try {
            ScmFactory.Bucket.deleteBucket( session, bucketName );
            Assert.fail( "Delete bucket：" + bucketName + " should failed" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BUCKET_NOT_EXISTS ) {
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
