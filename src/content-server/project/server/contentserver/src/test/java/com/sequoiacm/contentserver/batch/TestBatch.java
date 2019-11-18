package com.sequoiacm.contentserver.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestBatch {
    
    private String token;
    
    @Before
    public void setUp() throws ClientProtocolException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://192.168.20.93:15001/api/v1/login");
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("user", "user"));
        params.add(new BasicNameValuePair("passwd", "76a2173be6393254e72ffa4d6df1030a"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        
        CloseableHttpResponse response = httpClient.execute(post);
        String ret = EntityUtils.toString(response.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(ret, Map.class);
        token = map.get("access_token");
        System.out.println(ret);
    }
    
    @Test
    public void testCreate() throws ClientProtocolException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost put = new HttpPost("http://192.168.20.93:15001/api/v1/batches");
        
        put.setHeader("Authorization", "Scm " + token);
        
        String desp = "{\"name\":\"batch-test2\",\"properties\":{\"k1\":\"v1\"},\"tags\":{\"k1\":\"v1\"}}";
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("workspace_name", "ws_default"));
        params.add(new BasicNameValuePair("description", desp));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        put.setEntity(entity);
        
        System.out.println(put.getRequestLine());
        
        CloseableHttpResponse response = httpClient.execute(put);
        String ret = EntityUtils.toString(response.getEntity());
        System.out.println(ret);
    }
    
    // TODO re-test
    @Test
    public void testList() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        String filter = "{\"name\":\"batch-test\"}";
        URIBuilder uriBuilder = new URIBuilder("http://192.168.20.93:15001/api/v1/batches");
        uriBuilder.setParameter("workspace_name", "ws_default");
        uriBuilder.setParameter("filter", filter);
        HttpGet get = new HttpGet(uriBuilder.build());
        
        get.setHeader("Authorization", "Scm " + token);
        
        CloseableHttpResponse response = httpClient.execute(get);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    
    @Test
    public void testGetBatch() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        String batchId = "5ac1f3a200000100002b0005";
        URIBuilder uriBuilder = new URIBuilder("http://192.168.20.93:15001/api/v1/batches/" + batchId);
        uriBuilder.setParameter("workspace_name", "ws_default");
        
        HttpHead head = new HttpHead(uriBuilder.build());
        head.setHeader("Authorization", "Scm " + token);
        
        CloseableHttpResponse response = httpClient.execute(head);
//        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    
    @Test
    public void testAttachFile() throws ClientProtocolException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        String batchId = "5ac1f3a200000100002b0005";
        String fileId = "5ac1de7e00000100002c0006";
        HttpPost post = new HttpPost("http://192.168.20.93:15101/api/v1/batches/" + batchId + "/attachfile");
        post.setHeader("Authorization", "Scm " + token);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("workspace_name", "ws_default"));
        params.add(new BasicNameValuePair("file_id", fileId));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        
        CloseableHttpResponse response = httpClient.execute(post);
        String ret = EntityUtils.toString(response.getEntity());
        System.out.println(ret);
    }
    
    @Test
    public void testDetachFile() throws ClientProtocolException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        String batchId = "5ac1f3a200000100002b0005";
        String fileId = "5ac1de7e00000100002c0006";
        HttpPost post = new HttpPost("http://192.168.20.93:15001/api/v1/batches/" + batchId + "/detachfile");
        post.setHeader("Authorization", "Scm " + token);
        
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("workspace_name", "ws_default"));
        params.add(new BasicNameValuePair("file_id", fileId));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        
        CloseableHttpResponse response = httpClient.execute(post);
        String ret = EntityUtils.toString(response.getEntity());
        System.out.println(ret);
    }
    
    @Test
    public void testUpdate() throws ClientProtocolException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String batchId = "5ac1f3a200000100002b0005";
        HttpPut put = new HttpPut("http://192.168.20.93:15001/api/v1/batches/" + batchId);
        
        put.setHeader("Authorization", "Scm " + token);
        
        String props = "{\"name\":\"batch-new2\", \"asd\":\"asd\"}";
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("workspace_name", "ws_default"));
        params.add(new BasicNameValuePair("props", props));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        put.setEntity(entity);
        
        CloseableHttpResponse response = httpClient.execute(put);
        String ret = EntityUtils.toString(response.getEntity());
        System.out.println(ret);
    }
    
    @Test
    public void testGetFile() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String fileId = "5ac1de7e00000100002c0006";
        
        URIBuilder uriBuilder = new URIBuilder("http://192.168.20.93:15001/api/v1/files/" + fileId);
        uriBuilder.setParameter("workspace_name", "ws_default");
        
        HttpHead head = new HttpHead(uriBuilder.build());
        head.setHeader("Authorization", "Scm " + token);
        
        CloseableHttpResponse response = httpClient.execute(head);
        String value = response.getLastHeader("file").getValue();
        System.out.println(value);
    }
    
    @Test
    public void testCreateFile() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String batchId = "5ac1de7e00000100002c0006";
        
        URIBuilder uriBuilder = new URIBuilder("http://192.168.20.93:15001/api/v1/files/" + batchId);
        uriBuilder.setParameter("workspace_name", "ws_default");
        
        HttpHead head = new HttpHead(uriBuilder.build());
        head.setHeader("Authorization", "Scm " + token);
        
        CloseableHttpResponse response = httpClient.execute(head);
//        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    @After
    public void tearDown() throws ClientProtocolException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://192.168.20.93:15001/api/v1/logout");
        post.setHeader("Authorization", "Scm " + token);
        
        CloseableHttpResponse response = httpClient.execute(post);
        String ret = EntityUtils.toString(response.getEntity());
        System.out.println(ret);
    }
    
    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        /*String filter = "{\"name\":\"batch-test\",\"properties\":{\"k1\":\"v1\"},\"tags\":{\"k1\":\"v1\"}}";
        String decode = URLDecoder.decode(filter, "UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.readValue(decode, Map.class);
        BasicBSONObject obj = new BasicBSONObject(map);
        Object object = obj.get("properties");
        
        System.out.println(obj instanceof Map);
        System.out.println(map);*/
        
        Date date = new Date();
        date.setTime(1522660258709l);
        System.out.println(date);
        
        
    }
}
