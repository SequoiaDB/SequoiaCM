package com.sequoiacm.s3.object.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
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
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5532:并发设置标签和删除相同对象标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5532 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< Tag > tagSet = new ArrayList<>();
    private Map< String, String > map = new HashMap<>();
    private String bucketName;
    private String objectKey = "object5532";
    private String tagKey = "key5532";
    private String tagValue = "value5532";
    private ScmWorkspace ws;
    private ScmId fileID;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        bucketName = TestScmBase.bucketName;
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, objectKey );
        s3Client.putObject( bucketName, objectKey, "test" );
        fileID = S3Utils.queryS3Object( ws, objectKey );
        initTag();
        S3Utils.setObjectTag( s3Client, bucketName, objectKey, tagSet );
    }

    @Test()
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new S3DeleteTag() );
        t.addWorker( new SCMSetTag() );
        t.run();

        // 获取结果校验
        ScmFile file = ScmFactory.File.getInstance( ws, fileID );
        Map< String, String > customTag = file.getCustomTag();
        if ( customTag.get( tagKey ) != null ) {
            Assert.assertEquals( customTag, map );
        } else {
            Assert.assertEquals( customTag, new HashMap<>() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        objectKey );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class S3DeleteTag {
        @ExecuteOrder(step = 1)
        private void run() {
            s3Client.deleteObjectTagging(
                    new DeleteObjectTaggingRequest( bucketName, objectKey ) );
        }
    }

    private class SCMSetTag {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmFile file = ScmFactory.File.getInstance( ws, fileID );
            file.setCustomTag( map );
        }
    }

    private void initTag() {
        tagSet.add( new Tag( tagKey, tagValue ) );
        map.put( tagKey, tagValue );
    }
}
