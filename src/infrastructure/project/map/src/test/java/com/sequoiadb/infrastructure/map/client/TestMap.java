package com.sequoiadb.infrastructure.map.client;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.core.ScmMapFactory;
import com.sequoiadb.infrastructure.map.client.model.ScmMap;
import com.sequoiadb.infrastructure.map.client.model.ScmMapGroup;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClient;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClientFactory;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { App.class })
@Component
public class TestMap {
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
            group.deleteMap("test");
        }
        catch (ScmMapServerException e) {
        }
    }

    @After
    public void after() throws Exception {
        try {
            // clear
            ScmMapGroup group = ScmMapFactory.getGroupMap(getClient(), "S3");
            group.deleteMap("test");
        }
        catch (ScmMapServerException e) {
        }
    }

    @Test
    public void testMapTable() throws Exception {
        ScmMapGroup group = ScmMapFactory.getGroupMap(getClient(), "S3");
        // create
        group.<String, BSONObject> createMap("test", String.class, BSONObject.class);

        // get
        ScmMap<String, BSONObject> scmMap = (ScmMap<String, BSONObject>) group
                .<String, BSONObject> getMap("test");
        assertEquals(scmMap.getName(), "test");
        assertEquals(scmMap.getkeyType(), String.class);
        assertEquals(scmMap.getValueType(), BSONObject.class);

        // delete
        group.deleteMap("test");

        try {
            // check delete
            ScmMap<String, BSONObject> map2 = (ScmMap<String, BSONObject>) group
                    .<String, BSONObject> getMap("test");
            throw new Exception("get map failed");
        }
        catch (ScmMapServerException e) {
            if (e.getError() != ScmMapError.MAP_TABLE_NOT_EXIST) {
                throw new Exception("exception error", e);
            }
        }
    }

    @Test
    public void testMap() throws Exception {
        ScmMapGroup group = ScmMapFactory.getGroupMap(getClient(), "S3");
        ScmMap<String, BSONObject> scmMap = (ScmMap<String, BSONObject>) group
                .<String, BSONObject> createMap("test", String.class, BSONObject.class);
        String[] keys = { "1", "2", "3", "4", "5" };
        String[] values1 = { "ws1", "ws2", "ws3", "ws4", "ws5" };
        String[] values2 = { "d1", "d2", "d3", "d4", "d5" };

        // put
        assertEquals(null, scmMap.put("", new BasicBSONObject()));

        // putALL
        Map<String, BSONObject> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            BSONObject bson = new BasicBSONObject();
            bson.put("wsName", values1[i]);
            bson.put("dir", values2[i]);
            map.put(keys[i], bson);
        }
        scmMap.putAll(map);

        // count:size,isEmpty,containsKey,containKeySet
        assertEquals(keys.length + 1, scmMap.size());
        assertEquals(false, scmMap.isEmpty());
        assertEquals(true, scmMap.containsKey(keys[keys.length - 1]));
        assertEquals(false, scmMap.containsKey("14545"));
        assertEquals(true, scmMap.containKeySet(Arrays.asList(keys)));
        assertEquals(false, scmMap.containKeySet(Arrays.asList(values1)));

        // overwrite put
        assertEquals(values1[0], (String) scmMap.put(keys[0], new BasicBSONObject()).get("wsName"));

        // remove
        assertEquals(false, scmMap.remove("").containsField("wsName"));
        assertEquals(null, scmMap.get(""));

        // removeAll
        assertEquals(true, scmMap.removeKeySet(Arrays.asList(keys)));
        assertEquals(0, scmMap.size());

        System.out.println(scmMap.toString());
        System.out.println(1);

        for (int i = 0; i < keys.length; i++) {
            BSONObject bson = new BasicBSONObject();
            bson.put("wsName", values1[i]);
            bson.put("dir", values2[i]);
            scmMap.put(keys[i], bson);
        }

        // keySet
        Set<String> scmSet = scmMap.keySet();
        List<String> keyList = Arrays.asList(keys);
        assertEquals(keyList.size(), scmSet.size());
        assertEquals(true, keyList.containsAll(scmSet));

        System.out.println(2);
        // entrySet
        Set<Entry<String, BSONObject>> scmEntrySet = scmMap.entrySet();
        assertEquals(keyList.size(), scmEntrySet.size());
        List<String> values1List = Arrays.asList(values1);
        List<String> values2List = Arrays.asList(values2);
        for (Entry<String, BSONObject> entry : scmEntrySet) {
            assertEquals(true, keyList.contains(entry.getKey()));
            String v1 = (String) entry.getValue().get("wsName");
            String v2 = (String) entry.getValue().get("dir");
            assertEquals(true, values1List.contains(v1));
            assertEquals(true, values2List.contains(v2));
        }

        System.out.println(3);
        // clear
        scmMap.clear();
        assertEquals(0, scmMap.size());
        assertEquals(null, scmMap.put(null, new BasicBSONObject()));
        assertEquals(0, scmMap.get(null).keySet().size());
        group.deleteMap("test");

    }

    @Test
    public void testSet() throws Exception {
        ScmMapGroup group = ScmMapFactory.getGroupMap(getClient(), "S3");
        ScmMap<String, BSONObject> scmMap = (ScmMap<String, BSONObject>) group
                .<String, BSONObject> createMap("test", String.class, BSONObject.class);
        String[] keys = { "1", "2", "3", "4", "5" };
        String[] values1 = { "ws1", "ws2", "ws3", "ws4", "ws5" };
        String[] values2 = { "d1", "d2", "d3", "d4", "d5" };

        Map<String, BSONObject> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            BSONObject bson = new BasicBSONObject();
            bson.put("wsName", values1[i]);
            bson.put("dir", values2[i]);
            map.put(keys[i], bson);
        }
        scmMap.putAll(map);

        List<String> keyList = Arrays.asList(keys);
        List<String> values1List = Arrays.asList(values1);
        List<String> values2List = Arrays.asList(values2);

        // keySet
        Set<String> scmSet = scmMap.keySet();
        assertEquals(keyList.size(), scmSet.size());
        assertEquals(true, keyList.containsAll(scmSet));

        // entrySet
        Set<Entry<String, BSONObject>> scmEntrySet = scmMap.entrySet();
        assertEquals(keyList.size(), scmEntrySet.size());
        for (Entry<String, BSONObject> entry : scmEntrySet) {
            assertEquals(true, keyList.contains(entry.getKey()));
            String v1 = (String) entry.getValue().get("wsName");
            String v2 = (String) entry.getValue().get("dir");
            assertEquals(true, values1List.contains(v1));
            assertEquals(true, values2List.contains(v2));
        }

        // add addAll
        try {
            scmSet.add("ddd");
            throw new Exception("add can not success");
        }
        catch (Exception e) {

        }
        try {
            scmSet.addAll(values1List);
            throw new Exception("add can not success");
        }
        catch (Exception e) {

        }

        // remove
        assertEquals(true, scmSet.remove(keys[0]));
        assertEquals(false, scmSet.remove(""));

        // removeALL
        scmMap.putAll(map);
        scmSet = scmMap.keySet();
        System.err.println(scmMap.keySet());
        System.err.println(map.keySet());
        System.err.println(scmSet);
        System.err.println(values1List);
        assertEquals(false, scmSet.removeAll(values1List));
        assertEquals(keys.length, scmSet.size());
        assertEquals(true, scmSet.removeAll(keyList));
        assertEquals(0, scmSet.size());

        // retainAll
        scmMap.putAll(map);
        scmSet = scmMap.keySet();
        assertEquals(false, scmSet.retainAll(keyList));
        assertEquals(keys.length, scmSet.size());
        assertEquals(true, scmSet.retainAll(values1List));
        assertEquals(0, scmSet.size());

        // iterator
        scmMap.putAll(map);
        scmSet = scmMap.keySet();
        Iterator<String> it = scmSet.iterator();
        while (it.hasNext()) {
            String key = it.next();
            assertEquals(true, keyList.contains(key));
            it.remove();
            assertEquals(null, scmMap.get(key));
        }
        assertEquals(0, scmSet.size());
        assertEquals(0, scmMap.size());
        group.deleteMap("test");
    }
}
