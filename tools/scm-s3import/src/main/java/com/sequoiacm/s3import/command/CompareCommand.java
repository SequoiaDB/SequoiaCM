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
        return ops;
    }

    @Override
    protected S3ImportOptions parseCommandLineArgs(CommandLine cl) throws ScmToolsException {
        ArgUtils.checkRequiredOption(cl, CommonDefine.Option.BUCKET);

        S3ImportOptions options = super.parseCommandLineArgs(cl);
        String bucketStr = cl.getOptionValue(CommonDefine.Option.BUCKET);
        options.setBucketList(ArgUtils.parseS3Bucket(bucketStr, NAME));
        return options;
    }

    @Override
    protected void checkAndInitBucketConf(List<S3Bucket> bucketList) throws ScmToolsException {
        super.checkAndInitBucketConf(bucketList);

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
                    "Is inconsistent with the bucket list of the last compare, bucketList="
                            + CommonUtils.bucketListToStr(bucketList) + ", lastBucketList="
                            + CommonUtils.bucketListToStr(checkS3BucketList),
                    S3ImportExitCode.INVALID_ARG);
        }
        Collections.sort(bucketList);
        Collections.sort(checkS3BucketList);
        for (int i = 0; i < bucketList.size(); i++) {
            S3Bucket s3Bucket = bucketList.get(i);
            S3Bucket checkS3Bucket = checkS3BucketList.get(i);
            if (!s3Bucket.equals(checkS3Bucket)) {
                throw new ScmToolsException(
                        "The bucket has not been compare or the target bucket name is inconsistent, bucket="
                                + s3Bucket.getName() + ", dest_bucket=" + s3Bucket.getDestName(),
                        S3ImportExitCode.INVALID_ARG);
            }
            s3Bucket.setProgress(checkS3Bucket.getProgress());
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
                                        toolProps.isStrictComparisonMode()));
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

    public CompareTask(AmazonS3Client srcClient, AmazonS3Client destClient,
            S3ImportObject srcImportObject, S3ImportObject destImportObject, S3Bucket s3Bucket,
            boolean isStrictComparisonMode) {
        this.srcClient = srcClient;
        this.destClient = destClient;
        this.srcImportObject = srcImportObject;
        this.destImportObject = destImportObject;
        this.s3Bucket = s3Bucket;
        this.isStrictComparisonMode = isStrictComparisonMode;
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
                return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
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

                        if (!checkObjectContent(srcObject, destObject)) {
                            return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
                        }

                        if (!checkMetadata(srcObject.getObjectMetadata(),
                                destObject.getObjectMetadata())) {
                            return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
                        }
                    }
                    finally {
                        ScmCommon.closeResource(srcObject, destObject);
                    }
                }
                else {
                    if (!checkData(srcSummary.getETag(), destSummary.getETag(),
                            srcSummary.getLastModified(), destSummary.getLastModified())) {
                        return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
                    }

                    ObjectMetadata srcObjectMetadata = S3Utils.getObjectMetadata(srcClient,
                            new GetObjectMetadataRequest(s3Bucket.getName(),
                                    srcImportObject.getKey(), srcSummary.getVersionId()));
                    ObjectMetadata destObjectMetadata = S3Utils.getObjectMetadata(destClient,
                            new GetObjectMetadataRequest(s3Bucket.getDestName(),
                                    destImportObject.getKey(), destSummary.getVersionId()));
                    if (!checkMetadata(srcObjectMetadata, destObjectMetadata)) {
                        return new CompareResult(srcImportObject.getKey(), DIFF_METADATA);
                    }
                }
            }
        }
        else {
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

                    if (!checkObjectContent(srcObject, destObject)) {
                        return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
                    }

                    if (!checkMetadata(srcObject.getObjectMetadata(),
                            destObject.getObjectMetadata())) {
                        return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
                    }
                }
                finally {
                    ScmCommon.closeResource(srcObject, destObject);
                }
            }

            if (!checkData(srcImportObject.getETag(), destImportObject.getETag(),
                    srcImportObject.getLastModified(), destImportObject.getLastModified())) {
                return new CompareResult(srcImportObject.getKey(), DIFF_CONTENT);
            }

            ObjectMetadata srcObjectMetadata = S3Utils.getObjectMetadata(srcClient,
                    new GetObjectMetadataRequest(s3Bucket.getName(), srcImportObject.getKey()));
            ObjectMetadata destObjectMetadata = S3Utils.getObjectMetadata(destClient,
                    new GetObjectMetadataRequest(s3Bucket.getDestName(),
                            destImportObject.getKey()));
            if (!checkMetadata(srcObjectMetadata, destObjectMetadata)) {
                return new CompareResult(srcImportObject.getKey(), DIFF_METADATA);
            }
        }

        return new CompareResult(srcImportObject.getKey(), SAME);
    }

    private boolean checkObjectContent(S3Object srcObject, S3Object destObject)
            throws ScmToolsException {
        String srcObjectMd5 = Md5Utils.getMD5(srcObject.getObjectContent());
        String destObjectMd5 = Md5Utils.getMD5(destObject.getObjectContent());
        return srcObjectMd5.equals(destObjectMd5);
    }

    private boolean checkData(String srcETag, String destETag, Date srcModified,
            Date destModified) {
        return Md5Utils.isETagValid(srcETag) ? srcETag.equals(destETag)
                : srcModified.equals(destModified);
    }

    private boolean checkMetadata(ObjectMetadata srcMetadata, ObjectMetadata destMetadata) {
        Map<String, String> srcUserMetadata = srcMetadata.getUserMetadata();
        Map<String, String> destUserMetadata = destMetadata.getUserMetadata();
        if (srcUserMetadata.size() != destUserMetadata.size()) {
            return false;
        }
        for (String key : srcUserMetadata.keySet()) {
            String destVal = destUserMetadata.get(key);
            if (destVal == null || StringUtils.equals(destVal, srcUserMetadata.get(key))) {
                return false;
            }
        }
        return true;
    }
}
