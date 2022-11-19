package com.sequoiacm.cloud.gateway.forward.decider;

import javax.servlet.http.HttpServletRequest;

public interface ForwardDecider {
    // 决定一个请求的转发方式
    Decision decide(HttpServletRequest req);
}

