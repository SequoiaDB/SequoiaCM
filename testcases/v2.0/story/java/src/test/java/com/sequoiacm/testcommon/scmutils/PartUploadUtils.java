package com.sequoiacm.testcommon.scmutils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @Description part upload function common class
 * @author wuyan
 * @Date 2019.04.12
 * @version 1.00
 */
public class PartUploadUtils extends TestScmBase {
    public static final int partLimitMinSize = 1024 * 1024 * 5;

    /**
     * @Description initiate multipart upload
     * @param s3Client
     * @param bucketName
     * @param key
     * @return the uploadId for partUpload,the type is string.
     */
    public static String initPartUpload( AmazonS3 s3Client, String bucketName,
            String key ) {
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
                bucketName, key );
        ObjectMetadata metadata = new ObjectMetadata();
        initRequest.setObjectMetadata( metadata );
        InitiateMultipartUploadResult result = s3Client
                .initiateMultipartUpload( initRequest );
        String uploadId = result.getUploadId();
        return uploadId;
    }

    /**
     * @Description upload mulitpart
     * @param s3Client
     * @param bucketName
     * @param key
     * @param uploadId
     * @param file
     *            upload object file
     * @return the list of part number and Etag
     */
    public static List< PartETag > partUpload( AmazonS3 s3Client,
            String bucketName, String key, String uploadId, File file ) {
        return PartUploadUtils.partUpload( s3Client, bucketName, key, uploadId,
                file, PartUploadUtils.partLimitMinSize );
    }

    /**
     * @Description upload mulitpart
     * @param s3Client
     * @param bucketName
     * @param key
     * @param uploadId
     * @param file
     *            upload object file
     * @param partSize
     * @return the list of part number and Etag
     */
    public static List< PartETag > partUpload( AmazonS3 s3Client,
            String bucketName, String key, String uploadId, File file,
            long partSize ) {
        List< PartETag > partEtags = new ArrayList<>();
        int filePosition = 0;
        long fileSize = file.length();
        for ( int i = 1; filePosition < fileSize; i++ ) {
            long eachPartSize = Math.min( partSize, fileSize - filePosition );
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( filePosition )
                    .withPartNumber( i ).withPartSize( eachPartSize )
                    .withBucketName( bucketName ).withKey( key )
                    .withUploadId( uploadId );
            UploadPartResult uploadPartResult = s3Client
                    .uploadPart( partRequest );
            partEtags.add( uploadPartResult.getPartETag() );
            filePosition += partSize;
        }
        return partEtags;
    }

    /**
     * @Description 复制分段上传
     * @param s3Client
     * @param sourceBucketName
     * @param sourceKey
     * @param targetBucketName
     * @param targetKey
     * @param uploadId
     * @param sourceObjectSize
     * @return the list of part number and Etag
     */
    public static List< PartETag > partUploadCopy( AmazonS3 s3Client,
            String sourceBucketName, String sourceKey, String targetBucketName,
            String targetKey, String uploadId, long sourceObjectSize ) {
        return PartUploadUtils.partUploadCopy( s3Client, sourceBucketName,
                sourceKey, targetBucketName, targetKey, uploadId,
                PartUploadUtils.partLimitMinSize, sourceObjectSize );
    }

    /**
     * @Description 复制分段上传
     * @param s3Client
     * @param sourceBucketName
     * @param sourceKey
     * @param targetBucketName
     * @param targetKey
     * @param uploadId
     * @param partSize
     * @param sourceObjectSize
     * @return the list of part number and Etag
     */
    public static List< PartETag > partUploadCopy( AmazonS3 s3Client,
            String sourceBucketName, String sourceKey, String targetBucketName,
            String targetKey, String uploadId, long partSize,
            long sourceObjectSize ) {
        List< PartETag > partEtags = new ArrayList<>();
        long filelength = sourceObjectSize;
        long filepositon = 0L;
        for ( int i = 1; filepositon < filelength; i++ ) {
            long partsize = Math.min( partSize, ( filelength - filepositon ) );
            CopyPartRequest request = new CopyPartRequest()
                    .withUploadId( uploadId ).withPartNumber( i )
                    .withSourceBucketName( sourceBucketName )
                    .withSourceKey( sourceKey )
                    .withDestinationBucketName( targetBucketName )
                    .withDestinationKey( targetKey )
                    .withFirstByte( filepositon )
                    .withLastByte( filepositon + partsize - 1 );
            CopyPartResult copyResult = s3Client.copyPart( request );
            s3Client.copyPart( request );
            partEtags.add( copyResult.getPartETag() );
            filepositon += partsize;
        }
        return partEtags;
    }

    /**
     * @Description complete multipart upload
     * @param s3Client
     * @param bucketName
     * @param key
     * @param uploadId
     * @param partEtags
     *            container for the part number and Etag of an uploaded part
     * @return the result infos by complete multipart upload.
     */
    public static CompleteMultipartUploadResult completeMultipartUpload(
            AmazonS3 s3Client, String bucketName, String key, String uploadId,
            List< PartETag > partEtags ) {
        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest()
                .withBucketName( bucketName ).withKey( key )
                .withUploadId( uploadId ).withPartETags( partEtags );
        CompleteMultipartUploadResult result = s3Client
                .completeMultipartUpload( completeRequest );
        return result;
    }

    /**
     * @Description check the part upload info after abort multipartUpload,than check the key.
     * @param s3Client
     * @param bucketName
     * @param uploadId
     * @return
     */
    public static void checkAbortMultipartUploadResult( AmazonS3 s3Client,
            String bucketName, String keyName, String uploadId ) {
        // check listparts no upload part.
        try {
            ListPartsRequest listRequest = new ListPartsRequest( bucketName,
                    keyName, uploadId );
            s3Client.listParts( listRequest );
            Assert.fail( "listParts must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchUpload",
                    "---statuscode=" + e.getStatusCode() );
        }

        // get key is not exist.
        try {
            s3Client.getObject( bucketName, keyName );
            Assert.fail( "get not exist key must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchKey" );
        }
    }

    /**
     * @Description list Parts,than check the partNumber of the returned result
     * @param s3Client
     * @param bucketName
     * @param keyName
     * @param partEtags
     * @param uploadId
     * @return
     */
    public static void listPartsAndCheckPartNumbers( AmazonS3 s3Client,
            String bucketName, String keyName, List< PartETag > partEtags,
            String uploadId ) {
        List< Integer > expPartNumbersList = new ArrayList<>();
        for ( PartETag expPartNumbers : partEtags ) {
            int partNumber = expPartNumbers.getPartNumber();
            expPartNumbersList.add( partNumber );
        }
        Collections.sort( expPartNumbersList );

        ListPartsRequest request = new ListPartsRequest( bucketName, keyName,
                uploadId );
        PartListing listResult = s3Client.listParts( request );
        List< PartSummary > listParts = listResult.getParts();
        List< Integer > actPartNumbersList = new ArrayList<>();
        for ( PartSummary partNumbers : listParts ) {
            int partNumber = partNumbers.getPartNumber();
            actPartNumbersList.add( partNumber );
        }

        // check the keyName
        Assert.assertEquals( actPartNumbersList, expPartNumbersList,
                "actPartNumbersList:" + actPartNumbersList
                        + "  expPartNumbersList:"
                        + expPartNumbersList.toString() );
    }

    /**
     * @Description list MultipartUploads,than check the upload and CommonPrefixes of the returned result
     * @param result
     *            the result by listMultipartUploads
     * @param expCommonPrefixes
     * @param expUploads
     *            include key and uploadId.
     * @return
     */
    public static void checkListMultipartUploadsResults(
            MultipartUploadListing result, List< String > expCommonPrefixes,
            MultiValueMap< String, String > expUploads ) {
        Collections.sort( expCommonPrefixes );
        List< String > actCommonPrefixes = result.getCommonPrefixes();
        Assert.assertEquals( actCommonPrefixes, expCommonPrefixes,
                "actCommonPrefixes = " + actCommonPrefixes.toString()
                        + ",expCommonPrefixes = "
                        + expCommonPrefixes.toString() );
        List< MultipartUpload > multipartUploads = result.getMultipartUploads();
        MultiValueMap< String, String > actUploads = new LinkedMultiValueMap< String, String >();
        for ( MultipartUpload multipartUpload : multipartUploads ) {
            String keyName = multipartUpload.getKey();
            String uploadId = multipartUpload.getUploadId();
            actUploads.add( keyName, uploadId );
        }

        Assert.assertEquals( actUploads.size(), expUploads.size(),
                "actMap = " + actUploads.toString() + ",expUpload = "
                        + expUploads.toString() );
        for ( Map.Entry< String, List< String > > entry : expUploads
                .entrySet() ) {
            Assert.assertEquals( actUploads.get( entry.getKey() ),
                    expUploads.get( entry.getKey() ),
                    "actMap = " + actUploads.toString() + ",expMap = "
                            + expUploads.toString() );
        }
    }

}
