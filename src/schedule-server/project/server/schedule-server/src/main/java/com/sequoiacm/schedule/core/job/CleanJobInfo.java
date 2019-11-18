package com.sequoiacm.schedule.core.job;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.ScheduleStrategyMgr;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.entity.SiteEntity;
import com.sequoiacm.schedule.exception.ScheduleException;

public class CleanJobInfo extends ScheduleJobInfo {
    private int days;
    private int siteId;
    private String siteName;
    private BSONObject extraCondition;
    private int scope;
    private long maxExecTime;

    public CleanJobInfo(String id, String type, String workspace, BSONObject content, String cron)
            throws ScheduleException {
        super(id, type, workspace, cron);
        checkAndParse(ScheduleServer.getInstance(), id, type, workspace, content, cron);
    }

    public CleanJobInfo(String id, String type, String workspace, int siteId, String siteName,
            int days, BSONObject extraCondition, String cron, int scope, long maxExecTime)
            throws ScheduleException {
        super(id, type, workspace, cron);
        this.days = days;
        this.siteId = siteId;
        this.siteName = siteName;
        this.extraCondition = extraCondition;
        this.scope = scope;
        this.maxExecTime = maxExecTime;
    }

    public int getDays() {
        return days;
    }

    public BSONObject getExtraCondtion() {
        return extraCondition;
    }

    public int getSiteId() {
        return siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    private void checkAndParse(ScheduleServer server, String id, String type, String workspace,
            BSONObject content, String cron) throws ScheduleException {
        WorkspaceInfo wsInfo = getWorkspaceNotNull(server, workspace);

        siteName = ScheduleCommonTools.getStringValue(content, FieldName.Schedule.FIELD_CLEAN_SITE);
        SiteEntity siteEntity = wsInfo.getSite(siteName);
        if (null == siteEntity) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "site is not exist in workspace:workspace=" + wsInfo.getName() + ",site_name="
                            + siteName);
        }
        siteId = siteEntity.getId();
        
        // check site
        ScheduleStrategyMgr.getInstance().checkCleanSite(wsInfo, siteId);

        String maxStayTime = ScheduleCommonTools.getStringValue(content,
                FieldName.Schedule.FIELD_MAX_STAY_TIME);
        days = parseMaxStayTime(maxStayTime);

        // just check value type
        extraCondition = ScheduleCommonTools.getBSONObjectValue(content,
                FieldName.Schedule.FIELD_EXTRA_CONDITION);
        if (null == extraCondition) {
            extraCondition = new BasicBSONObject();
        }
        
        if(content.containsField(FieldName.Schedule.FIELD_SCOPE)){
            scope = (int) content.get(FieldName.Schedule.FIELD_SCOPE);
            checkCondition(scope, extraCondition);
        }else {
            scope = ScheduleDefine.ScopeType.CURRENT;
        }

        if (content.containsField(FieldName.Schedule.FIELD_MAX_EXEC_TIME)) {
            maxExecTime = ((Number) content.get(FieldName.Schedule.FIELD_MAX_EXEC_TIME))
                    .longValue();
        }
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(getId()).append(",").append("type:").append(getType()).append(",")
        .append("workspace:").append(getWorkspace()).append(",").append("cron:")
        .append(getCron()).append(",").append("siteName:").append(getSiteName())
        .append(",").append("siteId:").append(getSiteId());

        return sb.toString();
    }
}
