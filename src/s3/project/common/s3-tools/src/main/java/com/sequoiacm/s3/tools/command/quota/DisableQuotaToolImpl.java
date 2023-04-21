package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisableQuotaToolImpl extends BaseQuotaToolImpl {
    private static final Logger logger = LoggerFactory.getLogger(DisableQuotaToolImpl.class);

    public DisableQuotaToolImpl() throws ScmToolsException {
        super("disable-quota");
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        super.process(args);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            ScmFactory.Quota.disableBucketQuota(session, bucket);
            System.out.println("disable quota successfully:bucket=" + bucket);
            logger.info("disable quota successfully:bucket={}", bucket);
        }
        catch (Exception e) {
            logger.error("disable quota failed:bucket={}", bucket, e);
            System.out.println("disable quota failed:" + e.getMessage());
            throw new ScmToolsException("disable quota failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
