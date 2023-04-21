package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.common.ScmCommandUtil;
import com.sequoiacm.s3.tools.common.ScmHelpGenerator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class BaseQuotaToolImpl extends ScmTool {

    private final String OPT_BUCKET = "bucket";
    private final String OPT_LONG_USER = "user";
    private final String OPT_SHORT_USER = "u";
    private final String OPT_SHORT_PWD = "p";
    private final String OPT_LONG_PWD = "password";
    private final String OPT_LONG_URL = "url";

    protected Options ops;
    protected ScmHelpGenerator hp;
    protected CommandLine cl;
    protected String user;
    protected String passwd;
    protected String url;
    protected String bucket;

    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public BaseQuotaToolImpl(String tooName) throws ScmToolsException {
        super(tooName);
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, OPT_BUCKET, "bucket name.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_USER, OPT_LONG_USER, "username for login.", true, true,
                false));
        ops.addOption(hp.createOpt(OPT_SHORT_PWD, OPT_LONG_PWD, "password for login.", true, true,
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_URL, "gateway url, example:localhost:8080", true,
                true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        cl = ScmCommandUtil.parseArgs(args, ops);
        user = cl.getOptionValue(OPT_SHORT_USER);
        passwd = cl.getOptionValue(OPT_SHORT_PWD);
        bucket = cl.getOptionValue(OPT_BUCKET);

        if (passwd == null) {
            System.out.print("password: ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }

        url = "localhost:8080";
        if (cl.hasOption(OPT_LONG_URL)) {
            url = cl.getOptionValue(OPT_LONG_URL);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

    protected void printQuotaInfo(ScmBucketQuotaInfo quotaInfo) {
        System.out.println("bucket: " + quotaInfo.getBucketName());
        System.out.println("enable: " + quotaInfo.isEnable());
        System.out.println("maxObjects: " + formatObjects(quotaInfo.getMaxObjects()));
        System.out.println("maxSize: " + formatSize(quotaInfo.getMaxSizeBytes()));

        if (quotaInfo.isEnable()) {
            System.out.println("usedObjects: "
                    + formatUsedObjects(quotaInfo.getUsedObjects(), quotaInfo.getMaxObjects()));
            System.out.println("usedSize: "
                    + formatUsedSize(quotaInfo.getUsedSizeBytes(), quotaInfo.getMaxSizeBytes()));
            System.out.println("lastUpdateTime: " + formatDate(quotaInfo.getLastUpdateTime()));
            ScmQuotaSyncStatus syncStatus = quotaInfo.getSyncStatus();
            System.out.println("syncStatus: " + syncStatus);
            if (syncStatus == ScmQuotaSyncStatus.SYNCING) {
                System.out.println(
                        "estimatedEffectiveTime: " + quotaInfo.getEstimatedEffectiveTime() + "ms");
            }

            if (syncStatus == ScmQuotaSyncStatus.FAILED) {
                System.out.println(
                        "last sync failed, please try again:errorMsg=" + quotaInfo.getErrorMsg());
            }
        }

    }

    protected String formatObjects(long objects) {
        if (objects < 0) {
            return "unlimited";
        }
        return String.valueOf(objects);
    }

    protected String formatUsedObjects(long usedObjects, long maxObjects) {
        StringBuilder res = new StringBuilder();
        res.append(usedObjects);
        if (maxObjects > 0 && usedObjects <= maxObjects) {
            res.append("(").append(String.format("%.2f", (usedObjects / (double) maxObjects) * 100))
                    .append("%)");
        }
        return res.toString();
    }

    protected String formatSize(long bytes) {
        if (bytes < 0) {
            return "unlimited";
        }
        return ScmQuotaUtils.formatSize(bytes);
    }

    protected String formatUsedSize(long usedSize, long maxSize) {
        StringBuilder res = new StringBuilder(formatSize(usedSize));
        if (maxSize > 0 && usedSize <= maxSize) {
            res.append("(").append(String.format("%.2f", (usedSize / (double) maxSize)))
                    .append("%)");
        }
        return res.toString();
    }

    protected String formatDate(Date date) {
        return df.format(date);
    }

}
