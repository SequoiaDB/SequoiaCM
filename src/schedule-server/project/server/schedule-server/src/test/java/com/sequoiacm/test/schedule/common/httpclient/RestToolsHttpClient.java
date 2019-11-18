package com.sequoiacm.test.schedule.common.httpclient;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.test.schedule.common.RestTestUtils;
import com.sequoiacm.test.schedule.common.RestTools;

public class RestToolsHttpClient extends RestTools {
    private static final String LOGIN_API = "/api/v1/login";
    private static final String GETNAME_URL = "/api/v1/name";
    private static final String CREATE_SCHEDULE_URL = "/api/v1/schedules";
    private static final String GET_SCHEDULE_URL = "/api/v1/schedules";
    private static final String DELETE_SCHEDULE_URL = "/api/v1/schedules";

    CloseableHttpClient client;

    public RestToolsHttpClient(String rootUrl) {
        super(rootUrl);
    }

    private void displayHeader(Header[] headers) {
        for (Header h : headers) {
            try {
                System.out.println(h.getName() + ":" + URLDecoder.decode(h.getValue(), "utf-8"));
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String login(String user, String passwd) {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + LOGIN_API);
            URI uri = builder.build();
            HttpPost post = new HttpPost(uri);
            UrlEncodedFormEntity entity = new UrlEncodedFormEntityBuilder()
            .addNameValue("user", user).addNameValue("passwd", RestTestUtils.md5(passwd))
            .build();
            post.setEntity(entity);
            response = client.execute(post);
            System.out.println(response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            return EntityUtils.toString(response.getEntity());
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }

        return "";
    }

    private void close(Closeable handler) {
        if (null == handler) {
            return;
        }

        try {
            handler.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName(String value) {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {

            URIBuilder builder = new URIBuilder(getRootUrl() + GETNAME_URL);
            URI uri = builder.build();
            HttpHead head = new HttpHead(uri);
            head.setHeader("name", value);
            response = client.execute(head);
            System.out.println(response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            return EntityUtils.toString(response.getEntity());
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }

        return "";
    }

    @Override
    public void createSchedule(String name, String desc, String type, BSONObject content,
            String workspace, String cron) {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + CREATE_SCHEDULE_URL);
            URI uri = builder.build();
            HttpPost post = new HttpPost(uri);
            BSONObject info = new BasicBSONObject();
            info.put(FieldName.Schedule.FIELD_NAME, name);
            info.put(FieldName.Schedule.FIELD_DESC, desc);
            info.put(FieldName.Schedule.FIELD_WORKSPACE, workspace);
            info.put(FieldName.Schedule.FIELD_TYPE, type);
            info.put(FieldName.Schedule.FIELD_CRON, cron);
            info.put(FieldName.Schedule.FIELD_CONTENT, content);

            UrlEncodedFormEntity entity = new UrlEncodedFormEntityBuilder().addNameValue(
                    RestCommonDefine.RestParam.KEY_DESCRIPTION, info.toString()).build();
            post.setEntity(entity);
            response = client.execute(post);
            System.out.println("status code:" + response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            System.out.println("entity:" + EntityUtils.toString(response.getEntity()));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }
    }

    @Override
    public void getSchedule(String scheduleId) {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + GET_SCHEDULE_URL + "/" + scheduleId);
            URI uri = builder.build();
            HttpGet get = new HttpGet(uri);
            response = client.execute(get);
            System.out.println("status code:" + response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            System.out.println("entity:" + EntityUtils.toString(response.getEntity()));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }
    }

    @Override
    public void listSchedule() {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + GET_SCHEDULE_URL);
            URI uri = builder.build();
            HttpGet get = new HttpGet(uri);
            response = client.execute(get);
            System.out.println("status code:" + response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            System.out.println("entity:" + EntityUtils.toString(response.getEntity()));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }
    }

    @Override
    public void deleteSchedule(String scheduleId) {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + DELETE_SCHEDULE_URL + "/"
                    + scheduleId);
            URI uri = builder.build();
            HttpDelete get = new HttpDelete(uri);
            response = client.execute(get);
            System.out.println("status code:" + response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            System.out.println("entity:" + EntityUtils.toString(response.getEntity()));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }
    }

    @Override
    public int getVersion() {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + "/api/v1/privileges");
            URI uri = builder.build();
            HttpGet get = new HttpGet(uri);
            response = client.execute(get);
            System.out.println("status code:" + response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            System.out.println("entity:" + EntityUtils.toString(response.getEntity()));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }

        return 0;
    }

    @Override
    public void listPrivileges() {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + "/api/v1/privileges/relations");
            URI uri = builder.build();
            HttpGet get = new HttpGet(uri);
            response = client.execute(get);
            System.out.println("status code:" + response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            System.out.println("entity:" + EntityUtils.toString(response.getEntity()));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }
    }

    @Override
    public void listUsers() {
        client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(getRootUrl() + "/api/v1/users");
            URI uri = builder.build();
            HttpGet get = new HttpGet(uri);
            response = client.execute(get);
            System.out.println("status code:" + response.getStatusLine().getStatusCode());

            displayHeader(response.getAllHeaders());
            System.out.println("entity:" + EntityUtils.toString(response.getEntity()));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(response);
            close(client);
        }
    }

}
