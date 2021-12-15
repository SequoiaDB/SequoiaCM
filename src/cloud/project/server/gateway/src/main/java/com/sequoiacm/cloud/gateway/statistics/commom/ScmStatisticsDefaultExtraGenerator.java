package com.sequoiacm.cloud.gateway.statistics.commom;

import com.alibaba.fastjson.JSON;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsFileMeta;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;

import javax.servlet.http.HttpServletRequest;

public class ScmStatisticsDefaultExtraGenerator {

    public static String generate(String type, HttpServletRequest request) {
        switch (type) {
            case ScmStatisticsType.FILE_DOWNLOAD:
            case ScmStatisticsType.FILE_UPLOAD:
                return generateWorkspaceExtra(request);
            case ScmStatisticsType.BREAKPOINT_FILE_UPLOAD:
                return null;
            default:
                throw new IllegalArgumentException("unrecognized statistics type:" + type);
        }
    }

    private static String generateWorkspaceExtra(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        String workspace = request.getParameter(CommonDefine.RestArg.WORKSPACE_NAME);
        if (workspace == null) {
            throw new IllegalArgumentException(
                    "missing request argument:" + CommonDefine.RestArg.WORKSPACE_NAME);
        }
        ScmStatisticsFileMeta defaultFileMeta = new ScmStatisticsFileMeta();
        defaultFileMeta.setWorkspace(workspace);
        return JSON.toJSONString(defaultFileMeta);
    }
}
