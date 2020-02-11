package com.sequoiacm.config.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sequoiacm.config.tools.ConfAdmin;
import com.sequoiacm.config.tools.common.RestErrorHandler;
import com.sequoiacm.config.tools.common.ScmCommandUtil;
import com.sequoiacm.config.tools.common.ScmHelpGenerator;
import com.sequoiacm.config.tools.common.SubscribersPrinter;
import com.sequoiacm.config.tools.exception.ScmExitCode;
import com.sequoiacm.config.tools.exception.ScmToolsException;

public class ScmListSubscribersImpl implements ScmTool {
    private String OPT_LONG_URL = "url";
    private String OPT_SHORT_URL = "u";

    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(ScmSubscribeImpl.class.getName());

    public ScmListSubscribersImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(OPT_SHORT_URL, OPT_LONG_URL, "config server url.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ConfAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String configUrl = cl.getOptionValue(OPT_LONG_URL);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new RestErrorHandler());
        String subscribeUrl = "http://" + configUrl + "/internal/v1/subscribe";
        ResponseEntity<String> resp;
        try {
            resp = restTemplate.getForEntity(subscribeUrl, String.class);
        }
        catch (ResourceAccessException e) {
            logger.error("failed to connect to config server:{}", configUrl, e);
            throw new ScmToolsException("failed to connect to config server:" + configUrl,
                    ScmExitCode.IO_ERROR);
        }
        catch (RestClientException e) {
            logger.error("failed to get subscribes from config server", e);
            throw new ScmToolsException("failed to get subscribes from config server",
                    ScmExitCode.SYSTEM_ERROR);
        }
        BasicBSONList respObj = (BasicBSONList) JSON.parse(resp.getBody());

        SubscribersPrinter printer = new SubscribersPrinter(respObj);
        printer.print();
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

}
