package com.sequoiacm.cloud.gateway.filter;

import javax.servlet.http.HttpServletRequest;

import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.sequoiacm.cloud.gateway.statistics.commom.ScmStatisticsDefaultExtraGenerator;
import com.sequoiacm.infrastructure.monitor.ReqRecorder;
import com.sequoiacm.infrastructure.common.SecurityRestField;
import com.sequoiacm.infrastructure.security.auth.ScmUserWrapper;
import com.sequoiacm.infrastructure.statistics.client.ScmStatisticsRawDataReporter;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

@Component
public class StatisticReqPostFilter extends ZuulFilter {

    private final ScmStatisticsRawDataReporter reporter;

    public final static int FILTER_ORDER = 10000;

    @Autowired
    public StatisticReqPostFilter(ScmStatisticsRawDataReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest req = ctx.getRequest();
        if (req == null) {
            return null;
        }
        Long preTime = (Long) req.getAttribute("preTime");
        if (preTime == null) {
            return null;
        }
        long time = System.currentTimeMillis() - preTime;
        ReqRecorder.getInstance().addRecord(time);
        String statisticsType = ctx.getZuulRequestHeaders()
                .get(ScmStatisticsDefine.STATISTICS_HEADER);
        if (statisticsType == null) {
            return null;
        }
        ScmUserWrapper userWrapper = (ScmUserWrapper) ctx.getRequest()
                .getAttribute(SecurityRestField.USER_INFO_WRAPPER);
        String username = userWrapper == null ? null : userWrapper.getUser().getUsername();
        if (ctx.getResponseStatusCode() >= 200 && ctx.getResponseStatusCode() < 300) {
            String extraStatistics = (String) ctx.getRequest()
                    .getAttribute(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER);
            reporter.report(true, statisticsType, username, preTime, time, extraStatistics);
        }
        else {
            String defaultExtra = ScmStatisticsDefaultExtraGenerator.generate(statisticsType, req);
            reporter.report(false, statisticsType, username, preTime, time, defaultExtra);
        }
        return null;
    }

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return FILTER_ORDER;
    }

}

// 阻止下游服务的部分响应头传递到客户端
// 将响应头存放至 Request Attribute 中，以便后续过滤器获取
@Component
class ExtraStatisticHeaderHandleFilter extends ZuulFilter {

    private final static List<String> excludeHeaders;

    static {
        excludeHeaders = new ArrayList<>();
        excludeHeaders.add(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER);
    }

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        if (RequestContext.getCurrentContext().getRequest()
                .getAttribute(StatisticReqPreFilter.STATISTICS_FLAG) == null) {
            return null;
        }
        List<Pair<String, String>> headers = RequestContext.getCurrentContext()
                .getZuulResponseHeaders();
        Iterator<Pair<String, String>> it = headers.iterator();
        while (it.hasNext()) {
            Pair<String, String> header = it.next();
            String headerName = header.first();
            String excludeHeader = getFromExcludeHeaders(headerName);
            if (excludeHeader != null) {
                it.remove();
                RequestContext.getCurrentContext().getRequest().setAttribute(excludeHeader,
                        header.second());
            }

        }
        return null;
    }

    private String getFromExcludeHeaders(String headerName) {
        for (String excludeHeader : excludeHeaders) {
            if (excludeHeader.equalsIgnoreCase(headerName)) {
                return excludeHeader;
            }
        }
        return null;
    }
}