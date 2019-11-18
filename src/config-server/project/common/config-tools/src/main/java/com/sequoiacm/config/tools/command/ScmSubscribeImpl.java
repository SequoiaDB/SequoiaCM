package com.sequoiacm.config.tools.command;

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
import org.springframework.web.client.RestTemplate;

import com.sequoiacm.config.tools.SchAdmin;
import com.sequoiacm.config.tools.common.ScmCommandUtil;
import com.sequoiacm.config.tools.common.ScmHelpGenerator;
import com.sequoiacm.config.tools.exception.ScmToolsException;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class ScmSubscribeImpl implements ScmTool {
    private final String OPT_SHORT_CONFIG = "c";
    private final String OPT_LONG_CONFIG = "config";
    private final String OPT_SHORT_SERVICE = "s";
    private final String OPT_LONG_SERVICE = "service";
    private String OPT_LONG_URL = "url";
    private String OPT_SHORT_URL = "u";

    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(ScmSubscribeImpl.class.getName());

    public ScmSubscribeImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(OPT_SHORT_CONFIG, OPT_LONG_CONFIG, "config name.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_SERVICE, OPT_LONG_SERVICE, "service name.", true, true,
                false));
        ops.addOption(hp.createOpt(OPT_SHORT_URL, OPT_LONG_URL, "config server url.", true,
                true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        SchAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);

        String configName = cl.getOptionValue(OPT_LONG_CONFIG);
        String serviceName = cl.getOptionValue(OPT_LONG_SERVICE);
        String configUrl = cl.getOptionValue(OPT_LONG_URL);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);
        String subscribeUrl = "http://" + configUrl + "/internal/v1/subscribe/" + configName;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(ScmRestArgDefine.SERVICE_NAME, serviceName);
        HttpEntity<MultiValueMap<String, String>> subscribeEntity = new HttpEntity<>(params,
                new HttpHeaders());
        restTemplate.exchange(subscribeUrl, HttpMethod.POST, subscribeEntity, String.class);
        System.out.println("subscribe successfully:configName=" +configName + ",serviceName=" + serviceName);
        logger.info("subscribe successfully:configName={},serviceName={}", configName, serviceName);
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

}
