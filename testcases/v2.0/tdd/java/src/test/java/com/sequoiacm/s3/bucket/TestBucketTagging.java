package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.TagSet;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.s3.common.S3ErrorCode;
import com.sequoiacm.testcommon.S3Utils;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TestBucketTagging extends ScmTestMultiCenterBase {

    private AmazonS3 s3Client;
    private ScmSession ss;
    private String bucketName = "s3-bucket-tagging";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void testTagging() {
        // create bucket
        s3Client.createBucket(bucketName);
        // set bucket tagging
        List<TagSet> list = new ArrayList<>();
        TagSet tagSet = new TagSet();
        tagSet.setTag("b", "h");
        tagSet.setTag("d", "g");
        tagSet.setTag("a", "f");
        tagSet.setTag("c", "w");
        list.add(tagSet);
        BucketTaggingConfiguration taggingConfiguration = new BucketTaggingConfiguration();
        taggingConfiguration.setTagSets(list);
        s3Client.setBucketTaggingConfiguration(bucketName, taggingConfiguration);

        // get bucket tagging
        BucketTaggingConfiguration resConfig = s3Client.getBucketTaggingConfiguration(bucketName);
        Assert.assertEquals(resConfig.getAllTagSets().size(), list.size());
        List<TagSet> sortedList = sortList(list);
        Assert.assertTrue(compareList(resConfig.getAllTagSets(), sortedList));

        // set bucket tagging number more than 50
        List<TagSet> numberList = new ArrayList<>();
        TagSet numberTagSet = new TagSet();
        for (int i = 0; i < 51; i++) {
            tagSet.setTag("a" + i, "b" + i);
        }
        numberList.add(numberTagSet);
        BucketTaggingConfiguration numberConfig = new BucketTaggingConfiguration();
        numberConfig.setTagSets(numberList);
        try {
            s3Client.setBucketTaggingConfiguration(bucketName, numberConfig);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.BAD_REQUEST);
        }

        // set bucket empty tagging
        List<TagSet> emptyList = new ArrayList<>();
        BucketTaggingConfiguration emptyConfig = new BucketTaggingConfiguration();
        emptyConfig.setTagSets(emptyList);
        s3Client.setBucketTaggingConfiguration(bucketName, emptyConfig);
        // check res
        BucketTaggingConfiguration emptyConfigRes = s3Client
                .getBucketTaggingConfiguration(bucketName);
        Assert.assertEquals(emptyConfigRes.getAllTagSets().size(), 1);

        // set bucket tagging key is null
        List<TagSet> nullKeyList = new ArrayList<>();
        TagSet nullKeyTagSet = new TagSet();
        nullKeyTagSet.setTag(null, "b1");
        numberList.add(nullKeyTagSet);
        BucketTaggingConfiguration nullKeyConfig = new BucketTaggingConfiguration();
        emptyConfig.setTagSets(nullKeyList);
        try {
            s3Client.setBucketTaggingConfiguration(bucketName, nullKeyConfig);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set bucket tagging key is ""
        List<TagSet> emptyKeyList = new ArrayList<>();
        TagSet emptyKeyTagSet = new TagSet();
        emptyKeyTagSet.setTag("", "b2");
        numberList.add(emptyKeyTagSet);
        BucketTaggingConfiguration emptyKeyConfig = new BucketTaggingConfiguration();
        emptyConfig.setTagSets(emptyKeyList);
        try {
            s3Client.setBucketTaggingConfiguration(bucketName, emptyKeyConfig);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set bucket tagging value is null
        List<TagSet> nullValueList = new ArrayList<>();
        TagSet nullValueTagSet = new TagSet();
        nullValueTagSet.setTag("a1", null);
        numberList.add(nullValueTagSet);
        BucketTaggingConfiguration nullValueConfig = new BucketTaggingConfiguration();
        emptyConfig.setTagSets(nullValueList);
        s3Client.setBucketTaggingConfiguration(bucketName, nullValueConfig);
        // check res
        BucketTaggingConfiguration nullValueRes = s3Client
                .getBucketTaggingConfiguration(bucketName);
        Assert.assertEquals(nullValueRes.getAllTagSets().size(), 1);

        // set bucket tagging value is ""
        List<TagSet> emptyValueList = new ArrayList<>();
        TagSet emptyValueTagSet = new TagSet();
        emptyValueTagSet.setTag("a1", "");
        numberList.add(emptyValueTagSet);
        BucketTaggingConfiguration emptyValueConfig = new BucketTaggingConfiguration();
        emptyConfig.setTagSets(emptyValueList);
        s3Client.setBucketTaggingConfiguration(bucketName, emptyValueConfig);
        // check res
        BucketTaggingConfiguration emptyValueRes = s3Client
                .getBucketTaggingConfiguration(bucketName);
        Assert.assertEquals(emptyValueRes.getAllTagSets().size(), 1);

        // delete bucket tagging
        s3Client.deleteBucketTaggingConfiguration(bucketName);
        // check res
        BucketTaggingConfiguration deleteConfig = s3Client
                .getBucketTaggingConfiguration(bucketName);
        Assert.assertEquals(deleteConfig.getAllTagSets().size(), 1);
    }

    private List<TagSet> sortList(List<TagSet> list) {
        List<TagSet> sortedList = new ArrayList<>();
        for (TagSet tagSet : list) {
            Map<String, String> sortedMap = new TreeMap<>(tagSet.getAllTags());
            TagSet newTagSet = new TagSet();
            for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                newTagSet.setTag(entry.getKey(), entry.getValue());
            }
            sortedList.add(newTagSet);
        }
        return sortedList;
    }

    private boolean compareList(List<TagSet> list, List<TagSet> targetList) {
        for (TagSet tagSet : list) {
            Map<String, String> map = tagSet.getAllTags();
            for (TagSet otherTagSet : targetList) {
                Map<String, String> targetMap = otherTagSet.getAllTags();
                if (!map.equals(targetMap)) {
                    return false;
                }
            }
        }
        return true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (null != bucketName) {
                s3Client.deleteBucket(bucketName);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
