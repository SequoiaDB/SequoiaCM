package org.sequoiacm.mq.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.server.common.PartitionDistibutor;

// 针对一些解决过的BUG，补充的测试场景
public class PartitionDistributorTest {

    //c1 六个分区
    //c2 五个分区
    //此时新进一个c3： c3必须从c1拿两个，c2拿一个，而不是从c1那拿三个导致不均匀
    @Test
    public void test1() throws MqException {
        List<ConsumerPartitionInfo> partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(4, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(5, "c1"));

        partitionInfos.add(new ConsumerPartitionInfo(6, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(7, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(8, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(9, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(10, "c2"));

        PartitionDistibutor pd = new PartitionDistibutor(partitionInfos);
        // 删除3号分区，不需调整
        Map<Integer, String> ret = pd.acquiresPartition("c3");
        ret = new TreeMap<>(ret);
        Assert.assertTrue(ret.size() == 3);
        Iterator<Integer> it = ret.keySet().iterator();
        int p = it.next();
        Assert.assertTrue(0 <= p && p <= 5, p + "");
        p = it.next();
        Assert.assertTrue(0 <= p && p <= 5, p + "");
        p = it.next();
        Assert.assertTrue(0 <= p && p <= 10, p + "");
        for (String c : ret.values()) {
            Assert.assertEquals(c, "c3");
        }
    }
    
    
    //c1 2个分区
    //c2 2个分区
    //此时新进一个c3： c3必须从c1或c2拿一个；
    //之前存在的问题：PartitionDistributor在从其它消费者当中获取分区时，余数处理逻辑有误提前退出循环，导致c3没有拿到分区。
    @Test
    public void test2() throws MqException {
        List<ConsumerPartitionInfo> partitionInfos = new ArrayList<>();
        partitionInfos.add(new ConsumerPartitionInfo(0, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(1, "c1"));
        partitionInfos.add(new ConsumerPartitionInfo(2, "c2"));
        partitionInfos.add(new ConsumerPartitionInfo(3, "c2"));

        PartitionDistibutor pd = new PartitionDistibutor(partitionInfos);
        // 删除3号分区，不需调整
        Map<Integer, String> ret = pd.acquiresPartition("c3");
        ret = new TreeMap<>(ret);
        Assert.assertTrue(ret.size() == 1);
        Iterator<Integer> it = ret.keySet().iterator();
        int p = it.next();
        Assert.assertTrue(0 <= p && p <= 3, p + "");
        for (String c : ret.values()) {
            Assert.assertEquals(c, "c3");
        }
    }
}
