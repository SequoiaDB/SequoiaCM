package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.client.element.quota.ScmUpdateBucketQuotaConfig;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateQuotaToolImpl extends BaseQuotaToolImpl {
    private static final Logger logger = LoggerFactory.getLogger(UpdateQuotaToolImpl.class);

    private final String OPT_MAX_OBJECTS = "max-objects";
    private final String OPT_MAX_SIZE = "max-size";
    private final String OPT_MAX_SIZE_BYTES = "max-size-bytes";

    public UpdateQuotaToolImpl() throws ScmToolsException {
        super("update-quota");
        ops.addOption(hp.createOpt(null, OPT_MAX_OBJECTS,
                "sets the maximum object count limit for specified bucket. if the value is less than 0 or unspecified, it means no limit.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_MAX_SIZE,
                "sets the maximum size limit for the bucket. if the value is less than 0 or unspecified, it means no limit. example: 100G、100g、1000M、1000m、-1(it means no limit)",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_MAX_SIZE_BYTES,
                "sets the maximum size limit with bytes for the bucket. if the value is less than 0 or unspecified, it means no limit.",
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
            ScmUpdateBucketQuotaConfig.Builder builder = ScmUpdateBucketQuotaConfig
                    .newBuilder(bucket);
            builder.setMaxObjects(quotaParams.maxObjects);
            builder.setMaxSizeBytes(quotaParams.maxSize);
            ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.updateBucketQuota(session,
                    builder.build());
            System.out.println("update quota successfully.");
            printQuotaInfo(quotaInfo);

            logger.info("update quota successfully:bucket={},maxObjects={},maxSize={}", bucket,
                    quotaParams.maxObjects, quotaParams.maxSize);
        }
        catch (Exception e) {
            logger.error("update quota failed:bucket={},maxObjects={},maxSize={}", bucket,
                    quotaParams.maxObjects, quotaParams.maxSize, e);
            System.out.println("update quota failed:" + e.getMessage());
            throw new ScmToolsException("update quota failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private QuotaParams checkAndParseArgs(CommandLine cl) throws ScmToolsException {
        if (!cl.hasOption(OPT_MAX_SIZE_BYTES) && !cl.hasOption(OPT_MAX_SIZE)
                && !cl.hasOption(OPT_MAX_OBJECTS)) {
            throw new ScmToolsException("maxObjects or maxSize must be specified at least one",
                    ScmExitCode.INVALID_ARG);
        }
        if (cl.hasOption(OPT_MAX_SIZE_BYTES) && cl.hasOption(OPT_MAX_SIZE)) {
            throw new ScmToolsException("param: " + OPT_MAX_SIZE_BYTES + " and " + OPT_MAX_SIZE
                    + " can not be specified at the same time", ScmExitCode.INVALID_ARG);
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
        return quotaParams;
    }

    private static class QuotaParams {
        private long maxSize = -1;
        private long maxObjects = -1;
    }
}
