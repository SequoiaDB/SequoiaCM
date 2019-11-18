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

public class CopyJobInfo extends ScheduleJobInfo {
    private int days;
    private int sourceSiteId;
    private String sourceSiteName;

    private int targetSiteId;
    private String targetSiteName;

    private BSONObject extraCondition;
    private int scope;

    private long maxExecTime;

    public CopyJobInfo(String id, String type, String workspace, BSONObject content, String cron)
            throws ScheduleException {
        super(id, type, workspace, cron);
        checkAndParse(ScheduleServer.getInstance(), id, type, workspace, content, cron);
    }

    public CopyJobInfo(String id, String type, String workspace, int sourceSiteId,
            String sourceSiteName, int targetSiteId, String targetSiteName, int days,
            BSONObject extraCondition, String cron, int scope, long maxExecTime)
            throws ScheduleException {
        super(id, type, workspace, cron);

        this.sourceSiteId = sourceSiteId;
        this.sourceSiteName = sourceSiteName;
        this.targetSiteId = targetSiteId;
        this.targetSiteName = targetSiteName;
        this.days = days;
        this.extraCondition = extraCondition;
        this.scope = scope;
        this.maxExecTime = maxExecTime;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public int getDays() {
        return days;
    }

    public BSONObject getExtraCondtion() {
        return extraCondition;
    }

    public int getSourceSiteId() {
        return sourceSiteId;
    }

    public String getSourceSiteName() {
        return sourceSiteName;
    }

    public int getTargetSiteId() {
        return targetSiteId;
    }

    public String getTargetSiteName() {
        return targetSiteName;
    }

    private void checkAndParse(ScheduleServer server, String id, String type, String workspace,
            BSONObject content, String cron) throws ScheduleException {
        WorkspaceInfo wsInfo = getWorkspaceNotNull(server, workspace);

        sourceSiteName = ScheduleCommonTools.getStringValue(content,
                FieldName.Schedule.FIELD_COPY_SOURCE_SITE);
        SiteEntity sourceSiteEntity = wsInfo.getSite(sourceSiteName);

        if (null == sourceSiteEntity) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "source site is not exist in workspace:workspace=" + wsInfo.getName()
                    + ",site_name=" + sourceSiteName);
        }
        sourceSiteId = sourceSiteEntity.getId();
        
        targetSiteName = ScheduleCommonTools.getStringValue(content,
                FieldName.Schedule.FIELD_COPY_TARGET_SITE);
        SiteEntity targetSiteEntity = wsInfo.getSite(targetSiteName);
        if (null == targetSiteEntity) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "target site is not exist in workspace:workspace=" + wsInfo.getName()
                    + ",site_name=" + targetSiteName);
        }
        targetSiteId = targetSiteEntity.getId();

        // check site
        ScheduleStrategyMgr.getInstance().checkTransferSite(wsInfo, sourceSiteId, targetSiteId);
        
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
        .append(getCron()).append(",").append("sourceSiteName:")
        .append(getSourceSiteName()).append(",").append("SourceSiteId:")
        .append(getSourceSiteId()).append(",").append("targetSiteName:")
        .append(getTargetSiteName()).append(",").append("targetSiteId:")
        .append(getTargetSiteId());

        return sb.toString();
    }
}