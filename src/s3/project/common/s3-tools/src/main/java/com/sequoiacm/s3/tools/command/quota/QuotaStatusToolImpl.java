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

    private final String OPT_SHOW_INNER_DETAIL = "show-inner-detail";
    private final String OPT_FORCE_REFRESH = "force-refresh";

    public QuotaStatusToolImpl() throws ScmToolsException {
        super("quota-status");
        ops.addOption(hp.createOpt(null, OPT_FORCE_REFRESH, "force refresh of quota info.", false,
                false, false));
        ops.addOption(hp.createOpt(null, OPT_SHOW_INNER_DETAIL,
                "show inner quota detail of the bucket. ", false, false, true));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        super.process(args);
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, passwd));
            boolean forceRefresh = cl.hasOption(OPT_FORCE_REFRESH);
            ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota(session, bucket,
                    forceRefresh);
            printQuotaInfo(quotaInfo);
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

}
