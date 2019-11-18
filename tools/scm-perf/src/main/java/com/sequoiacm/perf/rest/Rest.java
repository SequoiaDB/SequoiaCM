package com.sequoiacm.perf.rest;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.ConfigException;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.tool.Utils;
import com.sequoiacm.perf.vo.AuthVo;
import com.sequoiacm.perf.vo.FileVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;

public class Rest {

    private static final Logger logger = LoggerFactory.getLogger(Rest.class);

    private static final String LOGIN_API = "/api/v1/login";
    private static final String LOGOUT_API = "/api/v1/logout";
    private static final String FILE_API = "/api/v1/files";
    private static final String AUTHORIZATION = "Authorization";

    private static final String HTTP = "http";
    private static final String HTTP_URL_HEADER = "http://";
    private static final String BACKSLASH = "/";


    private final String loginUrl;
    private final String logoutUrl;
    private final String fileUrl;

    private String url;
    private String user;
    private String password;

    private RestTemplate rest;

    {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(10000);
        factory.setConnectionRequestTimeout(10000);
        factory.setReadTimeout(10000);
        rest = new RestTemplate(factory);
    }

    public Rest(String url, String user, String password) {
        url = url.trim();
        if (!url.startsWith(HTTP)) {
            url = HTTP_URL_HEADER.concat(url);
        }
        if (url.endsWith(BACKSLASH)) {
            url = url.substring(0, url.length() - 1);
        }
        this.url = url;
        this.user = user;
        this.password = Utils.md5(password);
        loginUrl = url.concat(LOGIN_API);
        logoutUrl = url.concat(LOGOUT_API);
        fileUrl = url.concat(FILE_API);
    }


    private String login() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(2);
        params.set("user", user);
        params.set("passwd", password);
        AuthVo authVo = rest.postForObject(loginUrl, params, AuthVo.class);
        return authVo.getAccessToken();
    }

    private void logout(String token) {
        HttpHeaders requestHeader = new HttpHeaders();
        requestHeader.add(AUTHORIZATION, "Scm " + token);
        HttpEntity<String> request = new HttpEntity<>(null, requestHeader);
        String resp = rest.postForObject(logoutUrl, request, String.class);
    }

    public String upload(String workspace, final String fileName, byte[] bytes) throws ScmException {
        String token = login();
        try {
            HttpHeaders requestHeader = new HttpHeaders();
            requestHeader.setContentType(MediaType.MULTIPART_FORM_DATA);

            requestHeader.add(AUTHORIZATION, "Scm " + token);

            MultiValueMap<String, Object> params = new LinkedMultiValueMap<>(4);
            params.add("workspace_name", workspace);
            params.add("description", "{\"title\":\"rest template\",\"author\":\"\"}");

            if (bytes != null) {
                params.add("file", new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                });

            } else {
                File file = new File(fileName);
                if (!file.isFile()) {
                    throw new ConfigException("fileReadPath is an invalid file path");
                }
                FileSystemResource resource = new FileSystemResource(file);
                params.add("file", resource);

            }

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(params, requestHeader);

            ResponseEntity<FileVo> exchange = rest.postForEntity(fileUrl, httpEntity, FileVo.class);
            FileVo body = exchange.getBody();
            return body.getFileId();
        } finally {
            logout(token);
        }
    }

    public void download(String workspace, String fileId, OutputStream fileStream) throws ScmException {
        String token = login();
        InputStream in = null;
        try {
            HttpHeaders requestHeader = new HttpHeaders();
            requestHeader.add(AUTHORIZATION, "Scm " + token);
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(null, requestHeader);
            String url = fileUrl + BACKSLASH + fileId + "?workspace_name=" + workspace;
            ResponseEntity<byte[]> response = rest.exchange(url, HttpMethod.GET, httpEntity, byte[].class);
            byte[] bytes = response.getBody();

            in = new ByteArrayInputStream(bytes);
            int len;
            byte[] buffer = new byte[1024];
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                fileStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            logout(token);
        }
    }


}