package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
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

    private final String OPT_USED_OBJECTS = "used-objects";
    private final String OPT_USED_SIZE = "used-size";
    private final String OPT_USED_SIZE_BYTES = "used-size-bytes";

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

        ops.addOption(hp.createOpt(null, OPT_USED_OBJECTS,
                "sets the used object count limit for the bucket. ", false, true, false));
        ops.addOption(hp.createOpt(null, OPT_USED_SIZE,
                "sets the used size for the bucket. example: 100G、100g、1000M、1000m", false, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_USED_SIZE_BYTES,
                "sets the used size with bytes for the bucket.", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        super.process(args);
        QuotaParams quotaParams = checkAndParseArgs(cl);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            ScmBucketQuotaInfo quotaInfo = null;

            if (quotaParams.maxObjects != null || quotaParams.maxSize != null) {
                ScmUpdateBucketQuotaConfig.Builder builder = ScmUpdateBucketQuotaConfig
                        .newBuilder(bucket);
                if (quotaParams.maxObjects != null) {
                    builder.setMaxObjects(quotaParams.maxObjects);
                }
                if (quotaParams.maxSize != null) {
                    builder.setMaxSizeBytes(quotaParams.maxSize);
                }
                quotaInfo = ScmFactory.Quota.updateBucketQuota(session, builder.build());
            }

            if (quotaParams.usedObjects != null || quotaParams.usedSize != null) {
                quotaInfo = ScmFactory.Quota.updateBucketUsedQuota(session, bucket,
                        quotaParams.usedObjects, quotaParams.usedSize);
            }

            if (quotaInfo == null) {
                // should never happen
                throw new IllegalArgumentException("quotaInfo is null");
            }

            System.out.println("update quota successfully.");
            printQuotaInfo(quotaInfo);

            logger.info(
                    "update quota successfully:bucket={},maxObjects={},maxSize={},usedObjects={},usedSize={}",
                    bucket, quotaParams.maxObjects, quotaParams.maxSize, quotaParams.usedObjects,
                    quotaParams.usedSize);
        }
        catch (Exception e) {
            logger.error(
                    "update quota failed:bucket={},maxObjects={},maxSize={},usedObjects={},usedSize={}",
                    bucket, quotaParams.maxObjects, quotaParams.maxSize, quotaParams.usedObjects,
                    quotaParams.usedSize, e);
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
        boolean missMaxQuotaArgs = !cl.hasOption(OPT_MAX_SIZE_BYTES) && !cl.hasOption(OPT_MAX_SIZE)
                && !cl.hasOption(OPT_MAX_OBJECTS);
        boolean missUsedQuotaArgs = !cl.hasOption(OPT_USED_SIZE)
                && !cl.hasOption(OPT_USED_SIZE_BYTES) && !cl.hasOption(OPT_USED_OBJECTS);

        if (missMaxQuotaArgs && missUsedQuotaArgs) {
            throw new ScmToolsException("param: " + OPT_MAX_SIZE_BYTES + " or " + OPT_MAX_SIZE
                    + " or " + OPT_MAX_OBJECTS + " or " + OPT_USED_SIZE + " or "
                    + OPT_USED_SIZE_BYTES + " or " + OPT_USED_OBJECTS + " must be specified",
                    ScmExitCode.INVALID_ARG);
        }

        if (cl.hasOption(OPT_MAX_SIZE_BYTES) && cl.hasOption(OPT_MAX_SIZE)) {
            throw new ScmToolsException("param: " + OPT_MAX_SIZE_BYTES + " and " + OPT_MAX_SIZE
                    + " can not be specified at the same time", ScmExitCode.INVALID_ARG);
        }
        if (cl.hasOption(OPT_USED_SIZE) && cl.hasOption(OPT_USED_SIZE_BYTES)) {
            throw new ScmToolsException("param: " + OPT_USED_SIZE + " and " + OPT_USED_SIZE_BYTES
                    + " can not be specified at same time", ScmExitCode.INVALID_ARG);
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
        private Long maxSize;
        private Long maxObjects;

        private Long usedSize;
        private Long usedObjects;
    }
}
