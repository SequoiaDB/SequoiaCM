package com.sequoiacm.cloud.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.sequoiacm.cloud.tools.ScmAdmin;
import com.sequoiacm.cloud.tools.common.ScmCommandUtil;
import com.sequoiacm.cloud.tools.common.ScmHelpGenerator;
import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.cloud.tools.exception.ScmToolsException;

public class ScmCreateUserToolmpl implements ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmCreateUserToolmpl.class);

    private final String SHORT_OP_USER = "u";
    private final String LONG_OP_USER = "username";
    private final String SHORT_OP_PASSWD = "p";
    private final String LONG_OP_PASSWD = "password";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_AUTH_URL = "auth-url";
    private final String LONG_OP_ADMIN_USER = "admin-user";
    private final String LONG_OP_ADMIN_PASSWD = "admin-password";

    private String adminUser = "admin";
    private String adminPasswd = "admin";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmCreateUserToolmpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(SHORT_OP_USER, LONG_OP_USER, "the name of new user.", true, true,
                false));
        ops.addOption(hp.createOpt(SHORT_OP_PASSWD, LONG_OP_PASSWD, "the password of new user.",
                true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL, "gateway url.", false, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_AUTH_URL, "auth server url.", false, true, true));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_USER, "admin username, default:admin.",
                false, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_PASSWD, "admin password, default:admin.",
                false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(SHORT_OP_USER);
        String passwd = cl.getOptionValue(SHORT_OP_PASSWD);

        String authAddr;
        if(cl.hasOption(LONG_OP_URL)) {
            authAddr = cl.getOptionValue(LONG_OP_URL) + "/auth";
        }else if(cl.hasOption(LONG_OP_AUTH_URL)){
            authAddr = cl.getOptionValue(LONG_OP_AUTH_URL);
        }else {
            throw new ScmToolsException("missing gateway url:--url", ScmExitCode.INVALID_ARG);
        }

        if (cl.hasOption(LONG_OP_ADMIN_PASSWD)) {
            adminPasswd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        }
        if (cl.hasOption(LONG_OP_ADMIN_USER)) {
            adminUser = cl.getOptionValue(LONG_OP_ADMIN_USER);
        }

        createUser(user, passwd, authAddr);
        System.out.println("Create user success:" + user);
    }

    private void createUser(String user, String passwd, String authAddr) throws ScmToolsException {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);
        String loginUrl = "http://" + authAddr + "/login";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", adminUser);
        params.add("password", adminPasswd);
        HttpEntity<MultiValueMap<String, String>> loginEntity = new HttpEntity<>(params,
                new HttpHeaders());

        ResponseEntity<String> resp = restTemplate.exchange(loginUrl, HttpMethod.POST, loginEntity,
                String.class);

        String sessionId = resp.getHeaders().getFirst("x-auth-token");
        if (sessionId == null) {
            throw new ScmToolsException("loggin failed,resp missing header:x-auth-token",
                    ScmExitCode.SYSTEM_ERROR);
        }
        String createUserUrl = "http://" + authAddr + "/api/v1/users/" + user;

        HttpHeaders header = new HttpHeaders();
        header.add("x-auth-token", sessionId);
        params = new LinkedMultiValueMap<>();
        params.add("password", passwd);
        HttpEntity<MultiValueMap<String, String>> createUserEntity = new HttpEntity<>(params, header);

        restTemplate.exchange(createUserUrl, HttpMethod.POST, createUserEntity, String.class);

        try {
            String logoutUrl = "http://" + authAddr + "/logout";
            HttpEntity<MultiValueMap<String, String>> logoutEntity = new HttpEntity<>(null,header);
            restTemplate.exchange(logoutUrl, HttpMethod.POST, logoutEntity, String.class);
        }
        catch (Exception e) {
            logger.warn("logout failed:sessionId={}", sessionId, e);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
