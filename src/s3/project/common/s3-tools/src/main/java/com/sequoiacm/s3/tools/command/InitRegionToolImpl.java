package com.sequoiacm.s3.tools.command;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.RestErrorHandler;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.common.ScmCommandUtil;
import com.sequoiacm.s3.tools.common.ScmHelpGenerator;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;


public class InitRegionToolImpl extends ScmTool {
    private final String OPT_SHORT_REGION = "r";
    private final String OPT_LONG_REGION = "region";
    private final String OPT_LONG_USER = "user";
    private final String OPT_SHORT_USER = "u";
    private final String OPT_SHORT_PWD = "p";
    private final String OPT_LONG_PWD = "password";
    private final String OPT_SHORT_URL = "s";
    private final String OPT_LONG_URL = "s3-url";

    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(InitRegionToolImpl.class.getName());

    public InitRegionToolImpl() throws ScmToolsException {
        super("init-region");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_REGION, OPT_LONG_REGION,
                "region name (workspace name).", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_USER, OPT_LONG_USER, "username for login.", true, true,
                false));
        ops.addOption(hp.createOpt(OPT_SHORT_PWD, OPT_LONG_PWD, "password for login.", true, true,
                false, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_URL, OPT_LONG_URL,
                "s3 server url, default:localhost:8002", false, true, false));

    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(OPT_SHORT_USER);
        String passwd = cl.getOptionValue(OPT_SHORT_PWD);
        String region = cl.getOptionValue(OPT_SHORT_REGION);
        if (passwd == null) {
            System.out.print("password: ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }
        String url = "localhost:8002";
        if (cl.hasOption(OPT_SHORT_URL)) {
            url = cl.getOptionValue(OPT_SHORT_URL);
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new RestErrorHandler());
        String subscribeUrl = "http://" + url + "/region?Action=InitRegion";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("RegionName", region);
        params.add("username", user);
        try {
            passwd = ScmPasswordMgr.getInstance().encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES,
                    passwd);
        }
        catch (Exception e1) {
            logger.error("failed to encode password", e1);
            throw new ScmToolsException("failed to encode password", ScmExitCode.SYSTEM_ERROR, e1);
        }

        params.add("password", passwd);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params,
                new HttpHeaders());
        try {
            restTemplate.exchange(subscribeUrl, HttpMethod.POST, entity, String.class);
        }
        catch (ResourceAccessException e) {
            logger.error("failed to connect to s3 server:{}", url, e);
            throw new ScmToolsException("failed to connect to s3 server:" + url,
                    ScmExitCode.IO_ERROR);
        }
        catch (RestClientException e) {
            logger.error("s3 server failed to do init", e);
            throw new ScmToolsException("s3 server failed to do init", ScmExitCode.SYSTEM_ERROR);
        }
        System.out.println("init successfully:region=" + region);
        logger.info("init successfully:region={}", region);
    }
}
