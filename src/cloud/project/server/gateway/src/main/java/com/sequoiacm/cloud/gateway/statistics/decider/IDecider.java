package com.sequoiacm.cloud.gateway.statistics.decider;

import javax.servlet.http.HttpServletRequest;

// 决策一个请求是否需要统计
interface IDecider {
    // 返回 null 来表示不识别的请求
    ScmStatisticsDecisionResult decide(HttpServletRequest request);

    // 支持的类型
    String getType();
}
