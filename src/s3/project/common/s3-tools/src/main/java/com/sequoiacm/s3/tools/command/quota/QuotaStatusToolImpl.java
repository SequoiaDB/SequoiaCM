package com.sequoiacm.s3.tools.command.quota;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import com.sequoiacm.infrastructure.tool.common.RestErrorHandler;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.apache.commons.cli.CommandLine;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class QuotaStatusToolImpl extends BaseQuotaToolImpl {
    private static final Logger logger = LoggerFactory.getLogger(QuotaStatusToolImpl.class);

    private final String OPT_SET_USED_OBJECTS = "set-used-objects";
    private final String OPT_SET_USED_SIZE = "set-used-size";
    private final String OPT_SET_USED_SIZE_BYTES = "set-used-size-bytes";
    private final String OPT_SHOW_INNER_DETAIL = "show-inner-detail";

    public QuotaStatusToolImpl() throws ScmToolsException {
        super("quota-status");
        ops.addOption(hp.createOpt(null, OPT_SET_USED_OBJECTS,
                "sets the used object count limit for specified bucket. ", false, true, false));
        ops.addOption(hp.createOpt(null, OPT_SET_USED_SIZE,
                "sets the used size for the bucket. example: 100G、100g、1000M、1000m", false, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_SET_USED_SIZE_BYTES,
                "sets the used size with bytes for the bucket.", false, true, false));
        ops.addOption(hp.createOpt(null, OPT_SHOW_INNER_DETAIL,
                "show inner quota detail of the bucket. ", false, false, true));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        super.process(args);
        QuotaParams quotaParams = checkAndParseArgs(cl);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            if (quotaParams.usedObjects != null || quotaParams.usedSize != null) {
                ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.updateBucketUsedQuota(session,
                        bucket, quotaParams.usedObjects, quotaParams.usedSize);
                System.out.println("update quota info success.");
                printQuotaInfo(quotaInfo);
            }
            else {
                ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota(session, bucket);
                printQuotaInfo(quotaInfo);
            }
            if (cl.hasOption(OPT_SHOW_INNER_DETAIL)) {
                BSONObject innerQuotaInfo = getInnerQuotaInfo(session.getSessionId(), bucket);
                System.out.println("inner quota info:");
                System.out.println(innerQuotaInfo);
            }
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("get quota info failed:bucket={}", bucket, e);
            System.out.println("get quota info failed:" + e.getMessage());
            throw new ScmToolsException("get quota info failed", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private QuotaParams checkAndParseArgs(CommandLine cl) throws ScmToolsException {
        if (cl.hasOption(OPT_SET_USED_SIZE) && cl.hasOption(OPT_SET_USED_SIZE_BYTES)) {
            throw new ScmToolsException("param: " + OPT_SET_USED_SIZE + " and "
                    + OPT_SET_USED_SIZE_BYTES + " can not be specified at same time",
                    ScmExitCode.INVALID_ARG);
        }
        QuotaParams quotaParams = new QuotaParams();
        if (cl.hasOption(OPT_SET_USED_SIZE)) {
            quotaParams.usedSize = ScmQuotaUtils
                    .convertToBytes(cl.getOptionValue(OPT_SET_USED_SIZE));
        }
        if (cl.hasOption(OPT_SET_USED_SIZE_BYTES)) {
            quotaParams.usedSize = Long.parseLong(cl.getOptionValue(OPT_SET_USED_SIZE_BYTES));
        }
        if (cl.hasOption(OPT_SET_USED_OBJECTS)) {
            quotaParams.usedObjects = Long.parseLong(cl.getOptionValue(OPT_SET_USED_OBJECTS));
        }
        return quotaParams;
    }

    private BSONObject getInnerQuotaInfo(String sessionId, String bucketName)
            throws ScmToolsException {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new RestErrorHandler());
        String targetUrl = "http://" + url + "/admin-server/api/v1/quotas/bucket/" + bucketName
                + "?action=" + CommonDefine.RestArg.QUOTA_ACTION_GET_QUOTA_INNER_DETAIL;
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-auth-token", sessionId);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(targetUrl, HttpMethod.GET, entity,
                    String.class);
            return (BSONObject) JSON.parse(resp.getBody());
        }
        catch (ResourceAccessException e) {
            throw new ScmToolsException("failed to connect to url:" + url,
                    ScmExitCode.SYSTEM_ERROR);
        }
        catch (RestClientException e) {
            logger.error("failed to get quota inner info:bucket={}", bucketName, e);
            throw new ScmToolsException("failed to quota quota inner info",
                    ScmExitCode.SYSTEM_ERROR);
        }
    }

    private static class QuotaParams {
        private Long usedSize;
        private Long usedObjects;
    }
}
