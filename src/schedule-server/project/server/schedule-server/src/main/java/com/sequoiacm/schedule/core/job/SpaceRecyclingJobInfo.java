package com.sequoiacm.schedule.core.job;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.entity.SiteEntity;
import org.bson.BSONObject;

public class SpaceRecyclingJobInfo extends ScheduleJobInfo {

    private String targetSite;
    private String recycleScope;
    private long maxExecTime;

    private int targetSiteId;

    public SpaceRecyclingJobInfo(String id, String type, String workspace, BSONObject content,
            String cron, String preferredRegion, String preferredZone) throws ScheduleException {
        super(id, type, workspace, cron, preferredRegion, preferredZone);
        checkAndParse(ScheduleServer.getInstance(), workspace, content);
    }

    private void checkAndParse(ScheduleServer server, String workspace, BSONObject content)
            throws ScheduleException {
        WorkspaceInfo wsInfo = getWorkspaceNotNull(server, workspace);

        targetSite = ScheduleCommonTools.getStringValue(content,
                FieldName.Schedule.FIELD_SPACE_RECYCLING_TARGET_SITE);
        SiteEntity targetSiteEntity = wsInfo.getSite(targetSite);

        if (null == targetSiteEntity) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "target site is not exist in workspace:workspace=" + wsInfo.getName()
                            + ",site_name=" + targetSite);
        }
        targetSiteId = targetSiteEntity.getId();

        recycleScope = ScheduleCommonTools.getStringValue(content,
                FieldName.Schedule.FIELD_SPACE_RECYCLING_SCOPE);
        if (recycleScope == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "recycleScope cannot be empty");
        }

        if (content.containsField(FieldName.Schedule.FIELD_MAX_EXEC_TIME)) {
            maxExecTime = ((Number) content.get(FieldName.Schedule.FIELD_MAX_EXEC_TIME))
                    .longValue();
        }
    }

    public String getTargetSite() {
        return targetSite;
    }

    public void setTargetSite(String targetSite) {
        this.targetSite = targetSite;
    }

    public String getRecycleScope() {
        return recycleScope;
    }

    public void setRecycleScope(String recycleScope) {
        this.recycleScope = recycleScope;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    public int getTargetSiteId() {
        return targetSiteId;
    }

    @Override
    public String toString() {
        return "SpaceRecyclingJobInfo{" + "targetSite='" + targetSite + '\'' + ", recycleScope='"
                + recycleScope + '\'' + ", maxExecTime=" + maxExecTime + ", targetSiteId="
                + targetSiteId + "} " + super.toString();
    }
}
