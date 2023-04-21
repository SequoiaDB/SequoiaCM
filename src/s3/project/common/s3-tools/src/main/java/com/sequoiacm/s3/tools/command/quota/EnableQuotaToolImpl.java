package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnableQuotaToolImpl extends BaseQuotaToolImpl {
    private static final Logger logger = LoggerFactory.getLogger(EnableQuotaToolImpl.class);

    private final String OPT_MAX_OBJECTS = "max-objects";
    private final String OPT_MAX_SIZE = "max-size";
    private final String OPT_MAX_SIZE_BYTES = "max-size-bytes";
    private final String OPT_USED_SIZE = "used-size";
    private final String OPT_USED_SIZE_BYTES = "used-size-bytes";
    private final String OPT_USED_OBJECTS = "used-objects";

    public EnableQuotaToolImpl() throws ScmToolsException {
        super("enable-quota");
        ops.addOption(hp.createOpt(null, OPT_MAX_OBJECTS,
                "sets the maximum object count limit for specified bucket. if the value is less than 0 or unspecified, it means no limit.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_MAX_SIZE,
                "sets the maximum size limit for the bucket. if the value is less than 0 or unspecified, it means no limit. example: 100G、100g、1000M、1000m、-1(it means no limit)",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_MAX_SIZE_BYTES,
                "sets the maximum size limit with bytes for the bucket. if the value is less than 0 or unspecified, it means no limit.",
                false, true, false));

        ops.addOption(hp.createOpt(null, OPT_USED_OBJECTS,
                "sets the used objects for the bucket. This is optional. If not set, the used quota information will be automatically calculated.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_USED_SIZE,
                "sets the used size for the bucket. example: 100G、100g、1000M、0m. This is optional. If not set, the used quota information will be automatically calculated.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_USED_SIZE_BYTES,
                "sets the used size with bytes for the bucket. This is optional. If not set, the used quota information will be automatically calculated.",
                false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        super.process(args);
        QuotaParams quotaParams = checkAndParseArgs(cl);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            ScmEnableBucketQuotaConfig.Builder builder = ScmEnableBucketQuotaConfig
                    .newBuilder(bucket);
            builder.setMaxObjects(quotaParams.maxObjects);
            builder.setMaxSizeBytes(quotaParams.maxSize);
            if (quotaParams.usedObjects != null && quotaParams.usedSize != null) {
                builder.setUsedQuota(quotaParams.usedObjects, quotaParams.usedSize);
            }
            ScmBucketQuotaInfo scmBucketQuotaInfo = ScmFactory.Quota.enableBucketQuota(session,
                    builder.build());
            System.out.println("enable quota successfully.");
            printQuotaInfo(scmBucketQuotaInfo);

            logger.info("enable quota successfully:bucket={},maxObjects={},maxSize={}", bucket,
                    quotaParams.maxObjects, quotaParams.maxSize);
        }
        catch (Exception e) {
            logger.error("enable quota failed:bucket={},maxObjects={},maxSize={}", bucket,
                    quotaParams.maxObjects, quotaParams.maxSize, e);
            System.out.println("enable quota failed:" + e.getMessage());
            throw new ScmToolsException("enable quota failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private QuotaParams checkAndParseArgs(CommandLine cl) throws ScmToolsException {
        if (!cl.hasOption(OPT_MAX_SIZE) && !cl.hasOption(OPT_MAX_SIZE_BYTES)
                && !cl.hasOption(OPT_MAX_OBJECTS)) {
            throw new ScmToolsException("maxObjects or maxSize must be specified at least one",
                    ScmExitCode.INVALID_ARG);
        }
        if (cl.hasOption(OPT_MAX_SIZE) && cl.hasOption(OPT_MAX_SIZE_BYTES)) {
            throw new ScmToolsException("param: " + OPT_MAX_SIZE + " and " + OPT_MAX_SIZE_BYTES
                    + " can not be specified at same time", ScmExitCode.INVALID_ARG);
        }

        if (cl.hasOption(OPT_USED_SIZE) && cl.hasOption(OPT_USED_SIZE_BYTES)) {
            throw new ScmToolsException("param: " + OPT_USED_SIZE + " and " + OPT_USED_SIZE_BYTES
                    + " can not be specified at same time", ScmExitCode.INVALID_ARG);
        }

        boolean hasUsedSize = cl.hasOption(OPT_USED_SIZE) || cl.hasOption(OPT_USED_SIZE_BYTES);
        boolean hasUsedObjects = cl.hasOption(OPT_USED_OBJECTS);
        if ((hasUsedSize && !hasUsedObjects) || (!hasUsedSize && hasUsedObjects)) {
            throw new ScmToolsException("usedObjects and usedSize must be specified at same time",
                    ScmExitCode.INVALID_ARG);
        }

        QuotaParams quotaParams = new QuotaParams();
        if (cl.hasOption(OPT_MAX_SIZE)) {
            quotaParams.maxSize = ScmQuotaUtils.parseMaxSize(cl.getOptionValue(OPT_MAX_SIZE));
        }
        if (cl.hasOption(OPT_MAX_SIZE_BYTES)) {
            quotaParams.maxSize = Long.parseLong(cl.getOptionValue(OPT_MAX_SIZE_BYTES));
        }
        if (cl.hasOption(OPT_MAX_OBJECTS)) {
            quotaParams.maxObjects = Long.parseLong(cl.getOptionValue(OPT_MAX_OBJECTS));
        }
        if (cl.hasOption(OPT_USED_SIZE)) {
            quotaParams.usedSize = ScmQuotaUtils.convertToBytes(cl.getOptionValue(OPT_USED_SIZE));
        }
        if (cl.hasOption(OPT_USED_SIZE_BYTES)) {
            quotaParams.usedSize = Long.parseLong(cl.getOptionValue(OPT_USED_SIZE_BYTES));
        }
        if (cl.hasOption(OPT_USED_OBJECTS)) {
            quotaParams.usedObjects = Long.parseLong(cl.getOptionValue(OPT_USED_OBJECTS));
        }
        return quotaParams;

    }

    private static class QuotaParams {
        private long maxSize = -1;
        private long maxObjects = -1;
        private Long usedSize;
        private Long usedObjects;
    }

}
