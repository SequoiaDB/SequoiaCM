package com.sequoiacm.cloud.gateway.filter;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.sequoiacm.infrastructure.monitor.ReqRecorder;

public class StatisticReqPostFilter extends ZuulFilter {

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest req = ctx.getRequest();
        if (req != null) {
            Long preTime = (Long) req.getAttribute("preTime");
            if (preTime != null) {
                long time = System.currentTimeMillis() - preTime.longValue();
                ReqRecorder.getInstance().addRecord(time);
            }
        }
        return null;
    }

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        // TODO Auto-generated method stub
        return Integer.MAX_VALUE;
    }

}
