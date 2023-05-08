package com.sequoiacm.s3import.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import com.sequoiacm.s3import.common.*;
import com.sequoiacm.s3import.config.ImportPathConfig;
import com.sequoiacm.s3import.config.ImportToolProps;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.factory.S3ImportBatchFactory;
import com.sequoiacm.s3import.module.S3Bucket;
import com.sequoiacm.s3import.module.S3ImportBatch;
import com.sequoiacm.s3import.module.S3ImportOptions;
import com.sequoiacm.s3import.progress.Progress;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.*;

@Command
public class MigrateCommand extends SubCommand {

    public static final String NAME = "migrate";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "migrate s3 data";
    }

    @Override
    protected Options commandOptions() throws ScmToolsException {
        Options ops = super.commandOptions();
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.BUCKET).hasArg(true)
                .desc("buckets that need to migrate data").build());
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
    protected void checkAndInitBucketConf(S3ImportOptions importOptions) throws ScmToolsException {
        super.checkAndInitBucketConf(importOptions);
        List<S3Bucket> bucketList = importOptions.getBucketList();

        // 迁移进度文件不存在
        String progressFilePath = ImportPathConfig.getInstance().getMigrateProgressFilePath();
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
            s3Bucket.setProgress(checkS3Bucket.getProgress());
        }
    }

    @Override
    protected void process(S3ImportOptions importOptions) throws ScmToolsException {
        List<S3Bucket> bucketList = importOptions.getBucketList();
        String progressFilePath = ImportPathConfig.getInstance().getMigrateProgressFilePath();

        S3ImportBatchRunner batchRunner = null;
        try {
            batchRunner = new S3ImportBatchRunner();
            S3ImportBatchFactory batchFactory = S3ImportBatchFactory.getInstance();
            boolean isNeedOverwrite = true;

            for (S3Bucket s3Bucket : bucketList) {
                Progress progress = s3Bucket.getProgress();
                if (progress.getStatus().equals(CommonDefine.ProgressStatus.FINISH)) {
                    continue;
                }

                if (progress.getStatus().equals(CommonDefine.ProgressStatus.INIT)) {
                    progress.setStatus(CommonDefine.ProgressStatus.RUNNING);
                    FileOperateUtils.updateProgress(progressFilePath, bucketList);
                    isNeedOverwrite = false;
                }

                S3ImportBatch batch;
                while ((batch = batchFactory.getNextBatch(s3Bucket, isNeedOverwrite)) != null) {
                    batchRunner.runAndWaitBatchFinish(batch);
                    if (batch.hasAbortedTask()) {
                        throw new ScmToolsException(
                                "Incomplete migration objects exist and cannot be cleaned up, execution interrupt",
                                S3ImportExitCode.SYSTEM_ERROR);
                    }
                    // 持久化失败列表、更新迁移进度
                    FileOperateUtils.appendErrorKeyList(s3Bucket, batch.getErrorKeys());
                    FileOperateUtils.updateProgress(progressFilePath, bucketList);

                    // 最后一个批次执行完成，直接退出，不需要再做超时/失败数检测
                    if (batch.isLastBatch()) {
                        break;
                    }

                    // 校验执行时间，失败数
                    batchRunner.increaseFailCount(batch.getErrorKeys().size());
                    CommonUtils.checkMaxExecTime(batchRunner.getStartTime(),
                            batchRunner.getRunStartTime(), importOptions.getMaxExecTime());
                    CommonUtils.checkFailCount(batchRunner.getFailureCount(),
                            ImportToolProps.getInstance().getMaxFailCount());

                    isNeedOverwrite = false;
                }
                // 更新桶的迁移状态
                progress.setStatus(CommonDefine.ProgressStatus.FINISH);
                FileOperateUtils.updateProgress(progressFilePath, bucketList);
            }
        }
        finally {
            if (batchRunner != null) {
                batchRunner.stop();
            }
        }
    }
}
