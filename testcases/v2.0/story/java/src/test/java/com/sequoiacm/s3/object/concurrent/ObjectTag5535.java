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
 * @descreption SCM-5535:复制对象与删除标签操作并发
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5535 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private Map< String, String > map = new HashMap<>();
    private List< Tag > tagSet = new ArrayList<>();
    private String bucketName;
    private String srcObjectKey = "object5535a";
    private String destObjectKey = "object5335b";
    private String tagKey = "key5535";
    private String tagValue = "value5535";
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
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, srcObjectKey );
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, destObjectKey );
        s3Client.putObject( bucketName, srcObjectKey, "test" );
        fileID = S3Utils.queryS3Object( ws, srcObjectKey );
        initTag();
        S3Utils.setObjectTag( s3Client, bucketName, srcObjectKey, tagSet );
    }

    @Test()
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new S3CopyObject() );
        t.addWorker( new SCMDeleteTag() );
        t.run();

        // 在线程中校验结果
        ScmId destFileID = S3Utils.queryS3Object( ws, destObjectKey );
        ScmFile file = ScmFactory.File.getInstance( ws, destFileID );
        Map< String, String > customTag = file.getCustomTag();
        if ( customTag.size() != 0 ) {
            Assert.assertEquals( customTag, map );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        srcObjectKey );
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        destObjectKey );
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

    private class S3CopyObject {
        @ExecuteOrder(step = 1)
        private void run() {
            s3Client.copyObject( bucketName, srcObjectKey, bucketName,
                    destObjectKey );
        }
    }

    private class SCMDeleteTag {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmFile file = ScmFactory.File.getInstance( ws, fileID );
            file.deleteCustomTag();
        }
    }

    private void initTag() {
        tagSet.add( new Tag( tagKey, tagValue ) );
        map.put( tagKey, tagValue );
    }
}
