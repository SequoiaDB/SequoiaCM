package com.sequoiacm.cloud.gateway.forward.decider;

import javax.servlet.http.HttpServletRequest;

public interface CustomForwardDecider {
    // 决定一个请求要不要走自实现的转发逻辑
    Decision decide(HttpServletRequest req);
}

