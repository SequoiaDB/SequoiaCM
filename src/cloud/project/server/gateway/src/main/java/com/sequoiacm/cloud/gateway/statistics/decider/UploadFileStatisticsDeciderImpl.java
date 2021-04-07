package com.sequoiacm.cloud.gateway.statistics.decider;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.gateway.statistics.config.ScmStatisticsFileUploadConfig;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;
@RefreshScope
@Component
class UploadFileStatisticsDeciderImpl implements IDecider {
    @Autowired
    private ScmStatisticsFileUploadConfig config;

    @Override
    public ScmStatisticsDecisionResult decide(HttpServletRequest request) {
        if (!request.getMethod().equals("POST")) {
            return null;
        }
        // 文件上传可以走 /zuul/*** ，也可以走 /sitename/***
        // 目前驱动普通文件上传走 /zuul/*** ，断点文件上传走 /sitename/***
        if (!Pattern.matches("^/[zuul/]?.+/api/v1/files[/]?$", request.getRequestURI())) {
            return null;
        }

        String workspace = request.getParameter("workspace_name");
        if (workspace == null) {
            return null;
        }
        if (!config.isContainWorkspace(workspace)) {
            return new ScmStatisticsDecisionResult(false, getType());
        }

        return new ScmStatisticsDecisionResult(true, getType());
    }

    @Override
    public String getType() {
        return ScmStatisticsType.FILE_UPLOAD;
    }
}
