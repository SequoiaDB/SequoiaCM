package com.sequoiacm.s3import.command;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.*;
import com.sequoiacm.s3import.config.ImportPathConfig;
import com.sequoiacm.s3import.config.ImportToolProps;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.factory.S3ImportBatchFactory;
import com.sequoiacm.s3import.fileoperation.S3ImportFileResource;
import com.sequoiacm.s3import.fileoperation.S3ImportResourceFactory;
import com.sequoiacm.s3import.module.S3Bucket;
import com.sequoiacm.s3import.module.S3ImportBatch;
import com.sequoiacm.s3import.module.S3ImportOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.*;

@Command
public class RetryCommand extends SubCommand {

    public static final String NAME = "retry";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "Re-migrate objects in the bucket that failed to migrate";
    }

    @Override
    protected Options commandOptions() throws ScmToolsException {
        Options ops = super.commandOptions();
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.BUCKET).hasArg(true)
                .desc("buckets that need to retry migration").build());
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
    protected void checkAndInitBucketConf(List<S3Bucket> bucketList)
            throws ScmToolsException {
        super.checkAndInitBucketConf(bucketList);
        ImportPathConfig pathConfig = ImportPathConfig.getInstance();
        for (S3Bucket s3Bucket : bucketList) {
            File errorKeyFile = new File(pathConfig.getErrorKeyFilePath(s3Bucket));
            Queue<String> errorKeyList = new LinkedList<>();
            if (errorKeyFile.exists() && errorKeyFile.isFile()) {
                S3ImportFileResource fileResource = S3ImportResourceFactory.getInstance()
                        .createFileResource(errorKeyFile);
                String key;
                while ((key = fileResource.readLine()) != null) {
                    errorKeyList.add(key);
                }
            }
            s3Bucket.setErrorKeyList(errorKeyList);
            s3Bucket.getProgress().setTotalCount(errorKeyList.size());
        }
    }

    @Override
    protected void process(S3ImportOptions importOptions) throws ScmToolsException {
        List<S3Bucket> bucketList = importOptions.getBucketList();

        S3ImportBatchRunner batchRunner = null;
        try {
            batchRunner = new S3ImportBatchRunner();
            S3ImportBatchFactory batchFactory = S3ImportBatchFactory.getInstance();

            for (S3Bucket s3Bucket : bucketList) {
                s3Bucket.getProgress().setStatus(CommonDefine.ProgressStatus.RUNNING);
                List<String> tmpErrorKeys = new ArrayList<>();
                S3ImportBatch batch;
                while ((batch = batchFactory.getNextBatchByErrorKeyList(s3Bucket)) != null) {
                    batchRunner.runAndWaitBatchFinish(batch);
                    if (batch.hasAbortedTask()) {
                        throw new ScmToolsException("Exist abnormal retry task",
                                S3ImportExitCode.SYSTEM_ERROR);
                    }
                    batchRunner.increaseFailCount(batch.getErrorKeys().size());
                    tmpErrorKeys.addAll(batch.getErrorKeys());
                    // 持久化失败列表
                    Queue<String> totalErrorKeys = new LinkedList<>();
                    totalErrorKeys.addAll(s3Bucket.getErrorKeyList());
                    totalErrorKeys.addAll(tmpErrorKeys);
                    FileOperateUtils.overwriteErrorKeyList(s3Bucket, totalErrorKeys);
                    // 校验执行时间、失败数
                    CommonUtils.checkMaxExecTime(batchRunner.getStartTime(),
                            batchRunner.getRunStartTime(), importOptions.getMaxExecTime());
                    CommonUtils.checkFailCount(batchRunner.getFailureCount(),
                            ImportToolProps.getInstance().getMaxFailCount());
                }
                s3Bucket.getProgress().setStatus(CommonDefine.ProgressStatus.FINISH);
            }
        }
        finally {
            if (batchRunner != null) {
                batchRunner.stop();
            }
        }
    }
}
