package com.sequoiacm.infrastructure.monitor.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import com.sequoiacm.infrastructure.monitor.HostManager;
import com.sequoiacm.infrastructure.monitor.ReqRecorder;
import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;
import com.sequoiacm.infrastructure.monitor.service.IMonitorCollectorService;

@Service
public class MonitorCollectorServiceImpl implements IMonitorCollectorService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorCollectorServiceImpl.class);

    @Override
    public Map<String, Object> getHostInfo() throws Exception {
        Map<String, Object> info = new HashMap<String, Object>();
        Map<String, Object> memInfo;
        Map<String, Object> stat;
        memInfo = HostManager.getMemInfo();
        stat = HostManager.getStat();
        if ((memInfo == null || memInfo.isEmpty()) && (stat == null || stat.isEmpty())) {
            logger.warn(" host info can not found ");
            return null;
        }
        info.put("cpu", stat);
        info.put("memory", memInfo);

        return info;
    }

    @Override
    public Map<String, Object> gaugeResponse() {
        Map<String, Object> result = new HashMap<String, Object>();
        AtomicLong count = ReqRecorder.getInstance().getCounts();
        AtomicLong time = ReqRecorder.getInstance().getTime();
        if (count == null || time == null) {
            return null;
        }
        result.put("response_count", count.get());
        result.put("response_time", time.get());
        logger.debug("gauge response : count={}, time={}", count.get(), time.get());
        return result;
    }

    @Override
    public Collection<WorkspaceFlow> shwoFlow() {
        return FlowRecorder.getInstance().getFlow();
    }
}
