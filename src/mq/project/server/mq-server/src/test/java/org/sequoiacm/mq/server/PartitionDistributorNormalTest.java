package org.sequoiacm.mq.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.server.common.PartitionDistibutor;


// 一般场景测试
public class PartitionDistributorNormalTest {

    @Test
    public void removePartition() {
        List<ConsumerPartitionInfo> partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c3"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c3"));
        PartitionDistibutor pd = new PartitionDistibutor(partitionInfos);
        // 删除3号分区，不需调整
        Map<Integer, String> ret = pd.removePartition(Arrays.asList(3));
        Assert.assertTrue(ret.size() == 0);

        partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c3"));
        partitionInfos.add(new ConsumerPartitionInfo(4, "c4"));
        pd = new PartitionDistibutor(partitionInfos);
        // 删除4号分区，c2需要给一个分区给c4
        ret = pd.removePartition(Arrays.asList(4));
        Assert.assertTrue(ret.size() == 1);
        Assert.assertTrue(ret.values().iterator().next().equals("c4"));
        Integer p = ret.keySet().iterator().next();
        Assert.assertTrue(p == 1 || p == 2);

        partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c3"));
        partitionInfos.add(new ConsumerPartitionInfo(4, "c4"));
        pd = new PartitionDistibutor(partitionInfos);
        // 删除3、4号分区，c2需要给一个分区给c4或c3
        ret = pd.removePartition(Arrays.asList(3, 4));
        Assert.assertTrue(ret.size() == 1, ret.toString());
        String consmer = ret.values().iterator().next();
        Assert.assertTrue(consmer.equals("c4") || consmer.equals("c3"));
        p = ret.keySet().iterator().next();
        Assert.assertTrue(p == 1 || p == 2);
    }

    @Test
    public void addPartition() {
        List<ConsumerPartitionInfo> partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        PartitionDistibutor pd = new PartitionDistibutor(partitionInfos);

        // 添加两个分区，每个分区分别分配c1, c2
        Map<Integer, String> ret = pd.addPartition(Arrays.asList(2, 3));
        Assert.assertEquals(ret.size(), 2);
        Assert.assertTrue(ret.containsKey(2) && ret.containsKey(3));
        Assert.assertTrue(!ret.get(2).equals(ret.get(3)));
        Assert.assertTrue(Arrays.asList("c1", "c2").contains(ret.get(2)));
        Assert.assertTrue(Arrays.asList("c1", "c2").contains(ret.get(3)));

        partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c3"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c3"));
        pd = new PartitionDistibutor(partitionInfos);

        // 添加两个分区，分区分别分配给c1,c2
        ret = pd.addPartition(Arrays.asList(4, 5));
        Assert.assertEquals(ret.size(), 2);
        Assert.assertTrue(ret.containsKey(4) && ret.containsKey(5));
        Assert.assertTrue(!ret.get(4).equals(ret.get(5)));
        Assert.assertTrue(Arrays.asList("c1", "c2").contains(ret.get(4)));
        Assert.assertTrue(Arrays.asList("c1", "c2").contains(ret.get(5)));
    }

    @Test
    public void rleasePartition() {
        List<ConsumerPartitionInfo> partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c3"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c4"));
        partitionInfos.add(new ConsumerPartitionInfo(4, "c5"));

        PartitionDistibutor pd = new PartitionDistibutor(partitionInfos);

        // c1释放分区, 分配给剩余消费者
        // c2 : 1, 0
        // c3 : 2
        // c4 : 3
        // c5 : 4
        Map<Integer, String> ret = pd.rleasePartition("c1");
        Assert.assertEquals(ret.size(), 1);
        Integer p0 = ret.keySet().iterator().next();
        Assert.assertEquals(p0.intValue(), 0);
        String p0Consumer = ret.values().iterator().next();
        Assert.assertTrue(Arrays.asList("c2", "c3", "c4", "c5").contains(p0Consumer));

        partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c3"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c4"));
        partitionInfos.add(new ConsumerPartitionInfo(4, "c5"));
        pd = new PartitionDistibutor(partitionInfos);

        // c3释放分区, 分配c4 或  c5
        // c2 : 1, 0
        // c4 : 3, 2
        // c5 : 4
        ret = pd.rleasePartition("c3");
        Assert.assertEquals(ret.size(), 1);
        Integer p2 = ret.keySet().iterator().next();
        Assert.assertEquals(p2.intValue(), 2);
        String p2Consumer = ret.values().iterator().next();
        Assert.assertTrue(Arrays.asList("c4", "c5").contains(p2Consumer));

        partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c4"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c4"));
        partitionInfos.add(new ConsumerPartitionInfo(4, "c5"));
        pd = new PartitionDistibutor(partitionInfos);

        // c4释放分区, 分配一个c5，另一个分配给c2或c5
        // c2 : 1, 0
        // c5 : 4, 3, 2
        ret = pd.rleasePartition("c4");
        Assert.assertEquals(ret.size(), 2);
        Assert.assertTrue(ret.containsKey(2) && ret.containsKey(3));
        for (String v : ret.values()) {
            Assert.assertTrue(v.equals("c2") || v.equals("c5"));
        }

        partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c5"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c5"));
        partitionInfos.add(new ConsumerPartitionInfo(4, "c5"));
        pd = new PartitionDistibutor(partitionInfos);

        // c5释放分区, 分配给c2
        // c2 : 0, 1, 2, 3, 4
        ret = pd.rleasePartition("c5");
        Assert.assertEquals(ret.size(), 3);
        Assert.assertTrue(ret.containsKey(2) && ret.containsKey(3) && ret.containsKey(4));
        for (String v : ret.values()) {
            Assert.assertTrue(v.equals("c2"));
        }
        // c2释放分区, 所有分区失去消费者
        ret = pd.rleasePartition("c2");
        Assert.assertEquals(ret.size(), 5);
        Assert.assertTrue(Arrays.asList(0, 1, 2, 3, 4).containsAll(ret.keySet()));
        for (String v : ret.values()) {
            Assert.assertTrue(v == null);
        }

    }

    @Test
    public void acquirePartition() throws MqException {
        List<ConsumerPartitionInfo> partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, null));
        partitionInfos.add(new ConsumerPartitionInfo(1, null));
        partitionInfos.add(new ConsumerPartitionInfo(2, null));
        partitionInfos.add(new ConsumerPartitionInfo(3, null));
        partitionInfos.add(new ConsumerPartitionInfo(4, null));

        PartitionDistibutor pd = new PartitionDistibutor(partitionInfos);

        Map<Integer, String> modifier = pd.acquiresPartition("c1");

        // 第一次分配拿走所有分区
        // c1 : 0, 1, 2, 3, 4
        modifier = new TreeMap<>(modifier);
        int pNum = 0;
        for (Entry<Integer, String> entry : modifier.entrySet()) {
            Assert.assertEquals(pNum, entry.getKey().intValue());
            pNum++;
            Assert.assertEquals(entry.getValue(), "c1");
        }
        Assert.assertTrue(pNum == 5);

        // 第二个消费者找c1要两个分区
        // c1 : 2, 3, 4
        // c2 : 0, 1
        modifier = pd.acquiresPartition("c2");
        Assert.assertEquals(modifier.size(), 2);
        Iterator<String> cit = modifier.values().iterator();
        Assert.assertEquals(cit.next(), "c2");
        Assert.assertEquals(cit.next(), "c2");
        Iterator<Integer> pit = modifier.keySet().iterator();
        Integer p1OfC2 = pit.next();
        Integer p2OfC2 = pit.next();
        Assert.assertNotEquals(p1OfC2, p2OfC2);
        List<Integer> pNums = Arrays.asList(0, 1, 2, 3, 4);
        Assert.assertTrue(pNums.contains(p1OfC2) && pNums.contains(p2OfC2), pNums.toString());

        // 第三个消费者找c1要一个分区
        // c1 : 3, 4
        // c2 : 0, 1
        // c3 : 2
        modifier = pd.acquiresPartition("c3");
        Assert.assertEquals(modifier.size(), 1);
        cit = modifier.values().iterator();
        Assert.assertEquals(cit.next(), "c3");
        Integer pOfC3 = modifier.keySet().iterator().next();
        // 不是从c2拿的
        Assert.assertNotEquals(p1OfC2, pOfC3);
        Assert.assertNotEquals(p2OfC2, pOfC3);
        Assert.assertTrue(pNums.contains(pOfC3));

        // 第四个消费者找  c1 || c2 要一个分区
        // c1 : 4
        // c2 : 0, 1
        // c3 : 2
        // c4 : 3
        // 或
        // c1 : 3, 4
        // c2 : 1
        // c3 : 2
        // c4 : 0
        modifier = pd.acquiresPartition("c4");
        Assert.assertEquals(modifier.size(), 1);
        cit = modifier.values().iterator();
        Assert.assertEquals(cit.next(), "c4");
        Integer pOfC4 = modifier.keySet().iterator().next();
        // 不是从c3拿的
        Assert.assertNotEquals(pOfC3, pOfC4);
        Assert.assertTrue(pNums.contains(pOfC4));

        // 第五个消费者请求分区
        modifier = pd.acquiresPartition("c5");
        Assert.assertEquals(modifier.size(), 1);
        cit = modifier.values().iterator();
        Assert.assertEquals(cit.next(), "c5");
        Integer pOfC5 = modifier.keySet().iterator().next();
        if (pOfC4 == p1OfC2 || pOfC4 == p2OfC2) {
            // 第四个消费者是找c2拿的，所以 第五个消费者找  c1 拿一个分区
            // c1 : 4
            // c2 : 1
            // c3 : 2
            // c4 : 0
            // c5 : 3
            Assert.assertTrue(!Arrays.asList(p1OfC2, p2OfC2, pOfC3, pOfC4).contains(pOfC5));
        }
        else {
            // 第四个消费者是找c1拿的，所以 第五个消费者找  c2 拿一个分区
            // c1 : 4
            // c2 : 1
            // c3 : 2
            // c4 : 3
            // c5 : 0
            Assert.assertTrue(p1OfC2 == pOfC5 || p2OfC2 == pOfC5);
        }
        // 第六个消费者请求分区
        try {
            modifier = pd.acquiresPartition("c6");
            Assert.fail("no partition for c6!");
        }
        catch (MqException e) {
            Assert.assertEquals(e.getError(), MqError.NO_PARTITION_FOR_CONSUMER);
        }
    }

}
