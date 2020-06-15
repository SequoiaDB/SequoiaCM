package com.sequoiacm.s3.tools.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
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

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.s3.tools.S3Admin;
import com.sequoiacm.s3.tools.common.ListLine;
import com.sequoiacm.s3.tools.common.ListTable;
import com.sequoiacm.s3.tools.common.RestErrorHandler;
import com.sequoiacm.s3.tools.common.ScmCommandUtil;
import com.sequoiacm.s3.tools.common.ScmCommonPrintor;
import com.sequoiacm.s3.tools.common.ScmHelpGenerator;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import com.sequoiacm.s3.tools.exception.ScmToolsException;

public class RefreshAccesskeyToolImpl implements ScmTool {
    private final String OPT_SHORT_TARGETUSER = "t";
    private final String OPT_LONG_TARGETUSER = "target-user";
    private final String OPT_LONG_USER = "user";
    private final String OPT_SHORT_USER = "u";
    private final String OPT_SHORT_PWD = "p";
    private final String OPT_LONG_PWD = "password";
    private final String OPT_SHORT_URL = "s";
    private final String OPT_LONG_URL = "s3-url";

    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(RefreshAccesskeyToolImpl.class.getName());

    public RefreshAccesskeyToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_TARGETUSER, OPT_LONG_TARGETUSER,
                "refresh the accesskey of specified username.", true, true, false));
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
        S3Admin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(OPT_SHORT_USER);
        String passwd = cl.getOptionValue(OPT_SHORT_PWD);
        String target = cl.getOptionValue(OPT_LONG_TARGETUSER);
        if (passwd == null) {
            System.out.print("password for " + user + ": ");
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
        String refreshUrl = "http://" + url + "/users?Action=RefreshAccessKey";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target-username", target);
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
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(refreshUrl, HttpMethod.POST, entity,
                    String.class);
            BSONObject ret = (BSONObject) JSON.parse(resp.getBody());
            ListTable t = new ListTable();
            ListLine l = new ListLine();
            l.addItem((String) ret.get("username"));
            l.addItem((String) ret.get("accesskey"));
            l.addItem((String) ret.get("secretkey"));
            t.addLine(l);
            List<String> headerList = new ArrayList<String>();
            headerList.add("username");
            headerList.add("accesskey");
            headerList.add("secretkey");
            ScmCommonPrintor.print(headerList, t);
        }
        catch (ResourceAccessException e) {
            logger.error("failed to connect to s3 server:{}", url, e);
            throw new ScmToolsException("failed to connect to s3 server:" + url,
                    ScmExitCode.IO_ERROR);
        }
        catch (RestClientException e) {
            logger.error("s3 server failed to do refresh", e);
            throw new ScmToolsException("s3 server failed to do refresh", ScmExitCode.SYSTEM_ERROR);
        }
        logger.info("refresh successfully:user={}", target);
    }
}
