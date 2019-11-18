package com.sequoiacm.cloud.adminserver.service;

import java.util.Collection;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;

public interface IMonitorService {

    BSONObject listHealth(String serviceName) throws Exception;

    BSONObject listHostInfo() throws Exception;;

    Collection<WorkspaceFlow> showFlow() throws Exception;;

    BSONObject gaugeResponse() throws Exception;;
}
