package com.sequoiacm.infrastructure.monitor.service;

import java.util.Collection;
import java.util.Map;

import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;

public interface IMonitorCollectorService {

    Map<String, Object> getHostInfo() throws Exception;

    Map<String, Object> gaugeResponse();

    Collection<WorkspaceFlow> shwoFlow();
}
