package com.sequoiacm.s3import.common;

import com.sequoiacm.s3import.module.S3Bucket;
import com.sequoiacm.s3import.progress.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProgressPrinter extends Thread {

    private Logger logger = LoggerFactory.getLogger(ProgressPrinter.class);
    private static final long PRINT_PERIOD = 5000;
    private String type;
    private List<S3Bucket> bucketList;
    private boolean loop = true;

    public ProgressPrinter(String type, List<S3Bucket> bucketList) {
        this.type = type;
        this.bucketList = bucketList;
    }

    public void stopPrint() {
        this.loop = false;
    }

    @Override
    public void run() {
        try {
            List<S3Bucket> bucketList = new ArrayList<>(this.bucketList);
            int lastPrintLen = 0;
            while (loop) {
                // 输出退格，清空上一次动态打屏的结果
                for (int i = 0; i < lastPrintLen; i++) {
                    System.out.print("\b");
                }

                Iterator<S3Bucket> iterator = bucketList.iterator();
                while (iterator.hasNext()) {
                    S3Bucket s3Bucket = iterator.next();
                    Progress progress = s3Bucket.getProgress();
                    if (progress.getStatus().equals(CommonDefine.ProgressStatus.FINISH)) {
                        System.out.println(generateProgress(s3Bucket));
                        logger.info(
                                "Finished {} in the bucket, bucket={}, dest_bucket={}, total_count={}, success_count={}, failure_count={}",
                                this.type, s3Bucket.getName(), s3Bucket.getDestName(),
                                progress.getTotalCount(), progress.getSuccessCount(),
                                progress.getFailureCount());
                        lastPrintLen = 0;
                        iterator.remove();
                    }
                    else if (progress.getStatus().equals(CommonDefine.ProgressStatus.RUNNING)) {
                        String currentProgress = generateProgress(s3Bucket);
                        System.out.print(currentProgress);
                        lastPrintLen = currentProgress.length();
                    }
                }
                if (bucketList.size() == 0) {
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(PRINT_PERIOD);
            }
        }
        catch (Exception e) {
            logger.error("Failed to print {} progress", this.type, e);
        }
    }

    private String generateProgress(S3Bucket s3Bucket) {
        StringBuilder progress = new StringBuilder()
                .append("bucket:").append(s3Bucket.getName())
                .append(", des_bucket:").append(s3Bucket.getDestName())
                .append(", ").append(s3Bucket.getProgress().toString());
        return progress.toString();
    }
}
