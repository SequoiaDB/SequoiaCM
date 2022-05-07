package com.sequoiacm.s3import.common;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.module.S3Bucket;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class ArgUtils {

    public static List<S3Bucket> parseS3Bucket(String bucketStr, String commandType)
            throws ScmToolsException {
        String[] bucketList = bucketStr.split(CommonDefine.Separator.BUCKET);
        List<S3Bucket> res = new ArrayList<>();
        for (String bucket : bucketList) {
            String[] split = bucket.split(CommonDefine.Separator.BUCKET_DIFF_ARG);
            CommonUtils.assertTrue(split.length <= 2,
                    "The bucket name is not in the correct format");
            String srcBucketName = split[0];
            String destBucketName = split.length == 2 ? split[1] : srcBucketName;
            res.add(new S3Bucket(srcBucketName, destBucketName, commandType));
        }
        return res;
    }

    public static void checkRequiredOption(CommandLine cl, String... requiredOptions)
            throws ScmToolsException {
        for (String optionName : requiredOptions) {
            CommonUtils.assertTrue(cl.hasOption(optionName),
                    "missing required option: " + optionName);
        }
    }
}
