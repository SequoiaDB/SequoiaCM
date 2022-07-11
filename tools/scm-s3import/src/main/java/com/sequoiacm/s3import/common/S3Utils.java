package com.sequoiacm.s3import.common;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.module.S3Bucket;
import com.sequoiacm.s3import.module.S3ImportObject;
import org.apache.commons.codec.binary.StringUtils;

import java.util.*;

public class S3Utils {
    private static int S3_SAME_OBJECT_CHECK_NUM = 10;
    private static int S3_MAX_LIST_NUM = 1000;

    public static void checkAndInitVersionControl(AmazonS3Client srcClient,
            AmazonS3Client destClient, List<S3Bucket> bucketList) throws ScmToolsException {
        Set<String> srcBuckets = new HashSet<>();
        for (Bucket srcBucket : srcClient.listBuckets()) {
            srcBuckets.add(srcBucket.getName());
        }

        Set<String> destBuckets = new HashSet<>();
        for (Bucket destBucket : destClient.listBuckets()) {
            destBuckets.add(destBucket.getName());
        }

        for (S3Bucket s3Bucket : bucketList) {
            if (!srcBuckets.contains(s3Bucket.getName())) {
                throw new ScmToolsException(
                        "Source bucket does not exist, bucket=" + s3Bucket.getName(),
                        S3ImportExitCode.ENV_ERROR);
            }
            if (!destBuckets.contains(s3Bucket.getDestName())) {
                throw new ScmToolsException(
                        "Dest bucket does not exist, dest_bucket=" + s3Bucket.getDestName(),
                        S3ImportExitCode.ENV_ERROR);
            }

            String statusOfSrcBucket = srcClient
                    .getBucketVersioningConfiguration(s3Bucket.getName()).getStatus();
            String statusOfDestBucket = destClient
                    .getBucketVersioningConfiguration(s3Bucket.getDestName()).getStatus();

            if (statusOfSrcBucket.equals(CommonDefine.VersionConf.OFF)) {
                if (!statusOfDestBucket.equals(CommonDefine.VersionConf.OFF)) {
                    throw new ScmToolsException(
                            "The required version control status of the dest bucket is:"
                                    + CommonDefine.VersionConf.OFF + ", desBucket="
                                    + s3Bucket.getDestName() + ", current status="
                                    + statusOfDestBucket,
                            S3ImportExitCode.ENV_ERROR);
                }
                continue;
            }

            if (!statusOfDestBucket.equals(CommonDefine.VersionConf.ENABLED)) {
                throw new ScmToolsException(
                        "The required version control status of the dest bucket is:"
                                + CommonDefine.VersionConf.ENABLED + ", desBucket="
                                + s3Bucket.getDestName() + ", current status=" + statusOfDestBucket,
                        S3ImportExitCode.ENV_ERROR);
            }
            s3Bucket.setEnableVersionControl(true);
        }
    }

    public static S3ImportObject getS3Object(AmazonS3Client s3Client, String bucket,
            boolean isVersionControl, String key) {
        List<S3ImportObject> importObjects = listS3Object(s3Client, bucket, isVersionControl, key,
                CommonDefine.KeyMarker.BEGINNING, 1, new ListContext());

        if (importObjects.size() == 0) {
            return null;
        }
        S3ImportObject importObject = importObjects.get(0);
        return importObject.getKey().equals(key) ? importObject : null;
    }

    public static List<S3ImportObject> listS3Object(AmazonS3Client s3Client, String bucket,
            boolean isVersionControl, String keyMarker, int count, ListContext listContext) {
        return listS3Object(s3Client, bucket, isVersionControl, null, keyMarker, count,
                listContext);
    }

    public static List<S3ImportObject> listS3Object(AmazonS3Client s3Client, String bucket,
            boolean isVersionControl, String prefix, String keyMarker, int count,
            ListContext listContext) {
        List<S3ImportObject> objectList = new ArrayList<>();
        while (count > 0 && keyMarker != CommonDefine.KeyMarker.END) {
            List<S3ImportObject> tmpObjectList = internalListS3Object(s3Client, bucket,
                    isVersionControl, prefix, keyMarker, count, listContext);
            objectList.addAll(tmpObjectList);
            count -= tmpObjectList.size();
            keyMarker = listContext.getNextKeyMarker();
        }
        return objectList;
    }

    private static List<S3ImportObject> internalListS3Object(AmazonS3Client s3Client, String bucket,
            boolean isVersionControl, String prefix, String keyMarker, int count,
            ListContext listContext) {
        List<S3ImportObject> importList = new ArrayList<>();
        String lastKey = null;

        if (isVersionControl) {
            List<S3VersionSummary> allSummary = new ArrayList<>();
            ListVersionsRequest request = new ListVersionsRequest();
            request.withBucketName(bucket).withKeyMarker(keyMarker).withPrefix(prefix)
                    .withMaxResults(count);

            boolean checkNextObjects = true;
            VersionListing result = listVersions(s3Client, request);
            List<S3VersionSummary> objectList = result.getVersionSummaries();
            if (objectList.size() == 0) {
                return importList;
            }
            allSummary.addAll(objectList);

            // 记录最后一个key
            lastKey = objectList.get(objectList.size() - 1).getKey();
            listContext.setNextKeyMarker(result.getNextKeyMarker());

            int maxResult = S3_SAME_OBJECT_CHECK_NUM;
            // 一直查询到服务端没有相同的 lastKey 为止
            while (checkNextObjects && result.isTruncated()) {
                request.setKeyMarker(result.getNextKeyMarker());
                request.setVersionIdMarker(result.getNextVersionIdMarker());
                maxResult = maxResult < S3_MAX_LIST_NUM ? maxResult * 2 : S3_MAX_LIST_NUM;
                request.setMaxResults(maxResult);
                result = listVersions(s3Client, request);
                listContext.setNextKeyMarker(result.getNextKeyMarker());
                List<S3VersionSummary> tempList = result.getVersionSummaries();
                for (S3VersionSummary object : tempList) {
                    if (!object.getKey().equals(lastKey)) {
                        checkNextObjects = false;
                        listContext.setNextKeyMarker(lastKey);
                        break;
                    }
                    allSummary.add(object);
                }
            }

            importList = generateImportObject(allSummary);
        }
        else {
            ListObjectsRequest request = new ListObjectsRequest();
            request.withBucketName(bucket).withMarker(keyMarker).withPrefix(prefix)
                    .withMaxKeys(count);
            ObjectListing objectList = s3Client.listObjects(request);

            List<S3ObjectSummary> summaryList = objectList.getObjectSummaries();
            for (S3ObjectSummary summary : summaryList) {
                S3ImportObject o = new S3ImportObject(summary);
                importList.add(o);
            }
            // 对象列取最后一个 nextKeyMarker 为空串 ""，列取至末尾时将 keyMarker 置空，避免循环列取
            listContext.setNextKeyMarker(objectList.isTruncated() ? objectList.getNextMarker()
                    : CommonDefine.KeyMarker.END);
        }

        return importList;
    }

    // 兼容 SequoiaS3 的列取结果，每一次版本列取的结果需要按 key 升序排序
    private static VersionListing listVersions(AmazonS3Client s3Client,
            ListVersionsRequest request) {
        VersionListing versionListing = s3Client.listVersions(request);
        List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
        Collections.sort(versionSummaries, new Comparator<S3VersionSummary>() {
            @Override
            public int compare(S3VersionSummary o1, S3VersionSummary o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return versionListing;
    }


    /**
     * @param allSummary 版本列取结果，版本号为逆序（e.g. [key-v3、key-v2、key-v1]）
     *                   构造多版本对象时需要逆向添加版本记录 -> key(v1、v2、v3)
     * @return 对象列表，对象内封装了多个版本记录
     */
    private static List<S3ImportObject> generateImportObject(List<S3VersionSummary> allSummary) {
        LinkedList<S3ImportObject> importList = new LinkedList<>();

        String currentKey = null;
        S3ImportObject currentObject = null;
        for (S3VersionSummary summary : allSummary) {
            if (currentKey == null || !currentKey.equals(summary.getKey())) {
                currentKey = summary.getKey();
                currentObject = new S3ImportObject(summary.getBucketName(), summary.getKey(), true);
                importList.add(currentObject);
            }
            currentObject.addVersionSummaryHistory(summary);
            if (summary.isDeleteMarker()) {
                currentObject.setHasDeleteMarker(true);
            }
        }

        for (S3ImportObject s3ImportObject : importList) {
            // 不存在 deleteMarker 时不需要重新排序，listVersion 即可保证版本有序性
            if (!s3ImportObject.isHasDeleteMarker()) {
                continue;
            }

            List<S3VersionSummary> summaries = s3ImportObject.getVersionSummaryList();
            // 按 versionId 排序
            Collections.sort(summaries, new Comparator<S3VersionSummary>() {
                @Override
                public int compare(S3VersionSummary o1, S3VersionSummary o2) {
                    return o1.getVersionId().compareTo(o2.getVersionId());
                }
            });

            // 检查是否存在 null 版本
            S3VersionSummary lastSummary = summaries.get(summaries.size() - 1);
            if (lastSummary.getVersionId().equals("null")) {
                for (int i = 0; i < summaries.size() - 1; i++) {
                    if (summaries.get(i).getLastModified()
                            .compareTo(lastSummary.getLastModified()) > 0) {
                        summaries.add(i, summaries.remove(summaries.size() - 1));
                        break;
                    }
                }
            }
        }
        return importList;
    }

    public static S3Object getObject(AmazonS3Client s3Client, GetObjectRequest request) {
        return s3Client.getObject(request);
    }

    public static ObjectMetadata getObjectMetadata(AmazonS3Client s3Client,
            GetObjectMetadataRequest request) {
        return s3Client.getObjectMetadata(request);
    }

    public static void deleteObject(AmazonS3Client s3Client, DeleteObjectRequest request) {
        s3Client.deleteObject(request);
    }

    public static void deleteVersion(AmazonS3Client s3Client, DeleteVersionRequest request) {
        s3Client.deleteVersion(request);
    }

    // s3Object 中 lastModified 的时间粒度为秒
    // 需要另外传入一个更精确的时间（通过 listObjects 或 listVersions 接口拿到的对象创建时间，其粒度为毫秒数）
    public static void putObject(AmazonS3Client s3Client, String bucket, S3Object s3Object,
            long objCreateTime) throws ScmToolsException {
        ObjectMetadata objectMetadata = s3Object.getObjectMetadata();
        objectMetadata.setHeader(CommonDefine.SCM_OBJ_CREATE_TIME, objCreateTime);

        String eTag = objectMetadata.getETag();
        if (Md5Utils.isETagValid(eTag)) {
            PutObjectResult putObjectResult = s3Client.putObject(bucket, s3Object.getKey(),
                    s3Object.getObjectContent(), objectMetadata);
            if (!StringUtils.equals(putObjectResult.getETag(), eTag)) {
                throw new ScmToolsException("Upload object failed",
                        S3ImportExitCode.ETAG_NOT_MATCH);
            }
        }
        else {
            InputStreamWithCalc is = new InputStreamWithCalc(s3Object.getObjectContent());
            PutObjectResult putObjectResult = s3Client.putObject(bucket, s3Object.getKey(), is,
                    objectMetadata);
            if (!is.getEtag().equals(putObjectResult.getETag())) {
                throw new ScmToolsException("Upload object failed",
                        S3ImportExitCode.ETAG_NOT_MATCH);
            }
        }
    }
}
