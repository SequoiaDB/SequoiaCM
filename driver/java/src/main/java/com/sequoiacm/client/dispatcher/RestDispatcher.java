package com.sequoiacm.client.dispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmCheckConnTarget;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.infrastructure.common.ExceptionChecksUtils;
import com.sequoiacm.infrastructure.common.RestCommonDefine;
import com.sequoiacm.infrastructure.common.SignatureUtils;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import com.sequoiacm.client.core.*;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HttpContext;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.RestDefine;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.StatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.common.FultextRestCommonDefine;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;

public class RestDispatcher implements MessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(RestDispatcher.class);
    private static final String AUTHORIZATION = "x-auth-token";
    private static final String NO_AUTH_SESSION_ID = "-1";
    private static final String X_SCM_COUNT = "X-SCM-Count";
    private static final String CHARSET_UTF8 = "utf-8";
    private static final String LARGE_FILE_UPLOAD_PATH = "/zuul/";
    private static final String URL_SEP = "/";
    private static final String URL_PREFIX = "http://";
    private static final String API_VERSION = "/api/v1/";
    private static final String INTERNAL_VERSION = "/internal/v1/";
    private static final String API_V2_VERSION = "/api/v2/";
    private static final String LOGIN = "/login";
    private static final String V2LOCALLOGIN = "/v2/localLogin";

    private static final String V2_LOGIN_DATE_HEADER = "date";
    private static final String LOGOUT = "/logout";
    private static final String WORKSPACE = "workspaces/";
    private static final String FILE = "files/";
    private static final String SITE = "sites/";
    private static final String TASK = "tasks/";
    private static final String BATCH = "batches/";
    private static final String METADATA_CLASSES = "metadatas/classes/";
    private static final String METADATA_ATTRS = "metadatas/attrs/";
    private static final String DIRECTORIES = "directories/";
    private static final String SCHEDULE = "schedules/";
    private static final String LIFE_CYCLE_CONFIG = "lifeCycleConfig/";
    private static final String STAGE_TAG = "stageTag/";
    private static final String TRANSITION = "transition/";
    private static final String BREAKPOINT_FILES = "breakpointfiles/";
    private static final String SCHEDULE_SERVER = "/schedule-server";
    private static final String AUTH = "/auth";
    private static final String ROLE = "roles/";
    private static final String SALT = "salt";
    private static final String USER = "users/";
    private static final String SESSION = "sessions/";
    private static final String ID = "id/";
    private static final String RESOURCE = "resources/";
    private static final String PRIV_RELATIONS = "relations/";
    private static final String PRIVILEGES = "privileges";
    private static final String AUDIT = "audits/";
    private static final String ADMIN_SERVER = "/admin-server";
    private static final String STATISTICS = "statistics/";
    private static final String MONITOR = "monitor/";
    private static final String SERVICE_CENTER = "/service-center";
    private static final String CONFIG_SERVER = "/config-server";
    private static final String FULLTEXT_SERVER = "/fulltext-server";
    private static final String SERVICE_TRACE = "/service-trace";
    private static final String FULLTEXT = "fulltext";
    private static final String ACCESSKEY = "accesskey";
    private static final String BUCKETS = "buckets/";

    private static final String CONTENT_TYPE_BINARY = "binary/octet-stream";

    private final CloseableHttpClient httpClient;
    private final String pureUrl;
    private String remainUrl;
    private String url;
    private String sessionId = NO_AUTH_SESSION_ID;
    private boolean closed;
    private ScmRequestConfig requestConfig = ScmRequestConfig.custom().build();

    private class URIBuilder {

        private StringBuilder uri;

        private boolean hasFirstParam = false;

        public URIBuilder() {
            uri = new StringBuilder();
        }

        public URIBuilder appendURL(String... partUrls) {
            for (int i = 0; i < partUrls.length; i++) {
                uri.append(partUrls[i]);
            }
            return this;
        }

        public URIBuilder appendAndEncodeURL(String... partUrls) throws ScmException {
            for (int i = 0; i < partUrls.length; i++) {
                uri.append(encode(partUrls[i]));
            }
            return this;
        }

        public URIBuilder appendParam(String key, String value) throws ScmException {
            if (hasFirstParam) {
                uri.append("&");
            }
            else {
                uri.append("?");
                hasFirstParam = true;
            }
            uri.append(key);
            uri.append("=");
            uri.append(encode(value));
            return this;
        }

        @Override
        public String toString() {
            return uri.toString();
        }
    }

    public RestDispatcher(String url, ScmRequestConfig requestConfig) {
        this(url, requestConfig, null);
    }

    public RestDispatcher(String url, ScmRequestConfig requestConfig,
            CloseableHttpClient httpClient) {
        if (null != requestConfig) {
            this.requestConfig = requestConfig;
        }
        this.url = url;

        int idx = url.indexOf(URL_SEP);
        if (-1 != idx) {
            this.pureUrl = url.substring(0, idx);
            this.remainUrl = url.substring(idx + 1, url.length());
        }
        else {
            this.pureUrl = url;
            this.remainUrl = null;
        }

        if (httpClient == null) {
            this.httpClient = createHttpClient();
        }
        else {
            this.httpClient = httpClient;
        }
        closed = false;
    }

    private CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
        connMgr.setMaxTotal(2);
        connMgr.setDefaultMaxPerRoute(2);

        RequestConfig reqConf = RequestConfig.custom().setConnectionRequestTimeout(1)
                .setSocketTimeout(requestConfig.getSocketTimeout())
                .setConnectTimeout(requestConfig.getConnectTimeout()).build();

        return HttpClients.custom().setConnectionManager(connMgr).setDefaultRequestConfig(reqConf)
                .setRetryHandler(new HttpRequestRetryHandler() {
                    @Override
                    public boolean retryRequest(IOException exception, int executionCount,
                            HttpContext context) {
                        if (exception instanceof NoHttpResponseException && executionCount <= 1) {
                            return true;
                        }
                        return false;
                    }
                }).build();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            httpClient.close();
        }
        catch (Exception e) {
            logger.warn("close httpClient failed:client={}", httpClient, e);
        }
        closed = true;
    }

    private CloseableHttpClient getHttpClient() throws ScmException {
        if (closed) {
            throw new ScmException(ScmError.SESSION_CLOSED,
                    "Session has been closed:sessionId=" + sessionId);
        }
        return httpClient;
    }

    /**
     * login.
     *
     * @return sessionId
     */
    @Override
    public String login(String user, String password) throws ScmException {
        // login 兼容性算法和 Sequoiacm-infrastructure-security-auth 包下的 SignClient.loginWithUsername 里的算法相同,
        // 此处兼容性变更时，需两处同时变更
        BSONObject saltAndDate;
        try {
            saltAndDate = getSalt(user);
        }
        catch (ScmException e) {
            if (ExceptionChecksUtils.isOldVersion(e.getMessage(), e.getError().getErrorCode(),
                    "getSalt")
                    || ExceptionChecksUtils.isNotLocalUser(e.getError().getErrorDescription(),
                            e.getError().getErrorCode())) {
                return v1Login(user, password);
            }
            else if (e.getError() == ScmError.SALT_NOT_EXIST) {
                throw new ScmException(ScmError.HTTP_UNAUTHORIZED, "username or password error");
            }
            else {
                throw e;
            }
        }

        String salt = BsonUtils.getStringChecked(saltAndDate, "Salt");
        String date = BsonUtils.getStringChecked(saltAndDate, "Date");
        String signature = SignatureUtils.signatureCalculation(password, salt, date);

        try {
            return v2LocalLogin(user, signature, date);
        }
        catch (ScmException e) {
            if (ExceptionChecksUtils.isOldVersion(e.getMessage(), e.getError().getErrorCode(),
                    "v2LocalLogin")) {
                return v1Login(user, password);
            }
            else {
                throw e;
            }
        }
    }

    /**
     * logout.
     */
    @Override
    public void logout() throws ScmException {
        String uri = URL_PREFIX + pureUrl + LOGOUT;
        HttpPost request = new HttpPost(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    private BSONObject getSalt(String username) throws ScmException {
        String uri = URL_PREFIX + pureUrl + AUTH + API_V2_VERSION + SALT;
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", username));
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    private String v2LocalLogin(String user, String signature, String date) throws ScmException {
        HttpPost request = new HttpPost(URL_PREFIX + pureUrl + V2LOCALLOGIN);

        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("username", user));
        params.add(new BasicNameValuePair("password", signature));

        request.addHeader(V2_LOGIN_DATE_HEADER, date);

        sessionId = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                params, AUTHORIZATION);
        return sessionId;
    }

    private String v1Login(String user, String password) throws ScmException {
        HttpPost request = new HttpPost(URL_PREFIX + pureUrl + LOGIN);

        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("username", user));
        params.add(new BasicNameValuePair("password", password));

        sessionId = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                params, AUTHORIZATION);
        return sessionId;
    }

    @Override
    public BSONObject getSessionInfo(String sessionId) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, SESSION,
                sessionId);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), this.sessionId, request);
    }

    @Override
    public BsonReader listSessions(String username) throws ScmException {
        String uri;
        if (Strings.hasText(username)) {
            uri = String.format("%s%s%s%s%s?username=%s", URL_PREFIX, pureUrl, AUTH, API_VERSION,
                    SESSION, encode(username));
        }
        else {
            uri = String.format("%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, SESSION);
        }
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteSession(String sessionId) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, SESSION,
                sessionId);
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), this.sessionId, request);
    }

    @Override
    public BSONObject createRole(String roleName, String description) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s?description=%s", URL_PREFIX, pureUrl, AUTH,
                API_VERSION, ROLE, encode(roleName), encode(description));
        HttpPost request = new HttpPost(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getRole(String roleName) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, ROLE,
                encode(roleName));
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getRoleById(String roleId) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, ROLE,
                ID, roleId);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long countRole(BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + pureUrl + AUTH + API_VERSION + ROLE + "?filter=" + s;
        HttpHead request = new HttpHead(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.valueOf(count);
    }

    @Override
    public BSONObject getResourceById(String resourceId) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, RESOURCE,
                resourceId);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getPrivilegeById(String privilegeId) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION,
                PRIV_RELATIONS, privilegeId);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader listRoles(BSONObject filter, BSONObject orderBy, long skip, long limit)
            throws ScmException {
        String uri = String.format("%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, ROLE);
        String params = "";
        if (filter != null) {
            final String FIELD_FILTER = "filter";
            params = appendParam(params, FIELD_FILTER, encode(filter.toString()));
        }
        if (orderBy != null) {
            final String FIELD_ORDER_BY = "order_by";
            params = appendParam(params, FIELD_ORDER_BY, encode(orderBy.toString()));
        }

        final String FIELD_SKIP = "skip";
        final String FIELD_LIMIT = "limit";
        params = appendParam(params, FIELD_SKIP, skip + "");
        params = appendParam(params, FIELD_LIMIT, limit + "");

        uri += "?" + params;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader listPrivilegesByRoleId(String roleId) throws ScmException {
        final String FIELD_ROLE_ID = "role_id";

        String uri = String.format("%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION,
                PRIV_RELATIONS);
        String params = "";
        params = appendParam(params, FIELD_ROLE_ID, roleId);

        uri += "?" + params;
        HttpGet request = new HttpGet(uri);

        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader listPrivilegesByResource(String type, String resource) throws ScmException {
        final String FIELD_RESOURCE_TYPE = "resource_type";
        final String FIELD_RESOURCE = "resource";

        String uri = String.format("%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION,
                PRIV_RELATIONS);
        String params = "";
        params = appendParam(params, FIELD_RESOURCE_TYPE, type);
        params = appendParam(params, FIELD_RESOURCE, encode(resource));

        uri += "?" + params;
        HttpGet request = new HttpGet(uri);

        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader listResourceByWorkspace(String workspaceName) throws ScmException {
        final String FIELD_WORKSPACE_NAME = "workspace_name";

        String uri = String.format("%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, RESOURCE);

        String params = "";
        params = appendParam(params, FIELD_WORKSPACE_NAME, encode(workspaceName));

        uri += "?" + params;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteRole(String roleName) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, ROLE,
                encode(roleName));
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject createUser(String username, ScmUserPasswordType passwordType, String password)
            throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_V2_VERSION, USER,
                encode(username));
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("password_type", passwordType.name()));
        if (passwordType != ScmUserPasswordType.LDAP && passwordType != ScmUserPasswordType.TOKEN) {
            String encryptPassword;
            try {
                encryptPassword = ScmPasswordMgr.getInstance()
                        .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password);
            }
            catch (Exception e) {
                throw new ScmSystemException(CHARSET_UTF8, e);
            }
            params.add(new BasicNameValuePair("password", encryptPassword));
        }
        try {
            return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request,
                    params);
        }
        catch (ScmException e) {
            if (ExceptionChecksUtils.isOldVersion(e.getMessage(), e.getError().getErrorCode(),
                    "v2CreateUser")) {
                return v1CreateUser(username, passwordType, password);
            }
            else {
                throw e;
            }
        }
    }

    private BSONObject v1CreateUser(String username, ScmUserPasswordType passwordType,
            String password) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, USER,
                encode(username));
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("password_type", passwordType.name()));

        if (passwordType != ScmUserPasswordType.LDAP && passwordType != ScmUserPasswordType.TOKEN) {
            params.add(new BasicNameValuePair("password", password));
        }

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject getUser(String username) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, USER,
                encode(username));
        HttpGet request = new HttpGet(uri);

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject alterUser(String username, ScmUserModifier modifier) throws ScmException {
        final String FIELD_OLD_PASSWORD = "old_password";
        final String FIELD_NEW_PASSWORD = "new_password";
        final String FIELD_ADD_ROLES = "add_roles";
        final String FIELD_DEL_ROLES = "del_roles";
        final String FIELD_PASSWORD_TYPE = "password_type";
        final String FIELD_ENABLED = "enabled";
        final String FIELD_CLEAN_SESSIONS = "clean_sessions";

        boolean isAlterPassword = false;

        List<NameValuePair> params = new ArrayList<NameValuePair>();

        String encryptOldPassword = "";
        if (Strings.hasText(modifier.getOldPassword())) {
            try {
                encryptOldPassword = ScmPasswordMgr.getInstance()
                        .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, modifier.getOldPassword());
            }
            catch (Exception e) {
                throw new ScmSystemException(CHARSET_UTF8, e);
            }
            params.add(new BasicNameValuePair(FIELD_OLD_PASSWORD, encryptOldPassword));
        }

        String encryptNewPassword = "";
        if (Strings.hasText(modifier.getNewPassword())) {
            isAlterPassword = true;
            try {
                encryptNewPassword = ScmPasswordMgr.getInstance()
                        .encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, modifier.getNewPassword());
            }
            catch (Exception e) {
                throw new ScmSystemException(CHARSET_UTF8, e);
            }
            params.add(new BasicNameValuePair(FIELD_NEW_PASSWORD, encryptNewPassword));
        }

        if (!modifier.getAddRoles().isEmpty()) {
            StringBuilder roleNames = new StringBuilder();
            int n = modifier.getAddRoles().size();
            for (String roleName : modifier.getAddRoles()) {
                roleNames.append(roleName);
                n--;
                if (n != 0) {
                    roleNames.append(',');
                }
            }
            params.add(new BasicNameValuePair(FIELD_ADD_ROLES, roleNames.toString()));
        }
        if (!modifier.getDelRoles().isEmpty()) {
            StringBuilder roleNames = new StringBuilder();
            int n = modifier.getDelRoles().size();
            for (String roleName : modifier.getDelRoles()) {
                roleNames.append(roleName);
                n--;
                if (n != 0) {
                    roleNames.append(',');
                }
            }
            params.add(new BasicNameValuePair(FIELD_DEL_ROLES, roleNames.toString()));
        }
        if (modifier.getPasswordType() != null) {
            params.add(
                    new BasicNameValuePair(FIELD_PASSWORD_TYPE, modifier.getPasswordType().name()));
        }
        if (modifier.getEnabled() != null) {
            params.add(new BasicNameValuePair(FIELD_ENABLED, modifier.getEnabled().toString()));
        }
        if (modifier.getCleanSessions() != null) {
            params.add(new BasicNameValuePair(FIELD_CLEAN_SESSIONS,
                    modifier.getCleanSessions().toString()));
        }
        if (params.isEmpty()) {
            throw new ScmInvalidArgumentException("User modifier is empty");
        }

        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_V2_VERSION, USER,
                encode(username));
        HttpPut request = new HttpPut(uri);
        try {
            return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request,
                    params);
        }
        catch (ScmException e) {
            if (ExceptionChecksUtils.isOldVersion(e.getMessage(), e.getError().getErrorCode(),
                    "v2AlterUser")) {
                if (isAlterPassword) {
                    params.remove(new BasicNameValuePair(FIELD_OLD_PASSWORD, encryptOldPassword));
                    params.remove(new BasicNameValuePair(FIELD_NEW_PASSWORD, encryptNewPassword));
                    params.add(
                            new BasicNameValuePair(FIELD_OLD_PASSWORD, modifier.getOldPassword()));
                    params.add(
                            new BasicNameValuePair(FIELD_NEW_PASSWORD, modifier.getNewPassword()));
                }
                return v1AlterUser(username, params);
            }
            else {
                throw e;
            }
        }
    }

    private BSONObject v1AlterUser(String username, List<NameValuePair> params)
            throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, USER,
                encode(username));
        HttpPut request = new HttpPut(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BsonReader listUsers(BSONObject filter, long skip, long limit) throws ScmException {
        String uri = String.format("%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, USER);
        String params = "";
        if (filter != null) {
            final String FIELD_PASSWORD_TYPE = "password_type";
            final String FIELD_HAS_ROLE = "has_role";
            final String FIELD_ENABLED = "enabled";

            Object passwordType = BsonUtils.getObject(filter, FIELD_PASSWORD_TYPE);
            if (passwordType != null) {
                if (passwordType instanceof String) {
                    try {
                        ScmUserPasswordType.valueOf((String) passwordType);
                    }
                    catch (Exception e) {
                        throw new ScmInvalidArgumentException(
                                "Invalid password_type: " + passwordType, e);
                    }

                    params = appendParam(params, FIELD_PASSWORD_TYPE, (String) passwordType);
                }
                else if (passwordType instanceof ScmUserPasswordType) {
                    params = appendParam(params, FIELD_PASSWORD_TYPE,
                            ((ScmUserPasswordType) passwordType).name());
                }
                else {
                    throw new ScmInvalidArgumentException("Invalid password_type: " + passwordType);
                }
            }

            String roleName = BsonUtils.getString(filter, FIELD_HAS_ROLE);
            if (roleName != null && !roleName.isEmpty()) {
                params = appendParam(params, FIELD_HAS_ROLE, encode(roleName));
            }

            Boolean enabled = BsonUtils.getBoolean(filter, FIELD_ENABLED);
            if (enabled != null) {
                params = appendParam(params, FIELD_ENABLED, enabled.toString());
            }
        }

        final String FIELD_SKIP = "skip";
        final String FIELD_LIMIT = "limit";
        params = appendParam(params, FIELD_SKIP, skip + "");
        params = appendParam(params, FIELD_LIMIT, limit + "");
        uri += "?" + params;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteUser(String username) throws ScmException {
        String uri = String.format("%s%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION, USER,
                encode(username));
        HttpDelete request = new HttpDelete(uri);

        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getWorkspace(String wsName) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + WORKSPACE + encode(wsName);
        HttpGet request = new HttpGet(uri);

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        return (BSONObject) resp.get("workspace");
    }

    @Override
    public BsonReader getWorkspaceList(BSONObject condition, BSONObject orderBy, long skip,
            long limit) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + WORKSPACE + "?filter="
                + encodeCondition(condition);
        if (orderBy != null) {
            uri = uri + "&orderby=" + encode(orderBy.toString());
        }
        uri = uri + "&skip=" + skip;
        uri = uri + "&limit=" + limit;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long countFile(String workspaceName, int scope, BSONObject condition)
            throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + FILE + "?workspace_name="
                + encode(workspaceName) + "&filter=" + s + "&scope=" + scope;
        HttpHead request = new HttpHead(uri);

        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.valueOf(count);
    }

    @Override
    public BsonReader getFileList(String workspaceName, int scope, BSONObject condition,
            BSONObject orderby, long skip, long limit, BSONObject selector) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + "?workspace_name="
                + encode(workspaceName) + "&" + CommonDefine.RestArg.FILE_LIST_SCOPE + "=" + scope
                + "&" + CommonDefine.RestArg.FILE_SKIP + "=" + skip + "&"
                + CommonDefine.RestArg.FILE_LIMIT + "=" + limit + "&"
                + CommonDefine.RestArg.FILE_ORDERBY + "=" + encodeCondition(orderby) + "&"
                + CommonDefine.RestArg.FILE_FILTER + "=" + encodeCondition(condition) + "&"
                + CommonDefine.RestArg.FILE_SELECTOR + "=" + encodeCondition(selector);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader getFileList(String workspaceName, int scope, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmException {
        return getFileList(workspaceName, scope, condition, orderby, skip, limit, null);
    }

    @Override
    public BsonReader getSiteList(BSONObject condition, long skip, long limit) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + SITE + "?filter=" + s;
        uri = uri + "&skip=" + skip;
        uri = uri + "&limit=" + limit;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long countSite(BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + SITE + "?filter=" + s;
        HttpHead request = new HttpHead(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.parseLong(count);
    }

    /**
     * get file info.
     */
    @Override
    public BSONObject getFileInfo(String workspace_name, String fileId, String path,
            int majorVersion, int minorVersion) throws ScmException {
        String type;
        String file;
        if (fileId != null) {
            type = "id/";
            file = fileId;
        }
        else if (path != null) {
            type = "path/";
            file = processPath(path);
        }
        else {
            throw new ScmSystemException("inner error:path=null,id=null");
        }

        String uri = String.format(
                "%s%s%s%s%s%s?workspace_name=%s&minor_version=%d&major_version=%d", URL_PREFIX, url,
                API_VERSION, FILE, type, file, encode(workspace_name), minorVersion, majorVersion);

        HttpHead request = new HttpHead(uri);
        String resp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                CommonDefine.RestArg.FILE_RESP_FILE_INFO);
        return (BSONObject) org.bson.util.JSON.parse(resp);
    }

    @Override
    public BSONObject updateFileInfo(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject fileInfo) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileId + "?workspace_name="
                + encode(workspaceName);
        HttpPut request = new HttpPut(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("major_version", String.valueOf(majorVersion)));
        params.add(new BasicNameValuePair("minor_version", String.valueOf(minorVersion)));
        params.add(new BasicNameValuePair("file_info", fileInfo.toString()));

        String fileInfoResp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId,
                request, params, CommonDefine.RestArg.FILE_INFO);
        return (BSONObject) org.bson.util.JSON.parse(fileInfoResp);
    }

    @Override
    public BSONObject uploadFile(String workspaceName, InputStream is, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException {
        String uri = URL_PREFIX + pureUrl + LARGE_FILE_UPLOAD_PATH + remainUrl + API_VERSION + FILE
                + "?workspace_name=" + encode(workspaceName) + "&upload_config="
                + encode(uploadConf.toString());
        HttpPost request = new HttpPost(uri);
        request.addHeader(CommonDefine.RestArg.FILE_DESCRIPTION, encode(fileInfo.toString()));
        if (is != null) {
            InputStreamEntity isEntity = new InputStreamEntity(is);
            isEntity.setContentType(CONTENT_TYPE_BINARY);
            isEntity.setChunked(true);
            request.setEntity(isEntity);
        }

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        BSONObject file = (BSONObject) resp.get(CommonDefine.RestArg.FILE_RESP_FILE_INFO);
        return file;
    }

    @Override
    public BSONObject uploadFile(String workspaceName, String breakpointFileName,
            BSONObject fileInfo, BSONObject uploadConf) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + "?workspace_name="
                + encode(workspaceName) + "&upload_config=" + encode(uploadConf.toString());
        HttpPost request = new HttpPost(uri);
        request.addHeader(CommonDefine.RestArg.FILE_DESCRIPTION, encode(fileInfo.toString()));

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("breakpoint_file", breakpointFileName));

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        BSONObject file = (BSONObject) resp.get(CommonDefine.RestArg.FILE_RESP_FILE_INFO);
        return file;
    }

    public HttpURLConnection createHttpUrlConnection(String requestMethod, String urlStr,
            Map<String, String> reqProperties) throws ScmException {
        HttpURLConnection connection;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setChunkedStreamingMode(4096);
            connection.setUseCaches(false);
            connection.setRequestMethod(requestMethod);
            connection.setRequestProperty("Connection", "Keep-Alive");

            for (Map.Entry<String, String> entry : reqProperties.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            if (!sessionId.equals(NO_AUTH_SESSION_ID)) {
                connection.setRequestProperty(AUTHORIZATION, sessionId);
            }
            connection.connect();
            return connection;
        }
        catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ScmException(ScmError.NETWORK_IO,
                    "an error occurs during the http connection");
        }
    }

    @Override
    public HttpURLConnection getFileUploadConnection(String workspaceName, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException {
        String reqUrl = URL_PREFIX + pureUrl + LARGE_FILE_UPLOAD_PATH + remainUrl + API_VERSION
                + FILE + "?workspace_name=" + encode(workspaceName) + "&upload_config="
                + encode(uploadConf.toString());
        Map<String, String> reqProps = new HashMap<String, String>();
        reqProps.put("Content-Type", CONTENT_TYPE_BINARY);
        reqProps.put(CommonDefine.RestArg.FILE_DESCRIPTION, encode(fileInfo.toString()));
        return createHttpUrlConnection("POST", reqUrl, reqProps);
    }

    @Override
    public CloseableFileDataEntity downloadFile(String workspace_name, String fileId,
            int majorVersion, int minorVersion, int readFlag, long offset, int length)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileId + "?workspace_name="
                + encode(workspace_name) + "&" + CommonDefine.RestArg.FILE_MAJOR_VERSION + "="
                + majorVersion + "&" + CommonDefine.RestArg.FILE_MINOR_VERSION + "=" + minorVersion
                + "&" + CommonDefine.RestArg.FILE_READ_FLAG + "=" + readFlag + "&"
                + CommonDefine.RestArg.FILE_READ_OFFSET + "=" + offset + "&"
                + CommonDefine.RestArg.FILE_READ_LENGTH + "=" + length;

        HttpGet request = new HttpGet(uri);
        CloseableHttpResponseWrapper response = RestClient
                .sendRequestWithHttpResponse(getHttpClient(), sessionId, request);
        try {
            String dataLengthStr = response.getFirstHeader(CommonDefine.RestArg.DATA_LENGTH)
                    .getValue();
            Long dataLength = Long.valueOf(dataLengthStr);
            CloseableHttpResponseInputStream dataIs = new CloseableHttpResponseInputStream(
                    response);
            return new CloseableFileDataEntity(dataLength, dataIs);
        }
        catch (ScmException e) {
            throw e;
        }
        catch (Exception e) {
            response.closeResponse();
            throw new ScmSystemException("downloadFile failed", e);
        }
    }

    @Override
    public void deleteFile(String workspaceName, String fileID, int majorVersion, int minorVersion,
            boolean isPhysical) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileID + "?workspace_name="
                + encode(workspaceName) + "&" + CommonDefine.RestArg.FILE_IS_PHYSICAL + "="
                + isPhysical + "&majorVersion=" + majorVersion + "&minorVersion=" + minorVersion;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteFileByPath(String workspaceName, String filePath, int majorVersion, int minorVersion,
             boolean isPhysical) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + "?workspace_name="
                + encode(workspaceName) + "&" + CommonDefine.RestArg.FILE_IS_PHYSICAL + "="
                + isPhysical + "&file_path=" + filePath + "&majorVersion=" + majorVersion + "&minorVersion=" + minorVersion
                + "&action=" + CommonDefine.RestArg.ACTION_DELETE_FILE_BY_PATH;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public List<BSONObject> reloadBizConf(int scope, int id) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + "reload-bizconf?scope=" + scope + "&id=" + id;
        HttpPost request = new HttpPost(uri);

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        BasicBSONList array = (BasicBSONList) resp;
        List<BSONObject> list = new ArrayList<BSONObject>(array.size());
        for (Object obj : array) {
            list.add((BSONObject) obj);
        }
        return list;
    }

    @Override
    public BsonReader getNodeList(BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + "nodes" + "?filter=" + s;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader getTaskList(BSONObject condition) throws ScmException {
        return getTaskList(condition, null, null, 0, -1);
    }

    @Override
    public BsonReader getTaskList(BSONObject condition, BSONObject orderby, BSONObject selector,
            long skip, long limit) throws ScmException {
        String conditionStr = encodeCondition(condition);
        String orderbyStr = encodeCondition(orderby);
        String selectorStr = encodeCondition(selector);
        String uri = URL_PREFIX + url + API_VERSION + TASK + "?filter=" + conditionStr + "&orderby="
                + orderbyStr + "&selector=" + selectorStr + "&skip=" + skip + "&limit=" + limit;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public ScmId MsgStartTransferTask(String workspaceName, BSONObject condition, int scope,
            long maxExecTime, String targetSite, String dataCheckLevel, boolean quickStart)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + TASK + "?workspace_name="
                + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("task_type", "1"));
        BSONObject options = new BasicBSONObject();
        options.put("filter", condition);
        options.put(CommonDefine.RestArg.TASK_SCOPE, scope);
        options.put(CommonDefine.RestArg.TASK_MAX_EXEC_TIME, maxExecTime);
        options.put(CommonDefine.RestArg.TASK_DATA_CHECK_LEVEL, dataCheckLevel);
        options.put(CommonDefine.RestArg.TASK_QUICK_START, quickStart);
        params.add(new BasicNameValuePair("options", options.toString()));
        if (null != targetSite) {
            params.add(new BasicNameValuePair(CommonDefine.RestArg.CREATE_TASK_TARGET_SITE,
                    targetSite));
        }

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        BSONObject task = (BSONObject) resp.get("task");
        return new ScmId((String) task.get("id"), false);
    }

    @Override
    public ScmId MsgStartCleanTask(String workspaceName, BSONObject condition, int scope,
            long maxExecTime, String dataCheckLevel, boolean quickStart, boolean isRecycleSpace)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + TASK + "?workspace_name="
                + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("task_type", "2"));
        BSONObject options = new BasicBSONObject();
        options.put("filter", condition);
        options.put(CommonDefine.RestArg.TASK_SCOPE, scope);
        options.put(CommonDefine.RestArg.TASK_MAX_EXEC_TIME, maxExecTime);
        options.put(CommonDefine.RestArg.TASK_DATA_CHECK_LEVEL, dataCheckLevel);
        options.put(CommonDefine.RestArg.TASK_QUICK_START, quickStart);
        options.put(CommonDefine.RestArg.TASK_IS_RECYCLE_SPACE, isRecycleSpace);
        params.add(new BasicNameValuePair("options", options.toString()));

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        BSONObject task = (BSONObject) resp.get("task");
        return new ScmId((String) task.get("id"), false);
    }

    @Override
    public ScmId MsgStartMoveTask(String workspaceName, BSONObject condition, int scope,
            long maxExecTime, String targetSite, String dataCheckLevel, boolean quickStart,
            boolean isRecycleSpace) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + TASK + "?workspace_name="
                + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("task_type",
                String.valueOf(CommonDefine.TaskType.SCM_TASK_MOVE_FILE)));
        BSONObject options = new BasicBSONObject();
        options.put("filter", condition);
        options.put(CommonDefine.RestArg.TASK_SCOPE, scope);
        options.put(CommonDefine.RestArg.TASK_MAX_EXEC_TIME, maxExecTime);
        options.put(CommonDefine.RestArg.TASK_DATA_CHECK_LEVEL, dataCheckLevel);
        options.put(CommonDefine.RestArg.TASK_QUICK_START, quickStart);
        options.put(CommonDefine.RestArg.TASK_IS_RECYCLE_SPACE, isRecycleSpace);
        params.add(new BasicNameValuePair("options", options.toString()));
        if (null != targetSite) {
            params.add(new BasicNameValuePair(CommonDefine.RestArg.CREATE_TASK_TARGET_SITE,
                    targetSite));
        }
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        BSONObject task = (BSONObject) resp.get("task");
        return new ScmId((String) task.get("id"), false);
    }

    @Override
    public ScmId MsgStartSpaceRecyclingTask(String workspaceName, long maxExecTime,
            String recycleSpace) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + TASK + "?workspace_name="
                + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("task_type",
                String.valueOf(CommonDefine.TaskType.SCM_TASK_RECYCLE_SPACE)));
        BSONObject options = new BasicBSONObject();
        options.put(CommonDefine.RestArg.TASK_MAX_EXEC_TIME, maxExecTime);
        options.put(CommonDefine.RestArg.TASK_RECYCLE_SCOPE, recycleSpace);
        params.add(new BasicNameValuePair("options", options.toString()));

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        BSONObject task = (BSONObject) resp.get("task");
        return new ScmId((String) task.get("id"), false);
    }

    @Override
    public void MsgStopTask(ScmId taskId) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + TASK + taskId.get() + "/stop";
        HttpPost request = new HttpPost(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject MsgGetTask(ScmId taskId) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + TASK + taskId.get();
        HttpHead request = new HttpHead(uri);

        String task = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                "task");
        return (BSONObject) org.bson.util.JSON.parse(task);
    }

    @Override
    public void asyncTransferFile(String workspaceName, ScmId fileId, int majorVersion,
            int minorVersion, String targetSite) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileId.get() + "/async-transfer";
        String args = String.format("?%s=%s&%s=%s&%s=%s", CommonDefine.RestArg.WORKSPACE_NAME,
                encode(workspaceName), CommonDefine.RestArg.FILE_MAJOR_VERSION, majorVersion,
                CommonDefine.RestArg.FILE_MINOR_VERSION, minorVersion);
        if (targetSite != null) {
            args = args + "&" + CommonDefine.RestArg.FILE_ASYNC_TRANSFER_TARGET_SITE + "="
                    + encode(targetSite);
        }
        HttpPost request = new HttpPost(uri + args);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void asyncCacheFile(String workspaceName, ScmId fileId, int majorVersion,
            int minorVersion) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileId.get() + "/async-cache";
        String args = String.format("?%s=%s&%s=%s&%s=%s", CommonDefine.RestArg.WORKSPACE_NAME,
                encode(workspaceName), CommonDefine.RestArg.FILE_MAJOR_VERSION, majorVersion,
                CommonDefine.RestArg.FILE_MINOR_VERSION, minorVersion);
        HttpPost request = new HttpPost(uri + args);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getConfProperties(BSONObject keys) throws ScmException {
        StringBuilder sb = new StringBuilder();
        for (String key : keys.keySet()) {
            sb.append(key).append(",");
        }
        String uri = URL_PREFIX + url + API_VERSION + "conf-properties?keys="
                + encode(sb.toString().substring(0, sb.length() - 1));
        HttpGet request = new HttpGet(uri);
        BSONObject conf = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        return (BSONObject) conf.get("conf");
    }

    /**
     * Create a batch.
     */
    @Override
    public BSONObject createBatch(String workspaceName, BSONObject batchInfo) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BATCH + "?"
                + CommonDefine.RestArg.BATCH_WS_NAME + "=" + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.BATCH_DESCRIPTION,
                batchInfo.toString()));

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        return (BSONObject) resp.get(CommonDefine.RestArg.BATCH_OBJECT);
    }

    /**
     * Get batch info.
     */
    @Override
    public BSONObject getBatchInfo(String workspaceName, String batchId) throws ScmException {
        String uri = String.format("%s%s%s%s%s?%s=%s", URL_PREFIX, url, API_VERSION, BATCH,
                encode(batchId), CommonDefine.RestArg.BATCH_WS_NAME, encode(workspaceName));
        HttpGet request = new HttpGet(uri);
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        return (BSONObject) resp.get(CommonDefine.RestArg.BATCH_OBJECT);
    }

    /**
     * Get batch list
     */
    @Override
    public BsonReader getBatchList(String workspaceName, BSONObject filter, BSONObject orderBy,
            long skip, long limit) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BATCH + "?"
                + CommonDefine.RestArg.BATCH_WS_NAME + "=" + encode(workspaceName) + "&"
                + CommonDefine.RestArg.BATCH_FILTER + "=" + encodeCondition(filter);
        if (orderBy != null) {
            uri = uri + "&" + CommonDefine.RestArg.BATCH_ORDERBY + "=" + encode(orderBy.toString());
        }
        uri = uri + "&" + CommonDefine.RestArg.BATCH_SKIP + "=" + skip;
        uri = uri + "&" + CommonDefine.RestArg.BATCH_LIMIT + "=" + limit;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    /**
     * Delete batch
     */
    @Override
    public void deleteBatch(String workspaceName, String batchID) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BATCH + encode(batchID) + "?"
                + CommonDefine.RestArg.BATCH_WS_NAME + "=" + encode(workspaceName);
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    /**
     * Update a batch.
     */
    @Override
    public void updateBatchInfo(String workspaceName, String batchId, BSONObject batchInfo)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BATCH + encode(batchId) + "?"
                + CommonDefine.RestArg.BATCH_WS_NAME + "=" + encode(workspaceName);
        HttpPut request = new HttpPut(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.BATCH_DESCRIPTION,
                batchInfo.toString()));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    /**
     * Batch attach a file.
     */
    @Override
    public void batchAttachFile(String workspaceName, String batchId, String fileId)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BATCH + encode(batchId) + "/attachfile";
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.BATCH_WS_NAME, workspaceName));
        params.add(new BasicNameValuePair(CommonDefine.RestArg.BATCH_FILE_ID, fileId));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    /**
     * Batch detach a file.
     */
    @Override
    public void batchDetachFile(String workspaceName, String batchId, String fileId)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BATCH + encode(batchId) + "/detachfile";
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.BATCH_WS_NAME, workspaceName));
        params.add(new BasicNameValuePair(CommonDefine.RestArg.BATCH_FILE_ID, fileId));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject createClass(String workspaceName, BSONObject classInfo) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_CLASSES + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.METADATA_DESCRIPTION,
                classInfo.toString()));

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    /**
     * Get class attrs.
     */
    @Override
    public BSONObject getClassInfo(String workspaceName, ScmId classId) throws ScmException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.appendURL(URL_PREFIX, url, API_VERSION, METADATA_CLASSES, classId.get())
                .appendParam(CommonDefine.RestArg.WORKSPACE_NAME, workspaceName);
        HttpGet request = new HttpGet(uriBuilder.toString());
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    /**
     * Get class list
     */
    @Override
    public BSONObject getClassInfo(String workspaceName, String className) throws ScmException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.appendURL(URL_PREFIX, url, API_VERSION, METADATA_CLASSES)
                .appendParam(CommonDefine.RestArg.WORKSPACE_NAME, workspaceName)
                .appendParam(CommonDefine.RestArg.METADATA_CLASS_NAME, className)
                .appendParam("action", "findOneByName");
        HttpGet request = new HttpGet(uriBuilder.toString());
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    /**
     * Get class list
     */
    @Override
    public BsonReader getClassList(String workspaceName, BSONObject filter, BSONObject orderby,
            int skip, int limit) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_CLASSES + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(workspaceName) + "&"
                + CommonDefine.RestArg.METADATA_FILTER + "=" + encodeCondition(filter) + "&skip="
                + skip + "&limit=" + limit + "&orderby=" + encodeCondition(orderby);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteClass(String workspaceName, ScmId classId) throws ScmException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.appendURL(URL_PREFIX, url, API_VERSION, METADATA_CLASSES, classId.get())
                .appendParam(CommonDefine.RestArg.WORKSPACE_NAME, workspaceName);
        HttpDelete request = new HttpDelete(uriBuilder.toString());
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteClass(String workspaceName, String className) throws ScmException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.appendURL(URL_PREFIX, url, API_VERSION, METADATA_CLASSES)
                .appendParam(CommonDefine.RestArg.WORKSPACE_NAME, workspaceName)
                .appendParam(CommonDefine.RestArg.METADATA_CLASS_NAME, className)
                .appendParam("action", "deleteByName");
        HttpDelete request = new HttpDelete(uriBuilder.toString());
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject updateClassInfo(String workspaceName, ScmId classId, BSONObject classInfo)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_CLASSES + classId.get() + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(workspaceName);
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.METADATA_DESCRIPTION,
                classInfo.toString()));

        String classInfoResp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId,
                request, params, CommonDefine.RestArg.METADATA_CLASSINFO_RESP);
        return (BSONObject) org.bson.util.JSON.parse(classInfoResp);
    }

    @Override
    public void classAttachAttr(String workspaceName, ScmId classId, ScmId attrId)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_CLASSES + classId.get()
                + "/attachattr/" + attrId.get();
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.WORKSPACE_NAME, workspaceName));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void classDetachAttr(String workspaceName, ScmId classId, ScmId attrId)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_CLASSES + classId.get()
                + "/detachattr/" + attrId.get();
        HttpPut request = new HttpPut(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.WORKSPACE_NAME, workspaceName));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject createAttribute(String workspaceName, BSONObject attrInfo)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_ATTRS + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.METADATA_DESCRIPTION,
                attrInfo.toString()));

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject getAttributeInfo(String workspaceName, ScmId attrId) throws ScmException {
        String uri = String.format("%s%s%s%s%s?%s=%s", URL_PREFIX, url, API_VERSION, METADATA_ATTRS,
                attrId.get(), CommonDefine.RestArg.WORKSPACE_NAME, encode(workspaceName));
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader getAttributeList(String workspaceName, BSONObject filter)
            throws ScmException {
        String s = encodeCondition(filter);
        String uri = URL_PREFIX + url + API_VERSION + METADATA_ATTRS + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(workspaceName) + "&"
                + CommonDefine.RestArg.METADATA_FILTER + "=" + s;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteAttribute(String workspaceName, ScmId attrId) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_ATTRS + attrId.get() + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(workspaceName);
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject updateAttributeInfo(String workspaceName, ScmId attrId, BSONObject attrInfo)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + METADATA_ATTRS + attrId.get() + "?"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(workspaceName);
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.METADATA_DESCRIPTION,
                attrInfo.toString()));

        String attrInfoResp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId,
                request, params, CommonDefine.RestArg.METADATA_ATTRINFO_RESP);
        return (BSONObject) org.bson.util.JSON.parse(attrInfoResp);
    }

    @Override
    public BSONObject createDir(String workspaceName, String dirName, String dirParentId,
            String path) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES + "?workspace_name="
                + encode(workspaceName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (dirName != null) {
            params.add(new BasicNameValuePair(CommonDefine.Directory.SCM_REST_ARG_NAME, dirName));
        }
        if (dirParentId != null) {
            params.add(new BasicNameValuePair(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_ID,
                    dirParentId));
        }
        if (path != null) {
            params.add(new BasicNameValuePair(CommonDefine.Directory.SCM_REST_ARG_PATH, path));
        }

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject getDir(String workspaceName, String dirId, String path) throws ScmException {
        String type;
        String dir;

        if (dirId != null) {
            type = CommonDefine.Directory.SCM_REST_ARG_TYPE_ID + "/";
            dir = dirId;
        }
        else if (path != null) {
            type = CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH + "/";
            dir = processPath(path);
        }
        else {
            // should not come here
            throw new ScmSystemException("inner error");
        }

        String uri = String.format("%s%s%s%s%s%s?workspace_name=%s", URL_PREFIX, url, API_VERSION,
                DIRECTORIES, type, dir, encode(workspaceName));
        HttpHead request = new HttpHead(uri);

        String resp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                CommonDefine.Directory.SCM_REST_ARG_DIRECTORY);
        return (BSONObject) org.bson.util.JSON.parse(resp);
    }

    @Override
    public void deleteDir(String workspaceName, String dirId, String path) throws ScmException {
        String type;
        String dir;

        if (dirId != null) {
            type = CommonDefine.Directory.SCM_REST_ARG_TYPE_ID + "/";
            dir = dirId;
        }
        else if (path != null) {
            type = CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH + "/";
            dir = processPath(path);
        }
        else {
            // should not come here
            throw new ScmSystemException("inner error");
        }

        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES + type + dir + "?workspace_name="
                + encode(workspaceName);
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader getDirList(String workspaceName, BSONObject condition, BSONObject orderby,
            int skip, int limit) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES + "?workspace_name="
                + encode(workspaceName) + "&filter=" + encodeCondition(condition) + "&skip=" + skip
                + "&limit=" + limit + "&orderby=" + encodeCondition(orderby);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader getDirFileList(String workspaceName, String parentId, BSONObject condition,
            int skip, int limit, BSONObject orderby) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES + "id/" + parentId + "/listfiles/"
                + "?workspace_name=" + encode(workspaceName) + "&filter="
                + encodeCondition(condition) + "&" + CommonDefine.RestArg.FILE_SKIP + "=" + skip
                + "&" + CommonDefine.RestArg.FILE_LIMIT + "=" + limit + "&"
                + CommonDefine.RestArg.FILE_ORDERBY + "=" + encodeCondition(orderby);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long moveDir(String workspaceName, String dirId, String newParentId)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES
                + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID + "/" + dirId + "/move"
                + "?workspace_name=" + encode(workspaceName);
        HttpPut request = new HttpPut(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_ID,
                newParentId));

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        return (Long) resp.get(FieldName.FIELD_CLDIR_UPDATE_TIME);
    }

    @Override
    public long renameDir(String workspaceName, String dirId, String newName) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES
                + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID + "/" + dirId + "/rename"
                + "?workspace_name=" + encode(workspaceName);
        HttpPut request = new HttpPut(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.Directory.SCM_REST_ARG_NAME, newName));

        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        return (Long) resp.get(FieldName.FIELD_CLDIR_UPDATE_TIME);
    }

    @Override
    public String getPath(String workspaceName, String dirId) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES
                + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID + "/" + dirId + "/path"
                + "?workspace_name=" + encode(workspaceName);
        HttpGet httpGet = new HttpGet(uri);
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                httpGet);
        return (String) resp.get(CommonDefine.Directory.SCM_REST_ARG_PATH);
    }

    @Override
    public BSONObject createSchedule(String workspace, ScheduleType type, String name, String desc,
            BSONObject content, String cron, boolean enable, String preferredRegion,
            String preferredZone) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + SCHEDULE;
        HttpPost request = new HttpPost(uri);

        BSONObject obj = new BasicBSONObject();
        obj.put(RestDefine.RestKey.WORKSPACE, workspace);
        obj.put(RestDefine.RestKey.TYPE, type.getName());
        obj.put(RestDefine.RestKey.NAME, name);
        obj.put(RestDefine.RestKey.DESC, desc);
        obj.put(RestDefine.RestKey.CONTENT, content);
        obj.put(RestDefine.RestKey.CRON, cron);
        obj.put(RestDefine.RestKey.ENABLE, enable);
        obj.put(RestDefine.RestKey.PREFERRED_REGION, preferredRegion);
        obj.put(RestDefine.RestKey.PREFERRED_ZONE, preferredZone);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(RestDefine.RestKey.DESCRIPTION, obj.toString()));

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BsonReader getScheduleList(BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + SCHEDULE + "?filter="
                + encodeCondition(condition) + "&skip=" + skip + "&limit=" + limit + "&orderby="
                + encodeCondition(orderby);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteSchedule(String scheduleId) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + SCHEDULE + scheduleId;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getSchedule(String scheduleId) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + SCHEDULE + scheduleId;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject updateSchedule(String scheduleId, BSONObject newValue) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + SCHEDULE + scheduleId;
        HttpPut request = new HttpPut(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(
                new BasicNameValuePair(RestDefine.RestKey.DESCRIPTION, encodeCondition(newValue)));

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject createBreakpointFile(String workspaceName, String fileName, long createTime,
            ScmChecksumType checksumType, InputStream fileStream, boolean isLastContent,
            boolean isNeedMd5) throws ScmException {
        String uri = String.format(
                "%s%s%s%s%s?workspace_name=%s&create_time=%d&checksum_type=%s&is_last_content=%b&is_need_md5=%b",
                URL_PREFIX, url, API_VERSION, BREAKPOINT_FILES, encode(fileName),
                encode(workspaceName), createTime, checksumType.name(), isLastContent, isNeedMd5);
        HttpPost request = new HttpPost(uri);

        if (fileStream != null) {
            InputStreamEntity isEntity = new InputStreamEntity(fileStream);
            isEntity.setContentType(CONTENT_TYPE_BINARY);
            isEntity.setChunked(true);
            request.setEntity(isEntity);
        }

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject uploadBreakpointFile(String workspaceName, String fileName,
            InputStream fileStream, long offset, boolean isLastContent) throws ScmException {
        String uri;
        if (Strings.hasText(remainUrl)) {
            uri = String.format("%s%s%s%s%s%s%s?workspace_name=%s&offset=%d&is_last_content=%b",
                    URL_PREFIX, pureUrl, LARGE_FILE_UPLOAD_PATH, remainUrl, API_VERSION,
                    BREAKPOINT_FILES, encode(fileName), encode(workspaceName), offset,
                    isLastContent);
        }
        else {
            uri = String.format("%s%s%s%s%s?workspace_name=%s&offset=%d&is_last_content=%b",
                    URL_PREFIX, url, API_VERSION, BREAKPOINT_FILES, encode(fileName),
                    encode(workspaceName), offset, isLastContent);
        }
        HttpPut request = new HttpPut(uri);

        InputStreamEntity isEntity = new InputStreamEntity(fileStream);
        isEntity.setContentType(CONTENT_TYPE_BINARY);
        isEntity.setChunked(true);
        request.setEntity(isEntity);

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getBreakpointFile(String workspaceName, String fileName) throws ScmException {
        String uri = String.format("%s%s%s%s%s?workspace_name=%s", URL_PREFIX, url, API_VERSION,
                BREAKPOINT_FILES, encode(fileName), encode(workspaceName));
        HttpHead request = new HttpHead(uri);
        String resp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                "X-SCM-BREAKPOINTFILE");
        return (BSONObject) org.bson.util.JSON.parse(resp);
    }

    @Override
    public BsonReader listBreakpointFiles(String workspaceName, BSONObject condition)
            throws ScmException {
        String filter = encodeCondition(condition);

        String uri;
        if (filter.isEmpty()) {
            uri = String.format("%s%s%s%s?workspace_name=%s", URL_PREFIX, url, API_VERSION,
                    BREAKPOINT_FILES, encode(workspaceName));
        }
        else {
            uri = String.format("%s%s%s%s?workspace_name=%s&filter=%s", URL_PREFIX, url,
                    API_VERSION, BREAKPOINT_FILES, encode(workspaceName), filter);
        }

        HttpGet request = new HttpGet(uri);

        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteBreakpointFile(String workspaceName, String fileName) throws ScmException {
        String uri = String.format("%s%s%s%s%s?workspace_name=%s", URL_PREFIX, url, API_VERSION,
                BREAKPOINT_FILES, encode(fileName), encode(workspaceName));
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    private static String encodeCondition(BSONObject condition) throws ScmException {
        if (null == condition) {
            return "";
        }
        return encode(condition.toString());
    }

    private static String processPath(String path) throws ScmException {
        StringBuilder newPath = new StringBuilder("");
        String[] names = path.split("/+");
        for (int i = 0; i < names.length; i++) {
            if (!names[i].isEmpty()) {
                ScmArgChecker.Directory.checkDirectoryName(names[i]);
                newPath.append(encode(names[i]));
                newPath.append("/");
            }
        }
        return newPath.toString();
    }

    private static String appendParam(String originParams, String key, String value) {
        String params = originParams;

        if (!params.isEmpty()) {
            params += "&";
        }

        params += key + "=" + value;
        return params;
    }

    @Override
    public void grantPrivilege(String roleName, String resourceType, String resource,
            String privilege) throws ScmException {
        final String FIELD_RESOURCE_TYPE = "resource_type";
        final String FIELD_RESOURCE = "resource";
        final String FIELD_PRIVILEGE = "privilege";

        // send to content server
        String uri = String.format("%s%s%s%s%s/grant", URL_PREFIX, url, API_VERSION, ROLE,
                encode(roleName));
        String params = "";
        params = appendParam(params, FIELD_RESOURCE_TYPE, resourceType);
        params = appendParam(params, FIELD_RESOURCE, encode(resource));
        params = appendParam(params, FIELD_PRIVILEGE, encode(privilege));

        uri += "?" + params;

        HttpPut request = new HttpPut(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void revokePrivilege(String roleName, String resourceType, String resource,
            String privilege) throws ScmException {
        final String FIELD_RESOURCE_TYPE = "resource_type";
        final String FIELD_RESOURCE = "resource";
        final String FIELD_PRIVILEGE = "privilege";

        // send to content server
        String uri = String.format("%s%s%s%s%s/revoke", URL_PREFIX, url, API_VERSION, ROLE,
                encode(roleName));
        String params = "";
        params = appendParam(params, FIELD_RESOURCE_TYPE, resourceType);
        params = appendParam(params, FIELD_RESOURCE, encode(resource));
        params = appendParam(params, FIELD_PRIVILEGE, encode(privilege));

        uri += "?" + params;

        HttpPut request = new HttpPut(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    private static String encode(String url) throws ScmException {
        if (Strings.isEmpty(url)) {
            return "";
        }

        try {
            return URLEncoder.encode(url, CHARSET_UTF8).replaceAll("[+]", "%20");
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmSystemException(CHARSET_UTF8, e);
        }
    }

    @Override
    public BSONObject getPrivilegeMeta() throws ScmException {
        String uri = String.format("%s%s%s%s%s", URL_PREFIX, pureUrl, AUTH, API_VERSION,
                PRIVILEGES);
        HttpGet request = new HttpGet(uri);

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject updateFileContent(String workspaceName, String fileId, int majorVersion,
            int minorVersion, InputStream is, BSONObject option) throws ScmException {
        String uri = URL_PREFIX + pureUrl + LARGE_FILE_UPLOAD_PATH + remainUrl + API_VERSION + FILE
                + fileId;
        String arg = String.format("?%s=%s&%s=%s&%s=%s&%s=%s", CommonDefine.RestArg.WORKSPACE_NAME,
                encode(workspaceName), CommonDefine.RestArg.FILE_MAJOR_VERSION, majorVersion,
                CommonDefine.RestArg.FILE_MINOR_VERSION, minorVersion,
                CommonDefine.RestArg.FILE_UPDATE_CONTENT_OPTION, encodeCondition(option));
        HttpPut request = new HttpPut(uri + arg);
        InputStreamEntity isEntity = new InputStreamEntity(is);
        isEntity.setContentType(CONTENT_TYPE_BINARY);
        isEntity.setChunked(true);
        request.setEntity(isEntity);
        String fileInfoResp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId,
                request, CommonDefine.RestArg.FILE_INFO);
        return (BSONObject) org.bson.util.JSON.parse(fileInfoResp);
    }

    @Override
    public BSONObject updateFileContent(String workspaceName, String fileId, int majorVersion,
            int minorVersion, String breakFileName, BSONObject option) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileId;
        String arg = String.format("?%s=%s&%s=%s&%s=%s&%s=%s&%s=%s",
                CommonDefine.RestArg.WORKSPACE_NAME, encode(workspaceName),
                CommonDefine.RestArg.FILE_MAJOR_VERSION, majorVersion,
                CommonDefine.RestArg.FILE_MINOR_VERSION, minorVersion,
                CommonDefine.RestArg.FILE_BREAKPOINT_FILE, encode(breakFileName),
                CommonDefine.RestArg.FILE_UPDATE_CONTENT_OPTION, encodeCondition(option));
        HttpPut request = new HttpPut(uri + arg);
        String fileInfoResp = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId,
                request, CommonDefine.RestArg.FILE_INFO);
        return (BSONObject) org.bson.util.JSON.parse(fileInfoResp);
    }

    @Override
    public HttpURLConnection getUpdateFileContentConn(String workspaceName, String fileId,
            int majorVersion, int minorVersion) throws ScmException {
        HttpURLConnection connection;
        String uri = URL_PREFIX + pureUrl + LARGE_FILE_UPLOAD_PATH + remainUrl + API_VERSION + FILE
                + fileId;
        String arg = String.format("?%s=%s&%s=%s&%s=%s", CommonDefine.RestArg.WORKSPACE_NAME,
                encode(workspaceName), CommonDefine.RestArg.FILE_MAJOR_VERSION, majorVersion,
                CommonDefine.RestArg.FILE_MINOR_VERSION, minorVersion);
        Map<String, String> reqProps = new HashMap<String, String>();
        reqProps.put("Content-Type", CONTENT_TYPE_BINARY);
        return createHttpUrlConnection("PUT", uri + arg, reqProps);
    }

    @Override
    public BSONObject createWorkspace(String wsName, BSONObject conf) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + WORKSPACE + encode(wsName);
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.WORKSPACE_CONF, conf.toString()));
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        return (BSONObject) resp.get(CommonDefine.RestArg.GET_WORKSPACE_REPS);
    }

    @Override
    public void deleteWorkspace(String wsName, boolean isEnforced) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + WORKSPACE + encode(wsName) + "?"
                + CommonDefine.RestArg.WORKSPACE_ENFORCED_DELETE + "=" + isEnforced;
        HttpDelete req = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, req);
    }

    /*
     *
     * get Audit List
     */
    @Override
    public BsonReader getAuditList(BSONObject filter) throws ScmException {
        String s = encodeCondition(filter);
        String uri = URL_PREFIX + url + API_VERSION + AUDIT + "?"
                + CommonDefine.RestArg.AUDIT_FILTER + "=" + s;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public CloseableFileDataEntity downloadFile(String workspace_name, String fileId,
            int majorVersion, int minorVersion, int readFlag) throws ScmException {
        return downloadFile(workspace_name, fileId, majorVersion, minorVersion, readFlag, 0,
                CommonDefine.File.UNTIL_END_OF_FILE);
    }

    @Override
    public BSONObject updateWorkspace(String wsName, BSONObject updator) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + WORKSPACE + encode(wsName);
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(
                new BasicNameValuePair(CommonDefine.RestArg.WORKSPACE_UPDATOR, updator.toString()));
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        return (BSONObject) resp.get(CommonDefine.RestArg.GET_WORKSPACE_REPS);
    }

    @Override
    public BsonReader listHealth(String serviceName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + MONITOR + "health";
        if (serviceName != null) {
            String name = encode(serviceName);
            uri = uri + "?name=" + name;
        }
        logger.info(uri);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BasicBSONList getCheckConnResult(String node, ScmCheckConnTarget target)
            throws ScmException {
        String uri = URL_PREFIX + node + INTERNAL_VERSION + "connectivity-check";
        HttpPost request = new HttpPost(uri);
        BasicBSONList ret = null;

        if (target.isAll()) {
            ret = (BasicBSONList) RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                    request);
        }
        else {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(CommonDefine.RestArg.NODES,
                    listToString(target.getInstances())));
            params.add(new BasicNameValuePair(CommonDefine.RestArg.SERVICES,
                    listToString(target.getServices())));
            ret = (BasicBSONList) RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                    request, params);
        }
        return ret;
    }

    private String listToString(List<String> list) throws ScmException {
        StringBuilder strBuilder = new StringBuilder();
        for (String item : list) {
            if (item == null || item.isEmpty()) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "item can not be null or empty");
            }
            if (strBuilder.length() == 0) {
                strBuilder.append(item);
            }
            else {
                strBuilder.append("," + item);
            }
        }
        return strBuilder.toString();
    }

    @Override
    public BsonReader listHostInfo() throws ScmException {
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + MONITOR + "host_info";
        logger.info(uri);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader showFlow() throws ScmException {
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + MONITOR + "show_flow";
        logger.info(uri);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader gaugeResponse() throws ScmException {
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + MONITOR + "gauge_response";
        logger.info(uri);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long countSessions() throws ScmException {
        String uri = URL_PREFIX + pureUrl + AUTH + API_VERSION + SESSION;
        logger.info(uri);
        HttpHead request = new HttpHead(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.valueOf(count);
    }

    @Override
    public void refreshStatistics(StatisticsType type, String workspace) throws ScmException {
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + STATISTICS + "refresh";
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.REFRESH_STATISTICS_TYPE,
                String.valueOf(type.getType())));
        params.add(
                new BasicNameValuePair(CommonDefine.RestArg.REFRESH_STATISTICS_WS_NAME, workspace));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BsonReader getStatisticsFileDeltaList(BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + STATISTICS + "delta/file"
                + "?filter=" + s;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader getStatisticsTrafficList(BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + STATISTICS + "traffic/file"
                + "?filter=" + s;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject listServerInstance(String serviceName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SERVICE_CENTER + "/eureka/apps/" + encode(serviceName);
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject updateConfProps(String targetType, List<String> targets,
            Map<String, String> props, List<String> deleteProps, boolean isAcceptUnknownProps)
            throws ScmException {
        String uri = URL_PREFIX + pureUrl + CONFIG_SERVER + API_VERSION + "config-props";
        HttpPut req = new HttpPut(uri);
        req.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        BSONObject obj = new BasicBSONObject();
        obj.put("target_type", targetType);
        obj.put("targets", targets);
        obj.put("update_properties", props);
        obj.put("delete_properties", deleteProps);
        obj.put("accept_unknown_props", isAcceptUnknownProps);
        req.setEntity(new StringEntity(obj.toString(), Consts.UTF_8));
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, req);
    }

    @Override
    public void resetRemainUrl(String remainUrl) {
        this.remainUrl = remainUrl;
        if (Strings.hasText(remainUrl)) {
            url = pureUrl + "/" + remainUrl;
        }
        else {
            url = pureUrl;
        }
    }

    @Override
    public String getRemainUrl() {
        return remainUrl;
    }

    @Override
    public long countDir(String workspaceName, BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + DIRECTORIES + "?workspace_name="
                + encode(workspaceName) + "&filter=" + s;
        HttpHead request = new HttpHead(uri);

        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.valueOf(count);
    }

    @Override
    public long countBatch(String workspaceName, BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + BATCH + "?workspace_name="
                + encode(workspaceName) + "&filter=" + s;
        HttpHead request = new HttpHead(uri);

        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.valueOf(count);
    }

    @Override
    public String calcBreakpointFileMd5(String wsName, String breakpointFile) throws ScmException {
        String uri = String.format("%s%s%s%s%s?action=%s", URL_PREFIX, url, API_VERSION,
                BREAKPOINT_FILES, encode(breakpointFile), CommonDefine.RestArg.ACTION_CALC_MD5);
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.WORKSPACE_NAME, wsName));
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        return BsonUtils.getStringChecked(resp, FieldName.BreakpointFile.FIELD_MD5);
    }

    @Override
    public String calcScmFileMd5(String wsName, String fileId, int majorVersion, int minorVersion)
            throws ScmException {
        String uri = String.format("%s%s%s%s%s?action=%s", URL_PREFIX, url, API_VERSION, FILE,
                fileId, CommonDefine.RestArg.ACTION_CALC_MD5);
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.WORKSPACE_NAME, wsName));
        params.add(
                new BasicNameValuePair(CommonDefine.RestArg.FILE_MAJOR_VERSION, majorVersion + ""));
        params.add(
                new BasicNameValuePair(CommonDefine.RestArg.FILE_MINOR_VERSION, minorVersion + ""));
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        return BsonUtils.getStringChecked(resp, FieldName.FIELD_CLFILE_FILE_MD5);
    }

    @Override
    public void createFulltextIndex(String wsName, BSONObject fileCondition, ScmFulltextMode mode)
            throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=create";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_FILEMATCHER,
                fileCondition.toString()));
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_INDEX_MONDE, mode.name()));
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_WORKSPACE, wsName));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void dropFulltextIndex(String wsName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=drop";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_WORKSPACE, wsName));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void updateFulltextIndex(String wsName, BSONObject newFileCondition,
            ScmFulltextMode newMode) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=update";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (newFileCondition != null) {
            params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_FILEMATCHER,
                    newFileCondition.toString()));
        }
        if (newMode != null) {
            params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_INDEX_MONDE,
                    newMode.name()));
        }
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_WORKSPACE, wsName));

        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public ScmFulltexInfo getWsFulltextIdxInfo(String wsName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=get_idx_info&" + FultextRestCommonDefine.REST_WORKSPACE + "="
                + encode(wsName);
        HttpGet get = new HttpGet(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_WORKSPACE, wsName));
        BSONObject bson = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, get);
        return new ScmFulltexInfo(bson);
    }

    @Override
    public BsonReader fulltextSearch(String ws, int scope, BSONObject fileCondition,
            BSONObject contentCondition) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=search&" + FultextRestCommonDefine.REST_WORKSPACE + "=" + encode(ws)
                + "&" + FultextRestCommonDefine.REST_SCOPE + "=" + scope + "&"
                + FultextRestCommonDefine.REST_FILE_CONDITION + "=" + encodeCondition(fileCondition)
                + "&" + FultextRestCommonDefine.REST_CONTENT_CONDITION + "="
                + encodeCondition(contentCondition);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void rebuildFulltextIdx(String ws, String fileId) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=rebuild";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_WORKSPACE, ws));
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_FILE_ID, fileId));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void inspectFulltextIndex(String wsName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=inspect";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FultextRestCommonDefine.REST_WORKSPACE, wsName));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BsonReader listFileWithFileIdxStatus(String ws, String status) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=get_file_idx_info&" + FultextRestCommonDefine.REST_WORKSPACE + "="
                + encode(ws) + "&" + FultextRestCommonDefine.REST_FILE_IDX_STATUS + "=" + status;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long countFileWithFileIdxStatus(String ws, String status) throws ScmException {
        String uri = URL_PREFIX + pureUrl + FULLTEXT_SERVER + API_VERSION + FULLTEXT
                + "?action=count_file&" + FultextRestCommonDefine.REST_WORKSPACE + "=" + encode(ws)
                + "&" + FultextRestCommonDefine.REST_FILE_IDX_STATUS + "=" + status;
        HttpGet request = new HttpGet(uri);
        String countStr = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId,
                request, X_SCM_COUNT);
        return Long.valueOf(countStr);
    }

    @Override
    public BSONObject getStatisticsData(String type, BSONObject condition) throws ScmException {
        String s = encodeCondition(condition);
        String uri = URL_PREFIX + pureUrl + ADMIN_SERVER + API_VERSION + STATISTICS + "types/"
                + type + "?" + ScmStatisticsDefine.REST_PARAM_CONDITION + "=" + s;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long countWorkspace(BSONObject condition) throws ScmException {
        String filter = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + WORKSPACE + "?&filter=" + filter;
        HttpHead request = new HttpHead(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.parseLong(count);
    }

    @Override
    public long countSchedule(BSONObject condition) throws ScmException {
        String filter = encodeCondition(condition);
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + SCHEDULE + "?&filter="
                + filter;
        HttpHead request = new HttpHead(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.parseLong(count);
    }

    @Override
    public BSONObject getSiteStrategy() throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + SITE + "?action="
                + CommonDefine.RestArg.ACTION_GET_SITE_STRATEGY;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long countTask(BSONObject condition) throws ScmException {
        String filter = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + TASK + "?&filter=" + filter;
        HttpHead request = new HttpHead(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.parseLong(count);
    }

    @Override
    public long countClass(String workspaceName, BSONObject condition) throws ScmException {
        String filter = encodeCondition(condition);
        String uri = URL_PREFIX + url + API_VERSION + METADATA_CLASSES + "?workspace_name="
                + encode(workspaceName) + "&filter=" + filter;
        HttpHead request = new HttpHead(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.parseLong(count);
    }

    @Override
    public BasicBSONList getContentLocations(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileId;

        String arg = String.format("?action=%s&%s=%s&%s=%s&%s=%s",
                CommonDefine.RestArg.ACTION_GET_CONTENT_LOCATION,
                CommonDefine.RestArg.WORKSPACE_NAME, encode(workspaceName),
                CommonDefine.RestArg.FILE_MAJOR_VERSION, majorVersion,
                CommonDefine.RestArg.FILE_MINOR_VERSION, minorVersion);
        HttpGet request = new HttpGet(uri + arg);
        return (BasicBSONList) RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
    }

    @Override
    public BSONObject createBucket(String wsName, String name) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + "?"
                + CommonDefine.RestArg.BUCKET_NAME + "=" + encode(name) + "&"
                + CommonDefine.RestArg.WORKSPACE_NAME + "=" + encode(wsName);
        HttpPost request = new HttpPost(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getBucket(String name) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(name)
                + "?action=get_bucket_by_name";
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getBucket(long id) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + id + "?action=get_bucket_by_id";
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteBucket(String name) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(name);
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader listBucket(BSONObject condition, BSONObject orderby, long skip, long limit)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + "?" + CommonDefine.RestArg.FILTER
                + "=" + encodeCondition(condition) + "&" + CommonDefine.RestArg.ORDER_BY + "="
                + encodeCondition(orderby) + "&" + CommonDefine.RestArg.SKIP + "=" + skip + "&"
                + CommonDefine.RestArg.LIMIT + "=" + limit;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject createFileInBucket(String bucketName, InputStream is, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException {
        String uri = URL_PREFIX + pureUrl + LARGE_FILE_UPLOAD_PATH + remainUrl + API_VERSION
                + BUCKETS + encode(bucketName) + "/files?action=create_file&upload_config="
                + encode(uploadConf.toString());
        HttpPost request = new HttpPost(uri);
        request.addHeader(CommonDefine.RestArg.FILE_DESCRIPTION, encode(fileInfo.toString()));
        if (is != null) {
            InputStreamEntity isEntity = new InputStreamEntity(is);
            isEntity.setContentType(CONTENT_TYPE_BINARY);
            isEntity.setChunked(true);
            request.setEntity(isEntity);
        }

        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public HttpURLConnection createFileInBucketConn(String bucketName, BSONObject fileInfo,
            BSONObject uploadConf) throws ScmException {
        String uri = URL_PREFIX + pureUrl + LARGE_FILE_UPLOAD_PATH + remainUrl + API_VERSION
                + BUCKETS + encode(bucketName) + "/files?action=create_file&upload_config="
                + encode(uploadConf.toString());
        Map<String, String> reqProps = new HashMap<String, String>();
        reqProps.put(CommonDefine.RestArg.FILE_DESCRIPTION, encode(fileInfo.toString()));
        reqProps.put("Content-Type", CONTENT_TYPE_BINARY);
        return createHttpUrlConnection("POST", uri, reqProps);
    }

    @Override
    public BSONObject createFileInBucket(String bucketName, String breakpointFileName,
            BSONObject fileInfo, BSONObject uploadConf) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=create_file&upload_config=" + encode(uploadConf.toString());
        HttpPost request = new HttpPost(uri);
        request.addHeader(CommonDefine.RestArg.FILE_DESCRIPTION, encode(fileInfo.toString()));

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.FILE_BREAKPOINT_FILE,
                breakpointFileName));
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public List<BSONObject> bucketAttachFile(String bucketName, List<String> fileId,
            ScmBucketAttachKeyType type) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=attach_file";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.FILE_ID_LIST,
                Strings.join(fileId, ",")));
        params.add(new BasicNameValuePair(CommonDefine.RestArg.ATTACH_KEY_TYPE, type.name()));

        BasicBSONList ret = (BasicBSONList) RestClient.sendRequestWithJsonResponse(getHttpClient(),
                sessionId, request, params);
        List<BSONObject> bsonObjects = new ArrayList<BSONObject>();
        for (Object b : ret) {
            bsonObjects.add((BSONObject) b);
        }
        return bsonObjects;
    }

    @Override
    public void bucketDetachFile(String bucketName, String fileName) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=detach_file";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.FILE_NAME, fileName));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject bucketGetFile(String bucketName, String fileName, int majorVersion,
            int minorVersion) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=get_file&" + CommonDefine.RestArg.FILE_NAME + "="
                + encode(fileName) + "&" + CommonDefine.RestArg.FILE_MAJOR_VERSION + "="
                + majorVersion + "&" + CommonDefine.RestArg.FILE_MINOR_VERSION + "=" + minorVersion;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject bucketGetFileNullVersion(String bucketName, String fileName)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=get_file_null_version&" + CommonDefine.RestArg.FILE_NAME + "="
                + encode(fileName);
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BsonReader bucketListFile(String bucketName, ScopeType scope, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=list_file&" + CommonDefine.RestArg.FILTER + "="
                + encodeCondition(condition) + "&" + CommonDefine.RestArg.ORDER_BY + "="
                + encodeCondition(orderby) + "&" + CommonDefine.RestArg.SKIP + "=" + skip + "&"
                + CommonDefine.RestArg.LIMIT + "=" + limit;
        if (scope != null) {
            uri = uri + "&" + CommonDefine.RestArg.FILE_LIST_SCOPE + "=" + scope.getScope();
        }
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithBsonReaderResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public long bucketCountFile(String bucketName, ScopeType scope, BSONObject condition)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=count_file&" + CommonDefine.RestArg.FILTER + "="
                + encodeCondition(condition);
        if (scope != null) {
            uri = uri + "&" + CommonDefine.RestArg.FILE_LIST_SCOPE + "=" + scope.getScope();
        }
        HttpGet request = new HttpGet(uri);
        String count = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                X_SCM_COUNT);
        return Long.parseLong(count);
    }

    @Override
    public void bucketDeleteFile(String bucketName, String fileName, boolean isPhysical)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=delete_file&" + CommonDefine.RestArg.FILE_IS_PHYSICAL + "="
                + isPhysical + "&" + CommonDefine.RestArg.FILE_NAME + "=" + encode(fileName);
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void bucketDeleteFileVersion(String bucketName, String fileName, int majorVersion,
            int minorVersion) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "/files?action=delete_file_version&" + CommonDefine.RestArg.FILE_NAME + "="
                + encode(fileName) + "&" + CommonDefine.RestArg.FILE_MAJOR_VERSION + "="
                + majorVersion + "&" + CommonDefine.RestArg.FILE_MINOR_VERSION + "=" + minorVersion;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public long countBucket(BSONObject condition) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + "?action=count_bucket&"
                + CommonDefine.RestArg.FILTER + "=" + encodeCondition(condition);
        HttpGet request = new HttpGet(uri);
        String countHeader = RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId,
                request, CommonDefine.RestArg.X_SCM_COUNT);
        return Long.parseLong(countHeader);
    }

    @Override
    public void setBucketVersionStatus(String bucketName, ScmBucketVersionStatus status)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName)
                + "?action=update_version_status&" + CommonDefine.RestArg.BUCKET_VERSION_STATUS
                + "=" + status.name();
        HttpPut request = new HttpPut(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void setDefaultRegion(String s3ServiceName, String wsName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + "/" + s3ServiceName.toLowerCase()
                + "/region?Action=SetDefaultRegion&workspace=" + encode(wsName);
        HttpPut request = new HttpPut(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void deleteFileVersion(String wsName, String fileId, int majorVersion, int minorVersion)
            throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + FILE + fileId + "?workspace_name="
                + encode(wsName) + "&action=" + CommonDefine.RestArg.ACTION_DELETE_VERSION + "&"
                + CommonDefine.RestArg.FILE_MAJOR_VERSION + "=" + majorVersion + "&"
                + CommonDefine.RestArg.FILE_MINOR_VERSION + "=" + minorVersion;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public String getDefaultRegion(String s3ServiceName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + "/" + s3ServiceName.toLowerCase()
                + "/region?Action=GetDefaultRegion";
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithHeaderResponse(getHttpClient(), sessionId, request,
                "default-region");
    }

    @Override
    public BSONObject refreshAccesskey(String targetUser, String password, String accesskey,
            String secretkey) throws ScmException {
        String uri = URL_PREFIX + pureUrl + AUTH + API_VERSION + ACCESSKEY + "?action=refresh";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", targetUser));
        if (Strings.hasText(password)) {
            password = encrypt(password);
            params.add(new BasicNameValuePair("password", password));
        }
        if (Strings.hasText(accesskey)) {
            params.add(new BasicNameValuePair("accesskey", accesskey));
        }
        if (Strings.hasText(secretkey)) {
            secretkey = encrypt(secretkey);
            params.add(new BasicNameValuePair("secretkey", secretkey));
        }
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public String getHealthStatus(String url, String healthPath) throws ScmException {
        int idx = url.indexOf(URL_SEP);
        if (-1 != idx) {
            url = url.substring(0, idx);
        }
        String uri = URL_PREFIX + url + healthPath;
        HttpGet request = new HttpGet(uri);
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        return BsonUtils.getStringChecked(resp, "status");
    }

    @Override
    public BSONObject listTrace(Long minDuration, String serviceName, Date startTime, Date endTime,
            Map<String, String> queryCondition, int limit) throws ScmException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.appendURL(URL_PREFIX, pureUrl, SERVICE_TRACE, API_VERSION, "traces");
        uriBuilder.appendParam("limit", String.valueOf(limit));
        if (serviceName != null) {
            uriBuilder.appendParam("serviceName", serviceName);
        }
        if (minDuration != null) {
            uriBuilder.appendParam("minDuration", String.valueOf(minDuration));
        }
        if (endTime != null) {
            uriBuilder.appendParam("endTs", String.valueOf(endTime.getTime()));
        }
        if (startTime != null) {
            if (endTime == null) {
                endTime = new Date();
            }
            uriBuilder.appendParam("lookback",
                    String.valueOf(endTime.getTime() - startTime.getTime()));
        }
        if (queryCondition != null) {
            Set<Map.Entry<String, String>> entries = queryCondition.entrySet();
            StringBuilder condition = new StringBuilder();
            int count = 0;
            for (Map.Entry<String, String> entry : entries) {
                count++;
                condition.append(entry.getKey());
                if (entry.getValue() != null) {
                    condition.append("=").append(entry.getValue());
                }
                if (count < entries.size()) {
                    condition.append(" and ");
                }
            }
            uriBuilder.appendParam("annotationQuery", condition.toString());
        }
        HttpGet request = new HttpGet(uriBuilder.toString());
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        return resp;
    }

    @Override
    public BSONObject getTrace(String traceId) throws ScmException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.appendURL(URL_PREFIX, pureUrl, SERVICE_TRACE, API_VERSION, "trace/", traceId);
        HttpGet request = new HttpGet(uriBuilder.toString());
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request);
        return resp;
    }

    @Override
    public void setGlobalLifeCycleConfig(BSONObject lifeCycleConfigBSON) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG;
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG,
                lifeCycleConfigBSON.toString()));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void deleteGlobalLifeCycleConfig() throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getGlobalLifeCycleConfig() throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void addGlobalStageTag(String name, String desc) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + STAGE_TAG;
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_NAME, name));
        params.add(new BasicNameValuePair(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_DESC, desc));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void removeGlobalStageTag(String name) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + STAGE_TAG + name;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void addGlobalTransition(BSONObject transitionBSON) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + TRANSITION;
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FieldName.LifeCycleConfig.FIELD_TRANSITION,
                transitionBSON.toString()));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void removeGlobalTransition(String name) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + TRANSITION + name;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public void updateGlobalTransition(String name, BSONObject transitionBSON) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + TRANSITION + name;
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FieldName.LifeCycleConfig.FIELD_TRANSITION,
                transitionBSON.toString()));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject getGlobalTransition(String name) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + TRANSITION + name + "?action=find_transition";
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject listWorkspaceByTransitionName(String name) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + TRANSITION + name + "?action=list_workspace";
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject listTransition(String name) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + TRANSITION;
        if (name != null) {
            uri += "?flow_stage_tag_name=" + encode(name);
        }
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void setSiteStageTag(String siteName, String stageTagName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG + SITE
                + STAGE_TAG + siteName;
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FieldName.FIELD_CLSITE_STAGE_TAG, stageTagName));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void alterSiteStageTag(String siteName, String stageTagName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG + SITE
                + STAGE_TAG + siteName;
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(FieldName.FIELD_CLSITE_STAGE_TAG, stageTagName));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void unsetSiteStageTag(String siteName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG + SITE
                + STAGE_TAG + siteName;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject getSiteStageTag(String siteName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG + SITE
                + STAGE_TAG + siteName;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject applyTransition(String workspace, String transitionName,
            ScmLifeCycleTransition transition, String preferredRegion, String preferredZone)
            throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + WORKSPACE + workspace;
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("transition_name", transitionName));
        if (transition != null) {
            BSONObject obj = transition.toBSONObject();
            params.add(new BasicNameValuePair(FieldName.LifeCycleConfig.FIELD_TRANSITION,
                    obj.toString()));
        }
        params.add(new BasicNameValuePair(RestDefine.RestKey.PREFERRED_REGION, preferredRegion));
        params.add(new BasicNameValuePair(RestDefine.RestKey.PREFERRED_ZONE, preferredZone));
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void removeWsTransition(String workspace, String transitionName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + WORKSPACE + encode(workspace) + "?transition_name=" + transitionName;
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }

    @Override
    public BSONObject updateWsTransition(String workspace, String transitionName,
            BSONObject transitionBSON, String preferredRegion, String preferredZone)
            throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + WORKSPACE + workspace;
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("transition_name", transitionName));
        params.add(new BasicNameValuePair(FieldName.LifeCycleConfig.FIELD_TRANSITION,
                transitionBSON.toString()));
        if (preferredRegion != null) {
            params.add(
                    new BasicNameValuePair(RestDefine.RestKey.PREFERRED_REGION, preferredRegion));
        }
        if (preferredZone != null) {
            params.add(new BasicNameValuePair(RestDefine.RestKey.PREFERRED_ZONE, preferredZone));
        }
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject getWsTransition(String workspace, String transitionName) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + WORKSPACE + workspace + "?action=find_transition" + "&transition_name="
                + transitionName;
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public void updateWsTransitionStatus(String workspace, String transitionName, Boolean status)
            throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + WORKSPACE + workspace + "?action=update_status";
        HttpPut request = new HttpPut(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("status", status.toString()));
        params.add(new BasicNameValuePair("transition_name", transitionName));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public BSONObject listWsTransition(String workspace) throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG
                + WORKSPACE + workspace + "?action=list_transition";
        HttpGet request = new HttpGet(uri);
        return RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId, request);
    }

    @Override
    public ScmId startOnceTransition(String workspaceName, BSONObject condition, int scope,
            long maxExecTime, String source, String dest, String dataCheckLevel, boolean quickStart,
            boolean isRecycleSpace, String type, String preferredRegion, String preferredZone)
            throws ScmException {
        String uri = URL_PREFIX + pureUrl + SCHEDULE_SERVER + API_VERSION + LIFE_CYCLE_CONFIG + TASK
                + "onceTransition";
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("workspace_name", workspaceName));
        if (ScheduleType.getType(type) == ScheduleType.MOVE_FILE) {
            params.add(new BasicNameValuePair("task_type",
                    String.valueOf(CommonDefine.TaskType.SCM_TASK_MOVE_FILE)));
        }
        else if (ScheduleType.getType(type) == ScheduleType.COPY_FILE) {
            params.add(new BasicNameValuePair("task_type",
                    String.valueOf(CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE)));
        }
        else {
            throw new ScmInvalidArgumentException("unsupported this task type,type=" + type);
        }

        BSONObject options = new BasicBSONObject();
        options.put("filter", condition);
        options.put(CommonDefine.RestArg.TASK_SCOPE, scope);
        options.put(CommonDefine.RestArg.TASK_MAX_EXEC_TIME, maxExecTime);
        options.put(CommonDefine.RestArg.TASK_DATA_CHECK_LEVEL, dataCheckLevel);
        options.put(CommonDefine.RestArg.TASK_QUICK_START, quickStart);
        options.put(CommonDefine.RestArg.TASK_IS_RECYCLE_SPACE, isRecycleSpace);
        params.add(new BasicNameValuePair("options", options.toString()));
        params.add(new BasicNameValuePair("source", source));
        params.add(new BasicNameValuePair("dest", dest));
        params.add(new BasicNameValuePair(RestDefine.RestKey.PREFERRED_REGION, preferredRegion));
        params.add(new BasicNameValuePair(RestDefine.RestKey.PREFERRED_ZONE, preferredZone));
        BSONObject resp = RestClient.sendRequestWithJsonResponse(getHttpClient(), sessionId,
                request, params);
        BSONObject task = (BSONObject) resp.get("task");
        return new ScmId((String) task.get("id"), false);
    }

    private String encrypt(String str) throws ScmException {
        try {
            return ScmPasswordMgr.getInstance().encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, str);
        }
        catch (Exception e) {
            throw new ScmSystemException("Failed to encrypt", e);
        }
    }

    @Override
    public void setBucketTag(String bucketName, Map<String, String> customTag) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName) + "/tags";
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        Gson gson = new GsonBuilder().serializeNulls().create();
        params.add(new BasicNameValuePair(CommonDefine.RestArg.BUCKET_CUSTOM_TAG,
                gson.toJson(customTag)));
        RestClient.sendRequest(getHttpClient(), sessionId, request, params);
    }

    @Override
    public void deleteBucketTag(String bucketName) throws ScmException {
        String uri = URL_PREFIX + url + API_VERSION + BUCKETS + encode(bucketName) + "/tags";
        HttpDelete request = new HttpDelete(uri);
        RestClient.sendRequest(getHttpClient(), sessionId, request);
    }
}
