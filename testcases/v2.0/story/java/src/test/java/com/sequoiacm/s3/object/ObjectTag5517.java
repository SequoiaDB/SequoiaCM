package com.sequoiacm.s3.object;

import java.io.File;
import java.util.*;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.SetObjectTaggingResult;
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
 * @descreption SCM-5517:多版本对象操作标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5517 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< Tag > tagSet = new ArrayList<>();
    private Map< String, String > map = new HashMap<>();
    private String bucketName;
    private String keyName = "Object5517";
    private ScmWorkspace ws;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private int versionNum = 3;
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
        bucketName = TestScmBase.enableVerBucketName;

        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        initTag();
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, keyName );
        for ( int i = 0; i < versionNum; i++ ) {
            s3Client.putObject( bucketName, keyName, filePath );
        }
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
        for ( int i = 5; i > 0; i-- ) {
            tagSet.add( new Tag( baseKey + i, baseValue + i ) );
            map.put( baseKey + i, baseValue + i );
        }
    }

    private void testSCMAPI() throws ScmException {
        ScmId fileID = S3Utils.queryS3Object( ws, keyName );
        ScmFile file = ScmFactory.File.getInstance( ws, fileID );
        // 设置标签
        file.setCustomTag( map );

        // 获取标签
        Assert.assertEquals( file.getCustomTag(), map );
    }

    private void testS3API() throws ScmException {
        // 设置标签
        SetObjectTaggingResult result = S3Utils.setObjectTag( s3Client,
                bucketName, keyName, tagSet );
        // TODO:SEQUOIACM-1174
        // Assert.assertEquals( result.getVersionId(), "3.0" );

        // 获取标签
        GetObjectTaggingResult objectTagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        Collections.sort( tagSet, new ListComparator() );
        Assert.assertEquals( objectTagging.getTagSet(), tagSet );

        // 历史版本设置标签
        String version = "1.0";
        result = S3Utils.setObjectTag( s3Client, bucketName, keyName, version,
                tagSet );
        Assert.assertEquals( result.getVersionId(), version );

        // 获取历史版本标签
        GetObjectTaggingRequest request = new GetObjectTaggingRequest(
                bucketName, keyName );
        request.setVersionId( version );
        objectTagging = s3Client.getObjectTagging( request );
        Assert.assertEquals( objectTagging.getTagSet(), tagSet );

        // deleteMarker
        // s3Client.deleteObject( bucketName, keyName );
        // TODO:SEQUOIACM-1173
        // result = S3Utils.setObjectTag( s3Client, bucketName, keyName, tagSet
        // );
        // s3Client.deleteVersion( bucketName, keyName, "4.0" );
    }

    private class ListComparator implements Comparator< Tag > {
        @Override
        public int compare( Tag t1, Tag t2 ) {
            return t1.getKey().compareTo( t2.getKey() );
        }
    }
}
