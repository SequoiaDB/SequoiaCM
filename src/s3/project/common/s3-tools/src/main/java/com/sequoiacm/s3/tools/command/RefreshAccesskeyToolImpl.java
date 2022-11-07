package com.sequoiacm.s3.tools.command;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmAccesskeyInfo;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.RestErrorHandler;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
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
import com.sequoiacm.s3.tools.common.ListLine;
import com.sequoiacm.s3.tools.common.ListTable;
import com.sequoiacm.s3.tools.common.ScmCommandUtil;
import com.sequoiacm.s3.tools.common.ScmCommonPrintor;
import com.sequoiacm.s3.tools.common.ScmHelpGenerator;

public class RefreshAccesskeyToolImpl extends ScmTool {
    private final String OPT_SHORT_TARGETUSER = "t";
    private final String OPT_LONG_TARGETUSER = "target-user";
    private final String OPT_LONG_USER = "user";
    private final String OPT_SHORT_USER = "u";
    private final String OPT_SHORT_PWD = "p";
    private final String OPT_LONG_PWD = "password";
    private final String OPT_SHORT_URL = "s";
    private final String OPT_LONG_URL = "s3-url";

    private final String OPT_LONG_GATEWAY_URL = "url";
    private final String OPT_LONG_ACCESSKEY = "accesskey";
    private final String OPT_LONG_SECRETKEY = "secretkey";

    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(RefreshAccesskeyToolImpl.class.getName());

    public RefreshAccesskeyToolImpl() throws ScmToolsException {
        super("refresh-accesskey");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_TARGETUSER, OPT_LONG_TARGETUSER,
                "refresh the accesskey of specified username.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_USER, OPT_LONG_USER, "username for login.", true, true,
                false));
        ops.addOption(hp.createOpt(OPT_SHORT_PWD, OPT_LONG_PWD, "password for login.", true, true,
                false, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_URL, OPT_LONG_URL,
                "s3 server url, default:localhost:8002", false, true, true));
        ops.addOption(hp.createOpt(null, OPT_LONG_GATEWAY_URL, "gateway url", false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ACCESSKEY, "target user accesskey", false, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_SECRETKEY, "target user secretkey", false, true,
                false, true, false));

    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        checkArgValid(cl);
        String user = cl.getOptionValue(OPT_SHORT_USER);
        String passwd = cl.getOptionValue(OPT_SHORT_PWD);
        String target = cl.getOptionValue(OPT_LONG_TARGETUSER);
        String accesskey = cl.getOptionValue(OPT_LONG_ACCESSKEY);
        String secretkey = cl.getOptionValue(OPT_LONG_SECRETKEY);
        if (passwd == null) {
            System.out.print("password for " + user + ": ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }

        if (isNeedSetAccesskey(cl)) {
            if (secretkey == null) {
                System.out.print("secretkey for " + accesskey + ": ");
                secretkey = ScmCommandUtil.readPasswdFromStdIn();
            }
        }

        String url = "localhost:8002";
        if (isHasGatewayURL(cl)) {
            url = cl.getOptionValue(OPT_LONG_GATEWAY_URL);
            ScmSession session = null;
            try {
                session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                        new ScmConfigOption(url, user, passwd));
                ScmAccesskeyInfo accesskeyInfo;
                if (target.equals(user)) {
                    accesskeyInfo = ScmFactory.S3.refreshAccesskey(session, target, passwd,
                            accesskey, secretkey);
                }
                else {
                    accesskeyInfo = ScmFactory.S3.refreshAccesskey(session, target, null, accesskey,
                            secretkey);
                }
                print(target, accesskeyInfo.getUsername(), accesskeyInfo.getAccesskey(),
                        accesskeyInfo.getSecretkey());
            }
            catch (Exception e) {
                logger.error("refresh accesskey failed", e);
                throw new ScmToolsException("refresh accesskey failed", ScmExitCode.SYSTEM_ERROR,
                        e);
            }
            finally {
                if (session != null) {
                    session.close();
                }
            }
        }
        else {
            url = isHasS3URL(cl) ? cl.getOptionValue(OPT_SHORT_URL) : url;
            s3RefreshAccesskey(url, target, user, passwd);
        }

    }

    private void print(String target, String username, String accesskey, String secretkey) {
        ListTable t = new ListTable();
        ListLine l = new ListLine();
        l.addItem(username);
        l.addItem(accesskey);
        l.addItem(secretkey);
        t.addLine(l);
        List<String> headerList = new ArrayList<String>();
        headerList.add("username");
        headerList.add("accesskey");
        headerList.add("secretkey");
        ScmCommonPrintor.print(headerList, t);

        logger.info("refresh successfully:user={}", target);
    }

    private void checkArgValid(CommandLine cl) throws ScmToolsException {
        boolean hasS3URL = isHasS3URL(cl);
        boolean hasGatewayURL = isHasGatewayURL(cl);
        boolean needSetAccesskey = isNeedSetAccesskey(cl);

        // 后续统一走网关进行 accesskey 的刷新，不再推荐使用 s3URL 的方式
        if (hasS3URL && hasGatewayURL) {
            throw new ScmToolsException(
                    "s3 url and gateway url cannot exist at the same time, please choose one",
                    ScmExitCode.SYSTEM_ERROR);
        }

        if (needSetAccesskey && !hasGatewayURL) {
            throw new ScmToolsException("s3 url does not support this new operation",
                    ScmExitCode.SYSTEM_ERROR);
        }
    }

    private boolean isHasS3URL(CommandLine cl) {
        return cl.hasOption(OPT_SHORT_URL);
    }

    private boolean isHasGatewayURL(CommandLine cl) {
        return cl.hasOption(OPT_LONG_GATEWAY_URL);
    }

    private boolean isNeedSetAccesskey(CommandLine cl) throws ScmToolsException {
        if (cl.hasOption(OPT_LONG_ACCESSKEY) && cl.hasOption(OPT_LONG_SECRETKEY)) {
            return true;
        }
        else if (!cl.hasOption(OPT_LONG_ACCESSKEY) && !cl.hasOption(OPT_LONG_SECRETKEY)) {
            return false;
        }
        else {
            throw new ScmToolsException("Accesskey and Secretkey need to exist at the same time",
                    ScmExitCode.SYSTEM_ERROR);
        }
    }

    private void s3RefreshAccesskey(String url, String target, String user, String passwd)
            throws ScmToolsException {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new RestErrorHandler());
        String refreshUrl = "http://" + url + "/users?Action=RefreshAccessKey";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target-username", target);
        params.add("username", user);
        String encryptPasswd = "";
        try {
            encryptPasswd = ScmPasswordMgr.getInstance().encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES,
                    passwd);
        }
        catch (Exception e1) {
            logger.error("failed to encode password", e1);
            throw new ScmToolsException("failed to encode password", ScmExitCode.SYSTEM_ERROR, e1);
        }
        params.add("password", encryptPasswd);
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(refreshUrl, HttpMethod.POST, entity,
                    String.class);
            BSONObject ret = (BSONObject) JSON.parse(resp.getBody());
            print(target, (String) ret.get("username"), (String) ret.get("accesskey"),
                    (String) ret.get("secretkey"));
        }
        catch (ResourceAccessException e) {
            logger.error("failed to connect to s3 server:{}", url, e);
            throw new ScmToolsException("failed to connect to s3 server:" + url,
                    ScmExitCode.SYSTEM_ERROR);
        }
        catch (RestClientException e) {
            logger.error("s3 server failed to do refresh", e);
            throw new ScmToolsException("s3 server failed to do refresh", ScmExitCode.SYSTEM_ERROR);
        }
    }
}
