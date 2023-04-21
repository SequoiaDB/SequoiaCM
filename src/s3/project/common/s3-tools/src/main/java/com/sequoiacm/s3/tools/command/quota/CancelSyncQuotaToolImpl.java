package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelSyncQuotaToolImpl extends BaseQuotaToolImpl {
    private static final Logger logger = LoggerFactory.getLogger(CancelSyncQuotaToolImpl.class);

    public CancelSyncQuotaToolImpl() throws ScmToolsException {
        super("cancel-sync-quota");
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        super.process(args);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            ScmFactory.Quota.cancelSyncBucketQuota(session, bucket);
            System.out.println("cancel sync quota successfully:bucket=" + bucket);
            logger.info("cancel sync quota successfully:bucket={}", bucket);
        }
        catch (Exception e) {
            logger.error("cancel sync quota failed:bucket={}", bucket, e);
            System.out.println("cancel sync quota failed:" + e.getMessage());
            throw new ScmToolsException("cancel sync quota failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
