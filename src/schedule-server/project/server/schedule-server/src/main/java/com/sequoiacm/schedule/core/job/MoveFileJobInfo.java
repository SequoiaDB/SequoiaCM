package com.sequoiacm.schedule.core.job;

import com.sequoiacm.schedule.bizconf.ScheduleStrategyMgr;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.entity.SiteEntity;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class MoveFileJobInfo extends ScheduleJobInfo {

    private int days;

    private int sourceSiteId;
    private String sourceSiteName;

    private int targetSiteId;
    private String targetSiteName;

    private BSONObject extraCondition;
    private int scope;
    private long maxExecTime;

    private boolean isQuickStart;
    private boolean isRecycleSpace;
    private String dataCheckLevel;

    public MoveFileJobInfo(String id, String type, String workspace, BSONObject content,
            String cron, String preferredRegion, String preferredZone) throws ScheduleException {
        super(id, type, workspace, cron, preferredRegion, preferredZone);
        checkAndParse(ScheduleServer.getInstance(), workspace, content);
    }

    private void checkAndParse(ScheduleServer server, String workspace, BSONObject content)
            throws ScheduleException {
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
        ScheduleStrategyMgr.getInstance().checkMoveFileSite(wsInfo, sourceSiteId, targetSiteId);

        String maxStayTime = ScheduleCommonTools.getStringValue(content,
                FieldName.Schedule.FIELD_MAX_STAY_TIME);
        days = parseMaxStayTime(maxStayTime);
        extraCondition = ScheduleCommonTools.getBSONObjectValue(content,
                FieldName.Schedule.FIELD_EXTRA_CONDITION);
        if (null == extraCondition) {
            extraCondition = new BasicBSONObject();
        }

        if (content.containsField(FieldName.Schedule.FIELD_SCOPE)) {
            scope = (int) content.get(FieldName.Schedule.FIELD_SCOPE);
            checkCondition(scope, extraCondition);
        }
        else {
            scope = ScheduleDefine.ScopeType.CURRENT;
        }

        if (content.containsField(FieldName.Schedule.FIELD_MAX_EXEC_TIME)) {
            maxExecTime = ((Number) content.get(FieldName.Schedule.FIELD_MAX_EXEC_TIME))
                    .longValue();
        }

        isQuickStart = ScheduleCommonTools.getBooleanOrElse(content,
                FieldName.Schedule.FIELD_QUICK_START, false);
        isRecycleSpace = ScheduleCommonTools.getBooleanOrElse(content,
                FieldName.Schedule.FIELD_IS_RECYCLE_SPACE, false);
        if (content.containsField(FieldName.Schedule.FIELD_DATA_CHECK_LEVEL)) {
            dataCheckLevel = ScheduleCommonTools.getStringValue(content,
                    FieldName.Schedule.FIELD_DATA_CHECK_LEVEL);
            validateDataCheckLevel(dataCheckLevel);
        }
        else {
            dataCheckLevel = ScheduleDefine.DataCheckLevel.WEEK;
        }
    }

    public int getDays() {
        return days;
    }

    public BSONObject getExtraCondition() {
        return extraCondition;
    }

    public int getScope() {
        return scope;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public boolean isQuickStart() {
        return isQuickStart;
    }

    public boolean isRecycleSpace() {
        return isRecycleSpace;
    }

    public String getDataCheckLevel() {
        return dataCheckLevel;
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

    @Override
    public String toString() {
        return "MoveFileJobInfo{" + "days=" + days + ", sourceSiteId=" + sourceSiteId
                + ", sourceSiteName='" + sourceSiteName + '\'' + ", targetSiteId=" + targetSiteId
                + ", targetSiteName='" + targetSiteName + '\'' + ", extraCondition="
                + extraCondition + ", scope=" + scope + ", maxExecTime=" + maxExecTime
                + ", isQuickStart=" + isQuickStart + ", isRecycleSpace=" + isRecycleSpace
                + ", dataCheckLevel='" + dataCheckLevel + '\'' + '}';
    }
}
