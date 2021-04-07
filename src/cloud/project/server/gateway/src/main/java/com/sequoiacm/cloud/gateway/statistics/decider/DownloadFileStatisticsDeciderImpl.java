package com.sequoiacm.cloud.gateway.statistics.decider;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.gateway.statistics.config.ScmStatisticsFileDownloadConfig;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;

@RefreshScope
@Component
public class DownloadFileStatisticsDeciderImpl implements IDecider {
    @Autowired
    private ScmStatisticsFileDownloadConfig config;

    @Override
    public ScmStatisticsDecisionResult decide(HttpServletRequest request) {
        if (!request.getMethod().equals("GET")) {
            return null;
        }
        if (!Pattern.matches("^/.+/api/v1/files/[0-9a-z]+[/]?$", request.getRequestURI())) {
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
        return ScmStatisticsType.FILE_DOWNLOAD;
    }
}
