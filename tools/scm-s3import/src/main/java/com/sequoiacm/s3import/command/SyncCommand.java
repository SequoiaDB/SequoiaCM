package com.sequoiacm.s3import.command;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.*;
import com.sequoiacm.s3import.config.ImportToolProps;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.factory.S3ImportBatchFactory;
import com.sequoiacm.s3import.module.CompareResult;
import com.sequoiacm.s3import.module.S3Bucket;
import com.sequoiacm.s3import.module.S3ImportBatch;
import com.sequoiacm.s3import.module.S3ImportOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

@Command
public class SyncCommand extends SubCommand {

    private static final Logger logger = LoggerFactory.getLogger(SyncCommand.class);
    public static final String NAME = "sync";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "repair the data difference in the bucket";
    }

    @Override
    protected Options commandOptions() throws ScmToolsException {
        Options ops = super.commandOptions();
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.CMP_RES_PATH).hasArg(true)
                .desc("the result path of data comparison").build());
        return ops;
    }

    @Override
    protected S3ImportOptions parseCommandLineArgs(CommandLine cl) throws ScmToolsException {
        ArgUtils.checkRequiredOption(cl, CommonDefine.Option.CMP_RES_PATH);

        S3ImportOptions options = super.parseCommandLineArgs(cl);
        String compareResultDir = cl.getOptionValue(CommonDefine.Option.CMP_RES_PATH);
        options.setCompareResultPath(compareResultDir);
        options.setBucketList(generateBucketList(compareResultDir));
        return options;
    }

    @Override
    protected void process(S3ImportOptions importOptions) throws ScmToolsException {
        List<S3Bucket> bucketList = importOptions.getBucketList();
        String compareResultPath = importOptions.getCompareResultPath();
        String syncErrorPath = compareResultPath + CommonDefine.PathConstant.SYNC_ERROR;
        String syncFinishPath = compareResultPath + CommonDefine.PathConstant.SYNC_FINISH;

        S3ImportBatchRunner batchRunner = null;
        try {
            batchRunner = new S3ImportBatchRunner();
            S3ImportBatchFactory batchFactory = S3ImportBatchFactory.getInstance();
            for (S3Bucket s3Bucket : bucketList) {
                s3Bucket.getProgress().setStatus(CommonDefine.ProgressStatus.RUNNING);
                Queue<CompareResult> errorSyncKeys = new LinkedList<>();
                S3ImportBatch batch;
                while ((batch = batchFactory.getNextBatchByCmpResult(s3Bucket)) != null) {
                    batchRunner.runAndWaitBatchFinish(batch);
                    if (batch.hasAbortedTask()) {
                        throw new ScmToolsException("Exist abnormal sync task",
                                S3ImportExitCode.SYSTEM_ERROR);
                    }
                    errorSyncKeys.addAll(batch.getErrorSyncKeys());
                    FileOperateUtils.appendCompareResult(s3Bucket, syncErrorPath, errorSyncKeys);

                    // 最后一个批次执行完成，直接退出，不需要再做超时/失败数检测
                    if (batch.isLastBatch()) {
                        break;
                    }

                    // 校验执行时间、失败数
                    batchRunner.increaseFailCount(batch.getErrorSyncKeys().size());
                    CommonUtils.checkMaxExecTime(batchRunner.getStartTime(),
                            batchRunner.getRunStartTime(), importOptions.getMaxExecTime());
                    CommonUtils.checkFailCount(batchRunner.getFailureCount(),
                            ImportToolProps.getInstance().getMaxFailCount());
                }
                s3Bucket.releaseResultFileResource();
                s3Bucket.getProgress().setStatus(CommonDefine.ProgressStatus.FINISH);
                FileOperateUtils.moveFile(s3Bucket.getCompareResultFilePath(), syncFinishPath);
            }

            if (new File(syncErrorPath).exists()) {
                System.out.println("Sync fail path:" + syncErrorPath
                        + ", please specify this path to sync again");
                logger.warn("Sync fail path:{}, please specify this path to sync again",
                        syncErrorPath);
            }
        }
        finally {
            if (batchRunner != null) {
                batchRunner.stop();
            }
        }
    }

    private List<S3Bucket> generateBucketList(String compareResultDir) throws ScmToolsException {
        File resultDir = new File(compareResultDir);
        if (!resultDir.exists()) {
            throw new ScmToolsException("Directory does not exist", S3ImportExitCode.FILE_NOT_FIND);
        }
        if (!resultDir.isDirectory()) {
            throw new ScmToolsException("The comparison result requires a directory",
                    S3ImportExitCode.INVALID_ARG);
        }

        List<S3Bucket> res = new ArrayList<>();
        for (File resultFile : resultDir.listFiles()) {
            if (resultFile.isDirectory()) {
                continue;
            }
            String fileName = resultFile.getName();
            String[] split = fileName.split(CommonDefine.Separator.BUCKET_DIFF);
            if (split.length != 2) {
                throw new ScmToolsException("Illegal file name, file name:" + fileName,
                        S3ImportExitCode.INVALID_ARG);
            }
            S3Bucket s3Bucket = new S3Bucket(split[0], split[1], NAME);
            s3Bucket.setCompareResultFilePath(resultFile.getAbsolutePath());
            res.add(s3Bucket);
        }
        return res;
    }
}
