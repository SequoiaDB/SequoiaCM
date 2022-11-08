package com.sequoiacm.s3import.command;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import com.sequoiacm.s3import.common.*;
import com.sequoiacm.s3import.config.ImportPathConfig;
import com.sequoiacm.s3import.config.ImportToolProps;
import com.sequoiacm.s3import.config.S3ClientManager;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.module.*;
import com.sequoiacm.s3import.progress.CompareProgress;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sequoiacm.s3import.common.CommonDefine.DiffType.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

@Command
public class CompareCommand extends SubCommand {

    private final static Logger logger = LoggerFactory.getLogger(CompareCommand.class);
    public static final String NAME = "compare";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "quickly compare data differences in buckets";
    }

    @Override
    protected Options commandOptions() throws ScmToolsException {
        Options ops = super.commandOptions();
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.BUCKET).hasArg(true)
                .desc("buckets that need to compare data").build());
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.RESET).hasArg(false)
                .desc("reset the comparison progress").build());
        return ops;
    }

    @Override
    protected S3ImportOptions parseCommandLineArgs(CommandLine cl) throws ScmToolsException {
        ArgUtils.checkRequiredOption(cl, CommonDefine.Option.BUCKET);

        S3ImportOptions options = super.parseCommandLineArgs(cl);
        String bucketStr = cl.getOptionValue(CommonDefine.Option.BUCKET);
        options.setBucketList(ArgUtils.parseS3Bucket(bucketStr, NAME));
        options.setResetCompareProgress(cl.hasOption(CommonDefine.Option.RESET));
        return options;
    }

    @Override
    protected void checkAndInitBucketConf(S3ImportOptions importOptions) throws ScmToolsException {
        super.checkAndInitBucketConf(importOptions);
        List<S3Bucket> bucketList = importOptions.getBucketList();

        String progressFilePath = ImportPathConfig.getInstance().getCompareProgressFilePath();
        File progressFile = new File(progressFilePath);
        if (!progressFile.exists()) {
            FileOperateUtils.updateProgress(progressFilePath, bucketList);
            return;
        }

        List<S3Bucket> checkS3BucketList = new ArrayList<>();
        ScmFileResource fileResource = ScmResourceFactory.getInstance()
                .createFileResource(progressFile);
        try {
            JsonArray progresses = new JsonParser().parse(fileResource.readFile()).getAsJsonArray();
            for (JsonElement progress : progresses) {
                S3Bucket s3Bucket = new S3Bucket(progress.getAsJsonObject(), NAME);
                checkS3BucketList.add(s3Bucket);
            }
        }
        finally {
            fileResource.release();
        }
        if (checkS3BucketList.size() != bucketList.size()) {
            throw new ScmToolsException(
                    "The current working directory is already occupied by "
                            + CommonUtils.bucketListToStr(checkS3BucketList)
                            + " ,please specify another working directory.",
                    S3ImportExitCode.INVALID_ARG);
        }
        // 记录用户原来的桶数据
        String originalData = CommonUtils.bucketListToStr(checkS3BucketList);
        Collections.sort(bucketList);
        Collections.sort(checkS3BucketList);
        for (int i = 0; i < bucketList.size(); i++) {
            S3Bucket s3Bucket = bucketList.get(i);
            S3Bucket checkS3Bucket = checkS3BucketList.get(i);
            if (!s3Bucket.equals(checkS3Bucket)) {
                throw new ScmToolsException(
                        "The current working directory is already occupied by " + originalData
                                + " ,please specify another working directory.",
                        S3ImportExitCode.INVALID_ARG);
            }
            if (!importOptions.isResetCompareProgress()) {
                s3Bucket.setProgress(checkS3Bucket.getProgress());
            }
        }
    }

    @Override
    protected void process(S3ImportOptions importOptions) throws ScmToolsException {
        List<S3Bucket> bucketList = importOptions.getBucketList();

        S3ClientManager clientManager = S3ClientManager.getInstance();
        AmazonS3Client srcS3Client = clientManager.getSrcS3Client();
        AmazonS3Client destS3Client = clientManager.getDestS3Client();

        ImportPathConfig pathConfig = ImportPathConfig.getInstance();
        String compareResultPath = pathConfig.getCompareResultPath();
        String progressFilePath = pathConfig.getCompareProgressFilePath();
        ScmCommon.createDir(compareResultPath);
        System.out.println("executing data comparison, result output path: " + compareResultPath);
        logger.info("executing data comparison, result output path: {}", compareResultPath);
        if (importOptions.isResetCompareProgress()) {
            FileOperateUtils.backupCompareResult(compareResultPath);
        }

        ImportToolProps toolProps = ImportToolProps.getInstance();
        int batchSize = toolProps.getBatchSize();
        long startTime = System.currentTimeMillis();
        long runStartTime;

        int workCount = toolProps.getWorkCount();
        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(workCount);
            for (S3Bucket s3Bucket : bucketList) {
                CompareProgress progress = (CompareProgress) s3Bucket.getProgress();
                if (progress.getStatus().equals(CommonDefine.ProgressStatus.FINISH)) {
                    continue;
                }
                progress.setStatus(CommonDefine.ProgressStatus.RUNNING);

                String srcKeyMarker = progress.getSrcNextKeyMarker();
                String destKeyMarker = progress.getDestNextKeyMarker();
                String lastSrcKey = srcKeyMarker;
                String lastDestKey = destKeyMarker;
                ListContext srcListContext = new ListContext();
                ListContext destListContext = new ListContext();

                Queue<CompareResult> diffList = new LinkedList<>();
                Queue<S3ImportObject> srcObjects = new LinkedList<>();
                Queue<S3ImportObject> destObjects = new LinkedList<>();
                while (true) {
                    runStartTime = System.currentTimeMillis();
                    if (srcObjects.size() * 2 < batchSize) {
                        List<S3ImportObject> tmpSrcObjects = S3Utils.listS3Object(srcS3Client,
                                s3Bucket.getName(), s3Bucket.isEnableVersionControl(), srcKeyMarker,
                                batchSize, srcListContext);
                        srcObjects.addAll(tmpSrcObjects);
                        srcKeyMarker = srcListContext.getNextKeyMarker();
                    }

                    if (destObjects.size() * 2 < batchSize) {
                        List<S3ImportObject> tmpDestObjects = S3Utils.listS3Object(destS3Client,
                                s3Bucket.getDestName(), s3Bucket.isEnableVersionControl(),
                                destKeyMarker, batchSize, destListContext);
                        destObjects.addAll(tmpDestObjects);
                        destKeyMarker = destListContext.getNextKeyMarker();
                    }

                    if (srcObjects.size() == 0 && destObjects.size() == 0) {
                        break;
                    }

                    List<Future<CompareResult>> compareResultList = new ArrayList<>();
                    if (srcObjects.size() == 0) {
                        while (destObjects.size() != 0) {
                            S3ImportObject destObj = destObjects.poll();
                            diffList.add(new CompareResult(destObj.getKey(), DELETED));
                            progress.success(DELETED);
                            lastDestKey = destObj.getKey();
                        }
                    }
                    else if (destObjects.size() == 0) {
                        while (srcObjects.size() != 0) {
                            S3ImportObject srcObj = srcObjects.poll();
                            diffList.add(new CompareResult(srcObj.getKey(), NEW));
                            progress.success(NEW);
                            lastSrcKey = srcObj.getKey();
                        }
                    }
                    else {
                        while (srcObjects.size() > 0 && destObjects.size() > 0) {
                            S3ImportObject srcObject = srcObjects.peek();
                            S3ImportObject destObject = destObjects.peek();
                            int compareRes = srcObject.getKey().compareTo(destObject.getKey());
                            if (compareRes > 0) {
                                diffList.add(new CompareResult(destObject.getKey(), DELETED));
                                progress.success(DELETED);
                                destObjects.poll();
                                lastDestKey = destObject.getKey();
                            }
                            else if (compareRes < 0) {
                                diffList.add(new CompareResult(srcObject.getKey(), NEW));
                                progress.success(NEW);
                                srcObjects.poll();
                                lastSrcKey = srcObject.getKey();
                            }
                            else {
                                Future<CompareResult> resultFuture = executor.submit(new CompareTask(
                                        srcS3Client, destS3Client, srcObject, destObject, s3Bucket,
                                        toolProps.isStrictComparisonMode(),
                                        toolProps.getIgnoreMetadataMap()));
                                compareResultList.add(resultFuture);
                                srcObjects.poll();
                                destObjects.poll();
                                lastSrcKey = srcObject.getKey();
                                lastDestKey = destObject.getKey();
                            }
                        }

                        boolean hasAbortTask = false;
                        for (Future<CompareResult> future : compareResultList) {
                            try {
                                CompareResult result = future.get();
                                if (!result.getDiffType().equals(SAME)) {
                                    diffList.add(result);
                                }
                                progress.success(result.getDiffType());
                            }
                            catch (Exception e) {
                                logger.error("Failed to get the comparison result", e);
                                hasAbortTask = true;
                            }
                        }
                        if (hasAbortTask) {
                            throw new ScmToolsException("Exist abnormal compare task",
                                    S3ImportExitCode.SYSTEM_ERROR);
                        }
                    }

                    FileOperateUtils.appendCompareResult(s3Bucket, compareResultPath, diffList);
                    progress.setSrcNextKeyMarker(lastSrcKey);
                    progress.setDestNextKeyMarker(lastDestKey);
                    FileOperateUtils.updateProgress(progressFilePath, bucketList);
                    CommonUtils.checkMaxExecTime(startTime, runStartTime,
                            importOptions.getMaxExecTime());
                }
                progress.setStatus(CommonDefine.ProgressStatus.FINISH);
                FileOperateUtils.updateProgress(progressFilePath, bucketList);
            }
        }
        finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }
}

class CompareTask implements Callable<CompareResult> {

    private static final Logger logger = LoggerFactory.getLogger(CompareTask.class);

    private AmazonS3Client srcClient;
    private AmazonS3Client destClient;
    private S3Bucket s3Bucket;
    private S3ImportObject srcImportObject;
    private S3ImportObject destImportObject;
    private boolean isStrictComparisonMode;
    private Map<String, IgnoreMetadata> ignoreMetadataMap;

    public CompareTask(AmazonS3Client srcClient, AmazonS3Client destClient,
            S3ImportObject srcImportObject, S3ImportObject destImportObject, S3Bucket s3Bucket,
            boolean isStrictComparisonMode, Map<String, IgnoreMetadata> ignoreMetadataMap) {
        this.srcClient = srcClient;
        this.destClient = destClient;
        this.srcImportObject = srcImportObject;
        this.destImportObject = destImportObject;
        this.s3Bucket = s3Bucket;
        this.isStrictComparisonMode = isStrictComparisonMode;
        this.ignoreMetadataMap = ignoreMetadataMap;
    }

    @Override
    public CompareResult call() throws Exception {
        if (s3Bucket.isEnableVersionControl()) {
            List<S3VersionSummary> srcSummaries = srcImportObject.getVersionSummaryList();
            List<S3VersionSummary> destSummaries = destImportObject.getVersionSummaryList();
            if (srcSummaries.size() != destSummaries.size()) {
                logger.info(
                        "Version number is inconsistent, key={}, bucket={}, versionCount={}, "
                                + "destBucket={}, versionCount={}",
                        srcImportObject.getKey(), s3Bucket.getName(), srcSummaries.size(),
                        s3Bucket.getDestName(), destSummaries.size());
                return new CompareResult(srcImportObject.getKey(), DIFF_VERSION);
            }

            for (int i = 0; i < srcSummaries.size(); i++) {
                S3VersionSummary srcSummary = srcSummaries.get(i);
                S3VersionSummary destSummary = destSummaries.get(i);
                if (srcSummary.isDeleteMarker() != destSummary.isDeleteMarker()) {
                    logger.info("DeleteMarker is inconsistent, key={}, bucket={}, versionId={}",
                            srcSummary.isDeleteMarker(), destSummary.isDeleteMarker(),
                            srcSummary.getVersionId());
                    return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
                }

                if (srcSummary.isDeleteMarker()) {
                    continue;
                }

                ObjectMetadata srcMetadata, destMetadata;
                CheckResult checkDataResult;
                if (isStrictComparisonMode) {
                    S3Object srcObject = null;
                    S3Object destObject = null;
                    try {
                        GetObjectRequest srcGetObjectRequest = new GetObjectRequest(
                                srcSummary.getBucketName(), srcSummary.getKey(),
                                srcSummary.getVersionId());
                        srcObject = S3Utils.getObject(srcClient, srcGetObjectRequest);

                        GetObjectRequest destGetObjectRequest = new GetObjectRequest(
                                destSummary.getBucketName(), destSummary.getKey(),
                                destSummary.getVersionId());
                        destObject = S3Utils.getObject(destClient, destGetObjectRequest);

                        checkDataResult = checkObjectContent(srcObject, destObject);

                        srcMetadata = srcObject.getObjectMetadata();
                        destMetadata = destObject.getObjectMetadata();
                    }
                    finally {
                        ScmCommon.closeResource(srcObject, destObject);
                    }
                }
                else {
                    checkDataResult = checkData(srcSummary.getETag(), destSummary.getETag(),
                            srcSummary.getLastModified(), destSummary.getLastModified());

                    srcMetadata = S3Utils.getObjectMetadata(srcClient,
                            new GetObjectMetadataRequest(s3Bucket.getName(),
                                    srcImportObject.getKey(), srcSummary.getVersionId()));
                    destMetadata = S3Utils.getObjectMetadata(destClient,
                            new GetObjectMetadataRequest(s3Bucket.getDestName(),
                                    destImportObject.getKey(), destSummary.getVersionId()));
                }

                CheckResult checkMetaResult = checkMetadata(srcMetadata, destMetadata);
                if (!checkMetaResult.isMatch()) {
                    logger.info(
                            "metadata of object are different, key={}, srcBucket={}, srcVersionId={}, destBucket={}, destVersionId={}, diffDetail: {}",
                            srcImportObject.getKey(), s3Bucket.getName(), srcSummary.getVersionId(),
                            s3Bucket.getDestName(), destSummary.getVersionId(),
                            checkMetaResult.getDiffDetail());
                    return new CompareResult(srcImportObject.getKey(), DIFF_METADATA);
                }
                if (!checkDataResult.isMatch()) {
                    logger.info(
                            "the contents of the object are different, key={}, srcBucket={}, srcVersionId={}, destBucket={}, destVersionId={}, diffDetail: {}",
                            srcImportObject.getKey(), s3Bucket.getName(), srcSummary.getVersionId(),
                            s3Bucket.getDestName(), destSummary.getVersionId(),
                            checkDataResult.getDiffDetail());
                    return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
                }
            }
        }
        else {
            ObjectMetadata srcMetadata, destMetadata;
            CheckResult checkDataResult;
            if (isStrictComparisonMode) {
                S3Object srcObject = null;
                S3Object destObject = null;
                try {
                    GetObjectRequest srcGetObjectRequest = new GetObjectRequest(
                            srcImportObject.getBucket(), srcImportObject.getKey());
                    srcObject = S3Utils.getObject(srcClient, srcGetObjectRequest);

                    GetObjectRequest destGetObjectRequest = new GetObjectRequest(
                            destImportObject.getBucket(), destImportObject.getKey());
                    destObject = S3Utils.getObject(destClient, destGetObjectRequest);

                    checkDataResult = checkObjectContent(srcObject, destObject);

                    srcMetadata = srcObject.getObjectMetadata();
                    destMetadata = destObject.getObjectMetadata();
                }
                finally {
                    ScmCommon.closeResource(srcObject, destObject);
                }
            }
            else {
                checkDataResult = checkData(srcImportObject.getETag(), destImportObject.getETag(),
                        srcImportObject.getLastModified(), destImportObject.getLastModified());

                srcMetadata = S3Utils.getObjectMetadata(srcClient,
                        new GetObjectMetadataRequest(s3Bucket.getName(), srcImportObject.getKey()));
                destMetadata = S3Utils.getObjectMetadata(destClient, new GetObjectMetadataRequest(
                        s3Bucket.getDestName(), destImportObject.getKey()));
            }

            CheckResult checkMetaResult = checkMetadata(srcMetadata, destMetadata);
            if (!checkMetaResult.isMatch()) {
                logger.info(
                        "metadata of object are different, key={}, srcBucket={}, destBucket={}, diffDetail: {}",
                        srcImportObject.getKey(), s3Bucket.getName(), s3Bucket.getDestName(),
                        checkMetaResult.getDiffDetail());
                return new CompareResult(srcImportObject.getKey(), DIFF_METADATA);
            }
            if (!checkDataResult.isMatch()) {
                logger.info(
                        "the contents of the object are different, key={}, srcBucket={}, destBucket={}, diffDetail: {}",
                        srcImportObject.getKey(), s3Bucket.getName(), s3Bucket.getDestName(),
                        checkDataResult.getDiffDetail());
                return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
            }
        }

        return new CompareResult(srcImportObject.getKey(), SAME);
    }

    private CheckResult checkObjectContent(S3Object srcObject, S3Object destObject)
            throws ScmToolsException {
        String srcObjectMd5 = Md5Utils.getMD5(srcObject.getObjectContent());
        String destObjectMd5 = Md5Utils.getMD5(destObject.getObjectContent());
        if (!srcObjectMd5.equals(destObjectMd5)) {
            return CheckResult
                    .diff("srcObjectMd5=" + srcObjectMd5 + ", destObjectMd5=" + destObjectMd5);
        }
        return CheckResult.match();
    }

    private CheckResult checkData(String srcETag, String destETag, Date srcModified,
            Date destModified) {
        // 快速比对数据：
        // ETag 有效比对 ETag
        // ETag 无效比对 Last-Modified（上传时本身已经做了数据完整性校验，且迁移失败时会清除目标桶上的对象）
        if (Md5Utils.isETagValid(srcETag)) {
            if (!srcETag.equals(destETag)) {
                return CheckResult.diff("srcETag=" + srcETag + ", destETag=" + destETag);
            }
        }
        else {
            if (!srcModified.equals(destModified)) {
                return CheckResult.diff(
                        "srcLastModified=" + srcModified + ", destLastModified=" + destModified);
            }
        }
        return CheckResult.match();
    }

    private CheckResult checkMetadata(ObjectMetadata srcMetadata, ObjectMetadata destMetadata) {
        // check userMetadata
        Map<String, String> srcUserMetadata = srcMetadata.getUserMetadata();
        Map<String, String> destUserMetadata = destMetadata.getUserMetadata();
        boolean isUserMetaMatch = true;
        if (srcUserMetadata.size() != destUserMetadata.size()) {
            isUserMetaMatch = false;
        }
        else {
            for (String key : srcUserMetadata.keySet()) {
                String destVal = destUserMetadata.get(key);
                if (destVal == null || !StringUtils.equals(destVal, srcUserMetadata.get(key))) {
                    isUserMetaMatch = false;
                    break;
                }
            }
        }
        if (!isUserMetaMatch) {
            return CheckResult.diff(
                    "srcUserMeta=" + srcUserMetadata + ", destUserMeta=" + destUserMetadata);
        }

        // check ObjectMeta
        try {
            if (Md5Utils.isETagValid(srcMetadata.getETag())
                    && Md5Utils.isETagValid(destMetadata.getETag())) {
                AssertEqual("ETag", srcMetadata.getETag(), destMetadata.getETag());
            }
            AssertEqual("Cache-Control", srcMetadata.getCacheControl(),
                    destMetadata.getCacheControl());
            AssertEqual("Content-Disposition", srcMetadata.getContentDisposition(),
                    destMetadata.getContentDisposition());
            AssertEqual("Content-Encoding", srcMetadata.getContentEncoding(),
                    destMetadata.getContentEncoding());
            AssertEqual("Content-Language", srcMetadata.getContentLanguage(),
                    destMetadata.getContentLanguage());
            AssertEqual("Content-MD5", srcMetadata.getContentMD5(), destMetadata.getContentMD5());
            AssertEqual("Content-Length", srcMetadata.getContentLength(),
                    destMetadata.getContentLength());
            AssertEqual("Content-Type", srcMetadata.getContentType(),
                    destMetadata.getContentType());
            AssertEqual("expirationTime", srcMetadata.getExpirationTime(),
                    destMetadata.getExpirationTime());
            AssertEqual("expirationTimeRuleId", srcMetadata.getExpirationTimeRuleId(),
                    destMetadata.getExpirationTimeRuleId());
            AssertEqual("httpExpiresDate", srcMetadata.getHttpExpiresDate(),
                    destMetadata.getHttpExpiresDate());
            AssertEqual("restoreExpirationTime", srcMetadata.getRestoreExpirationTime(),
                    destMetadata.getRestoreExpirationTime());
            AssertEqual("SSECustomerKeyMd5", srcMetadata.getSSECustomerKeyMd5(),
                    destMetadata.getSSECustomerKeyMd5());
            AssertEqual("SSEAlgorithm", srcMetadata.getSSEAlgorithm(),
                    destMetadata.getSSEAlgorithm());
            AssertEqual("SSECustomerAlgorithm", srcMetadata.getSSECustomerAlgorithm(),
                    destMetadata.getSSECustomerAlgorithm());
            AssertEqual("SSEAwsKmsKeyId", srcMetadata.getSSEAwsKmsKeyId(),
                    destMetadata.getSSEAwsKmsKeyId());
        }
        catch (IllegalStateException e) {
            return CheckResult.diff(e.getMessage());
        }

        return CheckResult.match();
    }

    private void AssertEqual(String fieldName, Object srcVal, Object destVal) {
        if (srcVal == null && destVal == null) {
            return;
        }
        if (ignoreMetadataMap.containsKey(fieldName)) {
            IgnoreMetadata ignoreMetadata = ignoreMetadataMap.get(fieldName);
            if (Objects.equals(srcVal, ignoreMetadata.getSrcValue())
                    && Objects.equals(destVal, ignoreMetadata.getDestValue())) {
                return;
            }
        }
        if (srcVal == null || !srcVal.equals(destVal)) {
            throw new IllegalStateException(
                    "fieldName=" + fieldName + ", srcValue=" + srcVal + ", destValue=" + destVal);
        }
    }
}

class CheckResult {

    private boolean isMatch;
    private String diffDetail;

    public CheckResult(boolean isMatch, String diffDetail) {
        this.isMatch = isMatch;
        this.diffDetail = diffDetail;
    }

    static CheckResult match() {
        return new CheckResult(true, null);
    }

    static CheckResult diff(String diffDetail) {
        return new CheckResult(false, diffDetail);
    }

    public boolean isMatch() {
        return isMatch;
    }

    public String getDiffDetail() {
        return diffDetail;
    }
}
