package com.sequoiacm.infrastructure.ribbon;

import com.netflix.loadbalancer.Server;

import java.util.List;

public interface ScmServersFilter {

    List<Server> doFilter(List<Server> servers);

    boolean shouldFilter();

}
