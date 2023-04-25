package com.sequoiacm.schedule.service;

import com.sequoiacm.schedule.common.model.LifeCycleConfigUserEntity;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.TransitionFullEntity;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.common.model.TransitionUserEntity;
import org.bson.BSONObject;

import java.util.List;

public interface LifeCycleConfigService {
    void setGlobalLifeCycleConfig(String user, LifeCycleConfigUserEntity info)
            throws ScheduleException;

    BSONObject getGlobalLifeCycleConfig() throws ScheduleException;

    void addGlobalStageTag(String user, String stageTagName, String stageTagDesc) throws Exception;

    void removeGlobalStageTag(String stageTagName, String user) throws Exception;

    BSONObject listGlobalStageTag() throws ScheduleException;

    void addGlobalTransition(TransitionUserEntity info, String user) throws Exception;

    void removeGlobalTransition(String transitionName, String user) throws Exception;

    void updateGlobalTransition(String transitionName, TransitionUserEntity info, String user)
            throws Exception;

    TransitionFullEntity getGlobalTransitionByName(String transitionName) throws Exception;

    TransitionScheduleEntity wsApplyTransition(String user, String workspace, String transitionName,
            TransitionUserEntity info, String preferredRegion, String preferredZone)
            throws Exception;

    TransitionScheduleEntity wsUpdateTransition(String user, String workspace,
            String transitionName, TransitionUserEntity info, String preferredRegion,
            String preferredZone) throws Exception;

    void wsRemoveTransition(String workspace, String transitionName) throws Exception;

    TransitionScheduleEntity findWsTransition(String workspace, String transitionName)
            throws Exception;

    List<TransitionScheduleEntity> listWsTransition(String workspace) throws Exception;

    List<TransitionUserEntity> listGlobalTransitionByStageTag(String stageTag) throws Exception;

    BSONObject listWsApplyTransition(String transitionName) throws Exception;

    List<TransitionUserEntity> listGlobalTransition() throws Exception;

    void wsUpdateTransitionStatus(String user, String workspace, String transitionName,
            boolean status) throws Exception;

    BSONObject startOnceTransition(String workspaceName, String options, String sourceStageTag,
            String destStageTag, String userDetail, String sessionId, String preferredRegion,
            String preferredZone, int type, boolean isAsyncCountFile) throws Exception;

    void setSiteStageTag(String siteName, String stageTag) throws Exception;

    void alterSiteStageTag(String siteName, String stageTag) throws Exception;

    void deleteSiteStageTag(String siteName) throws Exception;

    String getSiteStageTag(String siteName) throws ScheduleException;

    void deleteLifeCycleConfig() throws Exception;
}
