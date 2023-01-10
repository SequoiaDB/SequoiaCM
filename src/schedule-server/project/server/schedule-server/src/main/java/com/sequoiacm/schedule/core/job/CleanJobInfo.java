package com.sequoiacm.schedule.core.job;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.schedule.bizconf.ScheduleStrategyMgr;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.entity.SiteEntity;

public class CleanJobInfo extends ScheduleJobInfo {
    private int days;
    private int siteId;
    private String siteName;
    private String checkSiteName;
    private int checkSiteId;
    private BSONObject extraCondition;
    private int scope;
    private long maxExecTime;

    private boolean quickStart;
    private boolean isRecycleSpace;
    private String dataCheckLevel;

    private BSONObject cleanTriggers;

    public CleanJobInfo(String id, String type, String workspace, BSONObject content, String cron,
            String preferredRegion, String preferredZone) throws ScheduleException {
        super(id, type, workspace, cron, preferredRegion, preferredZone);
        checkAndParse(ScheduleServer.getInstance(), id, type, workspace, content, cron);
    }

    public CleanJobInfo(String id, String type, String workspace, int siteId, String siteName,
            int days, BSONObject extraCondition, String cron, int scope, long maxExecTime,
            String preferredRegion, String preferredZone, boolean quickStart,
            boolean isRecycleSpace, String dataCheckLevel) throws ScheduleException {
        super(id, type, workspace, cron, preferredRegion, preferredZone);
        this.days = days;
        this.siteId = siteId;
        this.siteName = siteName;
        this.extraCondition = extraCondition;
        this.scope = scope;
        this.maxExecTime = maxExecTime;
        this.quickStart = quickStart;
        this.isRecycleSpace = isRecycleSpace;
        this.dataCheckLevel = dataCheckLevel;
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

    public boolean isQuickStart() {
        return quickStart;
    }

    public boolean isRecycleSpace() {
        return isRecycleSpace;
    }

    public String getDataCheckLevel() {
        return dataCheckLevel;
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

        // 延迟清理需检查的对端站点信息
        if (content.containsField(FieldName.Schedule.FIELD_CLEAN_CHECK_SITE)) {
            checkSiteName = ScheduleCommonTools.getStringValue(content,
                    FieldName.Schedule.FIELD_CLEAN_CHECK_SITE);
            SiteEntity checkSiteEntity = wsInfo.getSite(checkSiteName);
            if (null == checkSiteEntity) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                        "site is not exist in workspace:workspace=" + wsInfo.getName()
                                + ",site_name=" + checkSiteName);
            }
            checkSiteId = checkSiteEntity.getId();
        }

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

        quickStart = ScheduleCommonTools.getBooleanOrElse(content,
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

        cleanTriggers = ScheduleCommonTools.getBSONObjectValue(content,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS);
        if (null == cleanTriggers) {
            cleanTriggers = new BasicBSONObject();
        }
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public int getScope() {
        return scope;
    }

    public String getCheckSiteName() {
        return checkSiteName;
    }

    public int getCheckSiteId() {
        return checkSiteId;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public BSONObject getCleanTriggers() {
        return cleanTriggers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(getId()).append(",").append("type:").append(getType()).append(",")
                .append("workspace:").append(getWorkspace()).append(",").append("cron:")
                .append(getCron()).append(",").append("siteName:").append(getSiteName()).append(",")
                .append("siteId:").append(getSiteId()).append("quickStart:").append(isQuickStart())
                .append(",").append("isRecycleSpace:").append(isRecycleSpace()).append(",")
                .append("dataCheckLevel:").append(getDataCheckLevel());

        return sb.toString();
    }
}
