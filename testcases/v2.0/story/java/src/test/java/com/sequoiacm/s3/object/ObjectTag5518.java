package com.sequoiacm.s3.object;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @descreption SCM-5518:桶外文件设置标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5518 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< Tag > tagSet = new ArrayList<>();
    private List< Tag > newTagSet = new ArrayList<>();
    private Map< String, String > map = new HashMap<>();
    private Map< String, String > newMap = new HashMap<>();
    private String bucketName;
    private String keyName = "Object5518";
    private ScmWorkspace ws;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
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
        BSONObject query = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( keyName ).get();
        ScmFileUtils.cleanFile( ws.getName(), query );
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, keyName );
    }

    @Test
    public void test() throws ScmException {
        // 创建桶外文件，设置标签
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( keyName );
        file.setCustomTag( map );
        ScmId fileId = file.save();

        // 挂载到桶内
        ScmFactory.Bucket.attachFile( session, bucketName, fileId );

        GetObjectTaggingResult objectTagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );

        compareTagSet( objectTagging.getTagSet(), tagSet );

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
        for ( int i = 0; i < 5; i++ ) {
            tagSet.add( new Tag( baseKey + i, baseValue + i ) );
            map.put( baseKey + i, baseValue + i );
        }
        newTagSet.add( new Tag( baseKey, baseValue ) );
        newMap.put( baseKey, baseValue );
    }

    private void compareTagSet( List< Tag > actTagSet, List< Tag > expTagSet ) {
        for ( int i = 0; i < actTagSet.size(); i++ ) {
            Assert.assertEquals( actTagSet.get( i ).getKey(),
                    expTagSet.get( i ).getKey() );
            Assert.assertEquals( actTagSet.get( i ).getValue(),
                    expTagSet.get( i ).getValue() );
        }
    }
}
