package com.sequoiacm.schedule.core.job;

import org.bson.BSONObject;

import com.sequoiacm.schedule.bizconf.ScmArgChecker;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;

public class ScheduleJobInfo {
    private String id;
    private String type;
    private String workspace;
    private String cron;

    public ScheduleJobInfo(String id, String type, String workspace, String cron) {
        this.id = id;
        this.type = type;
        this.workspace = workspace;
        this.cron = cron;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getCron() {
        return cron;
    }

    int parseMaxStayTime(String maxStayTime) throws ScheduleException {
        if (maxStayTime.isEmpty()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "maxStayTime can't be empty:maxStayTime=" + maxStayTime);
        }

        if (!maxStayTime.endsWith("d")) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "maxStayTime only supports day period(d):maxStayTime=" + maxStayTime);
        }

        String num = maxStayTime.substring(0, maxStayTime.length() - 1);
        if (num.isEmpty()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "maxStayTime is invalid:maxStayTime=" + maxStayTime);
        }

        try {
            return Integer.parseInt(num);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "maxStayTime is invalid:maxStayTime=" + maxStayTime);
        }
    }

    void checkCondition(int scope, BSONObject condition) throws ScheduleException{
        if(scope == ScheduleDefine.ScopeType.CURRENT) {
            //no check for current scope
            return;
        }
        if(scope != ScheduleDefine.ScopeType.HISTORY && scope != ScheduleDefine.ScopeType.ALL) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "unknown scope:scope=" + scope);
        }
        ScmArgChecker.ExtralCondition.checkHistoryFileMatcher(condition);
    }

    WorkspaceInfo getWorkspaceNotNull(ScheduleServer server, String wsName)
            throws ScheduleException {
        WorkspaceInfo wsInfo = server.getWorkspace(wsName);
        if (null == wsInfo) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.WORKSPACE_NOT_EXISTS,
                    "workspace is not exist:workspace=" + wsName);
        }

        return wsInfo;
    }

}
