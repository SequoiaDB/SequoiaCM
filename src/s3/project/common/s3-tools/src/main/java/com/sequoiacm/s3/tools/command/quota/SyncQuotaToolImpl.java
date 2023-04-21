package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncQuotaToolImpl extends BaseQuotaToolImpl {
    private static final Logger logger = LoggerFactory.getLogger(SyncQuotaToolImpl.class);

    public SyncQuotaToolImpl() throws ScmToolsException {
        super("sync-quota");
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        super.process(args);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            ScmFactory.Quota.syncBucketQuota(session, bucket);
            System.out.println("synchronizing in the background:bucket=" + bucket);
            logger.info("synchronizing in the background:bucket={}", bucket);

            // 等待异步统计一段时间，计算出预估预估速度后，打印最新的配额信息
            Thread.sleep(2000);
            ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota(session, bucket);
            printQuotaInfo(quotaInfo);
        }
        catch (Exception e) {
            logger.error("sync quota failed:bucket={}", bucket, e);
            System.out.println("sync quota failed:" + e.getMessage());
            throw new ScmToolsException("sync quota failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
