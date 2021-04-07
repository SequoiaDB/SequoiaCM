package com.sequoiacm.cloud.gateway.filter;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.sequoiacm.cloud.gateway.statistics.decider.ScmStatisticsDeciderGroup;
import com.sequoiacm.cloud.gateway.statistics.decider.ScmStatisticsDecisionResult;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StatisticReqPreFilter extends ZuulFilter {
    final static String STATISTICS_FLAG = "STATISTICS_FLAG";
    private final ScmStatisticsDeciderGroup deciderGroup;

    @Autowired
    public StatisticReqPreFilter(ScmStatisticsDeciderGroup deciderGroup) {
        this.deciderGroup = deciderGroup;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest req = ctx.getRequest();
        if (req != null) {
            req.setAttribute("preTime", System.currentTimeMillis());
        }

        ScmStatisticsDecisionResult result = deciderGroup.decide(req);
        if (result.isNeedStatistics()) {
            req.setAttribute(STATISTICS_FLAG, result.getStatisticsType());
            ctx.addZuulRequestHeader(ScmStatisticsDefine.STATISTICS_HEADER,
                    result.getStatisticsType());
        }
        return null;
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

}
