package com.sequoiacm.s3.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5520:对象不存在，操作对象标签
 * @author YiPan
 * @date 2022/12/8
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5520 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< Tag > tagSet = new ArrayList<>();
    private Map< String, String > map = new HashMap<>();
    private String bucketName;
    private String keyName = "Object5520";
    private ScmWorkspace ws;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        bucketName = TestScmBase.bucketName;

        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        initTag();
    }

    @Test
    public void test() throws ScmException {
        testS3API();

        testSCMAPI();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                s3Client.deleteObject( bucketName, keyName );
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

    private void initTag() {
        String baseKey = "test";
        String baseValue = "value";
        for ( int i = 0; i < 9; i++ ) {
            tagSet.add( new Tag( baseKey + i, baseValue + i ) );
            map.put( baseKey + i, baseValue + i );
        }
    }

    private void testSCMAPI() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( keyName );
        ScmId fielID = file.save();
        ScmFactory.File.deleteInstance( ws, fielID, true );
        // 设置标签
        try {
            file.setCustomTag( map );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND );
        }

        // 获取标签
        try {
            Map< String, String > customTag = file.getCustomTag();
            // TODO:SEQUOIACM-1177
            // Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND );
        }

        // 删除标签
        try {
            file.deleteCustomTag();
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND );
        }
    }

    private void testS3API() {
        // 设置标签
        try {
            S3Utils.setObjectTag( s3Client, bucketName, keyName, tagSet );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "NoSuchKey" ) ) {
                throw e;
            }
        }

        // 获取标签
        try {
            s3Client.getObjectTagging(
                    new GetObjectTaggingRequest( bucketName, keyName ) );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "NoSuchKey" ) ) {
                throw e;
            }
        }

        // 删除标签
        try {
            s3Client.deleteObjectTagging(
                    new DeleteObjectTaggingRequest( bucketName, keyName ) );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().equals( "NoSuchKey" ) ) {
                throw e;
            }
        }
    }
}
