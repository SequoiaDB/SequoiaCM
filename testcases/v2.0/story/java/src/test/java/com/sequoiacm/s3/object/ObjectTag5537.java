package com.sequoiacm.s3.object;

import java.util.*;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
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
 * @descreption SCM-5537:对象标签规则校验
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5537 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< List< Tag > > validTags = new ArrayList<>();
    private List< List< Tag > > invalidTags = new ArrayList<>();
    private List< Map< String, String > > validMaps = new ArrayList<>();
    private List< Map< String, String > > invalidMaps = new ArrayList<>();
    private String bucketName;
    private ScmId fileId;
    private ScmWorkspace ws;
    private String objectKey = "Object5537";
    protected String bigString = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        bucketName = TestScmBase.bucketName;
        S3Utils.deleteObjectAllVersions( s3Client, bucketName, objectKey );
        initTags();
        s3Client.putObject( bucketName, objectKey, "test" );
        fileId = S3Utils.queryS3Object( ws, objectKey );
    }

    @Test
    public void test() throws ScmException {
        // key为无效值(s3 api)
        for ( List< Tag > tagSetList : invalidTags ) {
            try {
                S3Utils.setObjectTag( s3Client, bucketName, objectKey,
                        tagSetList );
                Assert.fail( "except fail but success" );
            } catch ( AmazonS3Exception e ) {
                if ( !e.getErrorCode().equals( "InvalidTag" ) ) {
                    throw e;
                }
            }
        }

        // key为有效值(s3 api)
        for ( List< Tag > tagSetList : validTags ) {
            S3Utils.setObjectTag( s3Client, bucketName, objectKey, tagSetList );
            GetObjectTaggingResult tagging = s3Client.getObjectTagging(
                    new GetObjectTaggingRequest( bucketName, objectKey ) );
            compareTagSet( tagging.getTagSet(), tagSetList );
        }

        // key为无效值(scm api)
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        for ( Map< String, String > map : invalidMaps ) {
            try {
                file.setCustomTag( map );
                Assert.fail( "except fail but success" );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(),
                        ScmError.FILE_INVALID_CUSTOMTAG );
            }
        }

        // key为有效值(scm api)
        for ( Map< String, String > map : validMaps ) {
            file.setCustomTag( map );
            Assert.assertEquals( file.getCustomTag(), map );
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

    private void initTags() {
        // 有效标签
        addValidTags( "testNormal", "value" );
        addValidTags( "testKey", "" );
        addValidTags( "testNull", null );
        addValidTags( "testValue", bigString );

        // 无效标签
        addInvalidTags( null, "test" );
        addInvalidTags( "", "test" );
        addInvalidTags( bigString, "value" );
        addInvalidTags( "testBigValue", bigString + bigString );

    }

    private void addValidTags( String key, String value ) {
        Map< String, String > map = new HashMap<>();
        map.put( key, value );
        validMaps.add( map );
        List< Tag > tagList = new ArrayList<>();
        tagList.add( new Tag( key, value ) );
        validTags.add( tagList );
    }

    private void addInvalidTags( String key, String value ) {
        Map< String, String > map = new HashMap<>();
        map.put( key, value );
        invalidMaps.add( map );
        List< Tag > tagList = new ArrayList<>();
        tagList.add( new Tag( key, value ) );
        invalidTags.add( tagList );
    }

    public static void compareTagSet( List< Tag > actTagSet,
            List< Tag > expTagSet ) {
        for ( int i = 0; i < actTagSet.size(); i++ ) {
            String actKey = actTagSet.get( i ).getKey();
            String actValue = actTagSet.get( i ).getValue();
            Assert.assertEquals( actKey, expTagSet.get( i ).getKey() );
            if ( !Objects.equals( actKey, "testNull" ) ) {
                Assert.assertEquals( actValue, expTagSet.get( i ).getValue() );
            } else {
                // null会被转换为"",教时额外处理
                Assert.assertEquals( actValue, "" );
            }
        }
    }
}
