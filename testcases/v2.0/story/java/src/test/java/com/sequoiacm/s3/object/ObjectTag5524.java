package com.sequoiacm.s3.object;

import java.io.File;
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
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5524:对象标签SCM API和S3 API互通性测试
 * @author YiPan
 * @date 2022/12/8
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5524 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< Tag > tagSet = new ArrayList<>();
    private Map< String, String > map = new HashMap<>();
    private List< Tag > newTagSet = new ArrayList<>();
    private Map< String, String > newMap = new HashMap<>();
    private String bucketName;
    private String keyName = "Object5524";
    private ScmWorkspace ws;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private ScmId fileID;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        bucketName = TestScmBase.bucketName;

        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        initTag();
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, keyName );
        s3Client.putObject( bucketName, keyName, filePath );
        fileID = S3Utils.queryS3Object( ws, keyName );
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
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        keyName );
                TestTools.LocalFile.removeFile( localPath );
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
        newMap.put( baseKey, baseValue );
        newTagSet.add( new Tag( baseKey, baseValue ) );
    }

    private void testSCMAPI() throws ScmException {
        S3Utils.setObjectTag( s3Client, bucketName, keyName, tagSet );
        ScmFile file = ScmFactory.File.getInstance( ws, fileID );

        // 获取标签
        Map< String, String > actMap = file.getCustomTag();
        Assert.assertEquals( actMap, map );

        // 更新标签
        file.setCustomTag( newMap );
        Assert.assertEquals( file.getCustomTag(), newMap );

        // 删除标签
        file.deleteCustomTag();
        Assert.assertEquals( file.getCustomTag(),
                new HashMap< String, String >() );
    }

    private void testS3API() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileID );
        file.setCustomTag( map );

        // 获取标签
        GetObjectTaggingResult tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        S3Utils.compareTagSet( tagging.getTagSet(), tagSet );

        // 更新标签
        S3Utils.setObjectTag( s3Client, bucketName, keyName, newTagSet );
        tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        S3Utils.compareTagSet( tagging.getTagSet(), newTagSet );

        // 删除标签
        s3Client.deleteObjectTagging(
                new DeleteObjectTaggingRequest( bucketName, keyName ) );
        tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        newTagSet.clear();
        S3Utils.compareTagSet( tagging.getTagSet(), newTagSet );
    }

    private void compareTag( Map< String, String > map, List< Tag > tagSet ) {
        Assert.assertEquals( map.size(), tagSet.size() );
        for ( Tag tag : tagSet ) {
            Assert.assertEquals( tag.getValue(), map.get( tag.getKey() ) );
        }
    }
}
