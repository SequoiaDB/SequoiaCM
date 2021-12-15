package com.sequoiacm.cloud.gateway.statistics.decider;

import com.sequoiacm.cloud.gateway.statistics.config.ScmStatisticsFileDownloadConfig;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

@RefreshScope
@Component
public class UploadBreakpointFileDeciderImpl implements IDecider {
    @Autowired
    private ScmStatisticsFileDownloadConfig config;

    @Override
    public ScmStatisticsDecisionResult decide(HttpServletRequest request) {
        if (!request.getMethod().equals("POST") && !request.getMethod().equals("PUT")) {
            return null;
        }
        // 通过断点续传方式创建文件分为三步：创建断点文件、断点上传、转为普通文件，
        // 此处拦截创建断点文件和断点上传的请求：POST/PUT /sitename/api/v1/breakpointfiles/{file_name}
        // 转为普通文件的请求在UploadFileStatisticsDeciderImpl中已经进行了拦截
        if (!Pattern.matches("^/[zuul/]?.+/api/v1/breakpointfiles/.+[/]?$",
                request.getRequestURI())) {
            return null;
        }
        if (request.getParameter("action") != null) {
            return null;
        }
        String workspace = request.getParameter(CommonDefine.RestArg.WORKSPACE_NAME);
        if (workspace == null) {
            return null;
        }
        if (!config.isContainWorkspace(workspace)) {
            return new ScmStatisticsDecisionResult(false, ScmStatisticsType.BREAKPOINT_FILE_UPLOAD);
        }
        return new ScmStatisticsDecisionResult(true, ScmStatisticsType.BREAKPOINT_FILE_UPLOAD);
    }

    @Override
    public String getType() {
        // 断点文件上传纳入文件上传（FILE_UPLOAD）统计，开启文件上传统计的同时也开启了断点文件上传统计
        return ScmStatisticsType.FILE_UPLOAD;
    }

}
