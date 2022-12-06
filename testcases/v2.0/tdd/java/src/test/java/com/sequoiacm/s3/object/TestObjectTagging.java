package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestObjectTagging extends ScmTestMultiCenterBase {

    private AmazonS3 s3Client;
    private ScmSession ss;
    private String bucketName = "s3-bucket-object-tagging";
    private String objName = ScmTestTools.getClassName();
    private String workDir;
    private String filePath;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workDir);
        ScmTestTools.createDir(workDir);
    }

    @Test
    public void testTagging() throws IOException {
        // create bucket
        s3Client.createBucket(bucketName);
        // put object
        filePath = workDir + File.separator + objName + ".txt";
        ScmTestTools.createFile(filePath, objName, 1024);
        PutObjectRequest putObjReq = new PutObjectRequest(bucketName, objName, new File(filePath));
        s3Client.putObject(putObjReq);

        // set tagging
        List<Tag> list = new ArrayList<>();
        list.add(new Tag("b", "h"));
        list.add(new Tag("d", "g"));
        list.add(new Tag("a", "f"));
        list.add(new Tag("c", "w"));
        ObjectTagging tagging = new ObjectTagging(list);
        SetObjectTaggingRequest request = new SetObjectTaggingRequest(bucketName, objName, tagging);
        s3Client.setObjectTagging(request);

        // get tagging
        GetObjectTaggingRequest getTagReq = new GetObjectTaggingRequest(bucketName, objName);
        GetObjectTaggingResult objectTagging = s3Client.getObjectTagging(getTagReq);
        Assert.assertEquals(objectTagging.getTagSet().size(), list.size());
        Assert.assertEquals(objectTagging.getTagSet(), sortList(list));

        // get object tagging count
        S3Object s3Object = s3Client.getObject(bucketName, objName);
        Assert.assertEquals(s3Object.getTaggingCount(), Integer.valueOf(list.size()));

        // set object tagging number more than 10
        List<Tag> numberList = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            numberList.add(new Tag("a" + i, "b" + i));
        }
        ObjectTagging numberTagging = new ObjectTagging(numberList);
        SetObjectTaggingRequest numberReq = new SetObjectTaggingRequest(bucketName, objName,
                numberTagging);
        try {
            s3Client.setObjectTagging(numberReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.BAD_REQUEST);
        }

        // set object tagging key length more than 128
        List<Tag> keyLengthList = new ArrayList<>();
        keyLengthList.add(new Tag(genStr(129), "b1"));
        ObjectTagging keyLengthTagging = new ObjectTagging(keyLengthList);
        SetObjectTaggingRequest keyLengthReq = new SetObjectTaggingRequest(bucketName, objName,
                keyLengthTagging);
        try {
            s3Client.setObjectTagging(keyLengthReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set file customTag value length more than 256
        List<Tag> valueLengthList = new ArrayList<>();
        valueLengthList.add(new Tag("a1", genStr(257)));
        ObjectTagging valueLengthTagging = new ObjectTagging(valueLengthList);
        SetObjectTaggingRequest valueLengthReq = new SetObjectTaggingRequest(bucketName, objName,
                valueLengthTagging);
        try {
            s3Client.setObjectTagging(valueLengthReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set object empty tagging
        List<Tag> emptyList = new ArrayList<>();
        ObjectTagging emptyTagging = new ObjectTagging(emptyList);
        SetObjectTaggingRequest emptyReq = new SetObjectTaggingRequest(bucketName, objName,
                emptyTagging);
        s3Client.setObjectTagging(emptyReq);
        // check res
        GetObjectTaggingRequest emptyTagReq = new GetObjectTaggingRequest(bucketName, objName);
        GetObjectTaggingResult emptyTagRes = s3Client.getObjectTagging(emptyTagReq);
        Assert.assertEquals(emptyTagRes.getTagSet().size(), emptyList.size());

        // set object tagging same key
        List<Tag> sameKeyList = new ArrayList<>();
        sameKeyList.add(new Tag("a2", "b2"));
        sameKeyList.add(new Tag("a2", "b3"));
        ObjectTagging sameKeyTagging = new ObjectTagging(sameKeyList);
        SetObjectTaggingRequest sameKeyReq = new SetObjectTaggingRequest(bucketName, objName,
                sameKeyTagging);
        try {
            s3Client.setObjectTagging(sameKeyReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set object tagging key is null
        List<Tag> nullKeyList = new ArrayList<>();
        nullKeyList.add(new Tag(null, "b1"));
        ObjectTagging nullKeyTagging = new ObjectTagging(nullKeyList);
        SetObjectTaggingRequest nullKeyReq = new SetObjectTaggingRequest(bucketName, objName,
                nullKeyTagging);
        try {
            s3Client.setObjectTagging(nullKeyReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set object tagging key is ""
        List<Tag> emptyKeyList = new ArrayList<>();
        emptyKeyList.add(new Tag("", "b1"));
        ObjectTagging emptyKeyTagging = new ObjectTagging(emptyKeyList);
        SetObjectTaggingRequest emptyKeyReq = new SetObjectTaggingRequest(bucketName, objName,
                emptyKeyTagging);
        try {
            s3Client.setObjectTagging(emptyKeyReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set object tagging value is null
        List<Tag> nullValueList = new ArrayList<>();
        nullValueList.add(new Tag("a", null));
        ObjectTagging nullValueTagging = new ObjectTagging(nullValueList);
        SetObjectTaggingRequest nullValueReq = new SetObjectTaggingRequest(bucketName, objName,
                nullValueTagging);
        try {
            s3Client.setObjectTagging(nullValueReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // set object tagging value is ""
        List<Tag> emptyValueList = new ArrayList<>();
        emptyValueList.add(new Tag("a", ""));
        ObjectTagging emptyValueTagging = new ObjectTagging(emptyValueList);
        SetObjectTaggingRequest emptyValueReq = new SetObjectTaggingRequest(bucketName, objName,
                emptyValueTagging);
        try {
            s3Client.setObjectTagging(emptyValueReq);
        }
        catch (AmazonS3Exception e) {
            Assert.assertEquals(e.getErrorCode(), S3ErrorCode.INVALID_TAG);
        }

        // delete object tagging
        DeleteObjectTaggingRequest deleteObjReq = new DeleteObjectTaggingRequest(bucketName,
                objName);
        s3Client.deleteObjectTagging(deleteObjReq);
        // check res
        GetObjectTaggingRequest deleteTagReq = new GetObjectTaggingRequest(bucketName, objName);
        GetObjectTaggingResult deleteTagRes = s3Client.getObjectTagging(deleteTagReq);
        Assert.assertEquals(deleteTagRes.getTagSet().size(), 0);

        // put object with set tagging
        String putObjName = "testPutObject";
        PutObjectRequest putObjectReq = new PutObjectRequest(bucketName, putObjName,
                new ByteArrayInputStream("tagging".getBytes()), new ObjectMetadata());
        List<Tag> putObjList = new ArrayList<>();
        putObjList.add(new Tag("a1", "b1"));
        ObjectTagging putObjTagging = new ObjectTagging(putObjList);
        putObjectReq.setTagging(putObjTagging);
        s3Client.putObject(putObjectReq);
        // check res
        GetObjectTaggingRequest putObjTagReq = new GetObjectTaggingRequest(bucketName, putObjName);
        GetObjectTaggingResult putObjTagRes = s3Client.getObjectTagging(putObjTagReq);
        Assert.assertEquals(putObjTagRes.getTagSet().size(), putObjList.size());
        // delete object
        s3Client.deleteObject(bucketName, putObjName);

        // multipart upload object and set tagging
        String multiObjName = "testMultiObject";
        String filePath = workDir + File.separator + multiObjName + ".txt";
        ScmTestTools.createFile(filePath, multiObjName, 1024);

        File file = new File(filePath);
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024;
        List<PartETag> partETags = new ArrayList<PartETag>();
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName,
                multiObjName);
        List<Tag> multiList = new ArrayList<>();
        multiList.add(new Tag("a1", "b1"));
        multiList.add(new Tag("a2", "b2"));
        ObjectTagging multiTagging = new ObjectTagging(multiList);
        initRequest.setTagging(multiTagging);
        InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

        long filePosition = 0;
        for (int i = 1; filePosition < contentLength; i++) {
            partSize = Math.min(partSize, (contentLength - filePosition));
            UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName)
                    .withKey(multiObjName).withUploadId(initResponse.getUploadId())
                    .withPartNumber(i).withFileOffset(filePosition).withFile(file)
                    .withPartSize(partSize);
            UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
            partETags.add(uploadResult.getPartETag());
            filePosition += partSize;
        }
        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName,
                multiObjName, initResponse.getUploadId(), partETags);
        s3Client.completeMultipartUpload(compRequest);
        // check res
        GetObjectTaggingRequest multiObjTagReq = new GetObjectTaggingRequest(bucketName,
                multiObjName);
        GetObjectTaggingResult multiObjTagRes = s3Client.getObjectTagging(multiObjTagReq);
        Assert.assertEquals(multiObjTagRes.getTagSet().size(), multiList.size());
        Assert.assertEquals(multiObjTagRes.getTagSet(), sortList(multiList));
        // delete object
        s3Client.deleteObject(bucketName, multiObjName);

        // copy object and set tagging
        String targetObjName = "testTargetObject";
        CopyObjectRequest copyObjReq = new CopyObjectRequest(bucketName, objName, bucketName,
                targetObjName);
        List<Tag> newTagList = new ArrayList<>();
        newTagList.add(new Tag("a_new", "b_new"));
        ObjectTagging copyTagging = new ObjectTagging(newTagList);
        copyObjReq.setNewObjectTagging(copyTagging);
        s3Client.copyObject(copyObjReq);
        // check res
        GetObjectTaggingRequest copyObjTagReq = new GetObjectTaggingRequest(bucketName,
                targetObjName);
        GetObjectTaggingResult copyObjTagRes = s3Client.getObjectTagging(copyObjTagReq);
        Assert.assertEquals(copyObjTagRes.getTagSet().size(), newTagList.size());
        Assert.assertEquals(copyObjTagRes.getTagSet(), sortList(newTagList));
        // delete object
        s3Client.deleteObject(bucketName, targetObjName);
    }

    private String genStr(int num) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

    private List<Tag> sortList(List<Tag> list) {
        list.sort(new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return list;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (null != objName) {
                s3Client.deleteObject(bucketName, objName);
            }
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
