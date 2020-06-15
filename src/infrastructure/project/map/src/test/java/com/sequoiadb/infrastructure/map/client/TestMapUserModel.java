package com.sequoiadb.infrastructure.map.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.core.ScmMapFactory;
import com.sequoiadb.infrastructure.map.client.model.ScmMap;
import com.sequoiadb.infrastructure.map.client.model.ScmMapGroup;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClient;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClientFactory;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { App.class })
@Component
public class TestMapUserModel {
    @Autowired
    private MapFeignClientFactory clientFactory;

    private MapFeignClient getClient() {
        return clientFactory.getFeignClientByServiceName("rootsite");
    }

    @Before
    public void before() throws Exception {
        try {
            // clear
            ScmMapGroup group = ScmMapFactory.getGroupMap(getClient(), "S3");
            group.deleteMap("test1");
        }
        catch (ScmMapServerException e) {
        }
    }

    @After
    public void after() throws Exception {
        try {
            // clear
            ScmMapGroup group = ScmMapFactory.getGroupMap(getClient(), "S3");
            group.deleteMap("test1");
        }
        catch (ScmMapServerException e) {
        }
    }

    @Test
    public void testTest() throws Exception {
        ScmMapGroup group = ScmMapFactory.getGroupMap(getClient(), "S3");
        ScmMap<String, UserModel> scmMap;
        try {
            scmMap = (ScmMap<String, UserModel>) group.<String, UserModel> createMap("test1",
                    String.class, UserModel.class);
        }
        catch (Exception e) {
            scmMap = (ScmMap<String, UserModel>) group.<String, UserModel> getMap("test1");
        }
        String[] keys = { "1", "2", "3" };
        UserModel[] users = { new UserModel("ss", "lisi", 15), new UserModel("ss1", "lisi1", 15),
                new UserModel("ss2", "lisi2", 16) };
        // String[] values1 = { "ws1", "ws2", "ws3", "ws4", "ws5" };
        // String[] values2 = { "d1", "d2", "d3", "d4", "d5" };
        //
        Map<String, UserModel> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], users[i]);
        }
        // // putALL
        scmMap.putAll(map);
        scmMap.put(null, null);
        scmMap.put("dd", new UserModel("ss2", "lisi2", 16));

        Iterator<Entry<String, UserModel>> it = scmMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, UserModel> entry = it.next();
            System.out.println(entry.getKey());
            UserModel user = entry.getValue();
            System.out.println(user);
        }
        group.deleteMap("test1");
    }

}
