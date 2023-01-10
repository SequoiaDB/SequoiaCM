package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmTransitionApplyInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.om.omserver.module.OmScheduleBasicInfo;
import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmTransitionSchedule;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.om.omserver.common.CommonUtil;
import com.sequoiacm.om.omserver.common.PageUtil;
import com.sequoiacm.om.omserver.dao.ScmLifeCycleConfigDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.lifecycle.OmStageTagDetail;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionBasic;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionFilter;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionSchedule;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionWithWs;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public class ScmLifeCycleConfigDaoImpl implements ScmLifeCycleConfigDao {

    private ScmOmSession session;

    public ScmLifeCycleConfigDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public List<OmStageTagDetail> getStageTagList(BSONObject condition, long skip, int limit)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            List<ScmLifeCycleStageTag> stageTagList = ScmSystem.LifeCycleConfig
                    .getLifeCycleConfig(con).getStageTagConfig();
            List<ScmSiteInfo> siteList = getSiteList(con);
            List<OmStageTagDetail> matchStageTagList = new ArrayList<>();
            String nameMatcher = BsonUtils.getString(condition, "nameMatcher");
            for (ScmLifeCycleStageTag scmLifeCycleStageTag : stageTagList) {
                String bindingSite = getBindingSite(siteList, scmLifeCycleStageTag.getName());
                if (StringUtils.isEmpty(nameMatcher)) {
                    matchStageTagList.add(new OmStageTagDetail(scmLifeCycleStageTag, bindingSite));
                    continue;
                }
                if (scmLifeCycleStageTag.getName().contains(nameMatcher)) {
                    matchStageTagList.add(new OmStageTagDetail(scmLifeCycleStageTag, bindingSite));
                }
            }
            return PageUtil.getPageOfResult(matchStageTagList, skip, limit);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get stage tag list, " + e.getMessage(), e);
        }
    }

    @Override
    public void createStageTag(String name, String description) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmSystem.LifeCycleConfig.addStageTag(con, name, description);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to create stage tag, " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteStageTag(String name) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmSystem.LifeCycleConfig.removeStageTag(con, name);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to delete stage tag, " + e.getMessage(), e);
        }
    }

    @Override
    public void addSiteStageTag(String siteName, String stageTag) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmFactory.Site.setSiteStageTag(con, siteName, stageTag);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to set stage tag for site, " + e.getMessage(), e);
        }
    }

    @Override
    public void removeSiteStageTag(String siteName) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmFactory.Site.unsetSiteStageTag(con, siteName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to remove the stage tag of the site, " + e.getMessage(), e);
        }
    }

    private List<ScmSiteInfo> getSiteList(ScmSession scmSession) throws ScmException {
        List<ScmSiteInfo> res = new ArrayList<>();
        ScmCursor<ScmSiteInfo> cursor = null;
        try {
            cursor = ScmFactory.Site.listSite(scmSession);
            while (cursor.hasNext()) {
                res.add(cursor.getNext());
            }
            return res;
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
    }

    private String getBindingSite(List<ScmSiteInfo> siteList, String stageTag) throws ScmException {
        for (ScmSiteInfo scmSiteInfo : siteList) {
            if (StringUtils.equals(scmSiteInfo.getStageTag(), stageTag)) {
                return scmSiteInfo.getName();
            }
        }
        return null;
    }

    @Override
    public List<OmTransitionWithWs> getTransitionList(OmTransitionFilter filter, long skip,
            int limit) throws ScmInternalException {
        try {
            List<OmTransitionWithWs> res = new ArrayList<>();
            List<ScmLifeCycleTransition> matchTransitionList = getMatcherTransitions(filter);
            List<ScmLifeCycleTransition> pageOfResult = PageUtil
                    .getPageOfResult(matchTransitionList, skip, limit);
            for (ScmLifeCycleTransition transition : pageOfResult) {
                res.add(transformToOmTransitionWithWs(transition));
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get transition list, " + e.getMessage(), e);
        }
    }

    private OmTransitionWithWs transformToOmTransitionWithWs(ScmLifeCycleTransition transition)
            throws ScmException {
        List<String> workspaces = new ArrayList<>();
        List<String> workspacesCustomized = new ArrayList<>();
        List<ScmTransitionApplyInfo> applyInfoList = ScmSystem.LifeCycleConfig
                .listWorkspace(session.getConnection(), transition.getName());
        for (ScmTransitionApplyInfo applyInfo : applyInfoList) {
            if (applyInfo.isCustomized()) {
                workspacesCustomized.add(applyInfo.getWorkspace());
            }
            else {
                workspaces.add(applyInfo.getWorkspace());
            }
        }
        return new OmTransitionWithWs(transition, workspaces, workspacesCustomized);
    }

    private List<ScmLifeCycleTransition> getMatcherTransitions(OmTransitionFilter filter)
            throws ScmException {
        List<ScmLifeCycleTransition> transitionList;
        if (filter.getStageTag() == null) {
            transitionList = ScmSystem.LifeCycleConfig
                    .getLifeCycleConfig(session.getConnection()).getTransitionConfig();
        }
        else {
            transitionList = ScmSystem.LifeCycleConfig.listTransition(session.getConnection(),
                    filter.getStageTag());
        }

        if (filter.getNameMatcher() == null) {
            return transitionList;
        }

        List<ScmLifeCycleTransition> res = new ArrayList<>();
        for (ScmLifeCycleTransition transition : transitionList) {
            if (transition.getName().contains(filter.getNameMatcher())) {
                res.add(transition);
            }
        }
        return res;
    }

    @Override
    public void createTransition(OmTransitionBasic omTransitionBasic) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmLifeCycleTransition transition = omTransitionBasic
                    .transformToScmLifeCycleTransition();
            ScmSystem.LifeCycleConfig.addTransition(con, transition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to create transition, " + e.getMessage(), e);
        }
    }

    @Override
    public void updateTransition(String oldTransition, OmTransitionBasic omTransitionBasic)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmSystem.LifeCycleConfig.updateTransition(con, oldTransition,
                    omTransitionBasic.transformToScmLifeCycleTransition());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to update transition, " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteTransition(String name) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmSystem.LifeCycleConfig.removeTransition(con, name);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to delete transition, " + e.getMessage(), e);
        }
    }

    @Override
    public List<OmTransitionSchedule> listTransitionByWs(String workspaceName)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            List<OmTransitionSchedule> res = new ArrayList<>();
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, con);
            List<ScmTransitionSchedule> transitionSchedules = workspace.listTransition();
            for (ScmTransitionSchedule transitionSchedule : transitionSchedules) {
                // 填充调度任务详情
                List<OmScheduleBasicInfo> scheduleList = new ArrayList<>();
                for (ScmId scheduleId : transitionSchedule.getScheduleIds()) {
                    ScmSchedule schedule = ScmSystem.Schedule.get(con, scheduleId);
                    OmScheduleBasicInfo omScheduleBasicInfo = new OmScheduleBasicInfo();
                    omScheduleBasicInfo.setName(schedule.getName());
                    omScheduleBasicInfo.setEnable(schedule.isEnable());
                    scheduleList.add(omScheduleBasicInfo);
                }
                res.add(new OmTransitionSchedule(transitionSchedule, scheduleList));
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get transitions by workspace, " + e.getMessage(), e);
        }
    }

    @Override
    public void addTransition(String workspaceName, OmTransitionBasic transition)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, con);
            if (!transition.isCustomized()) {
                workspace.applyTransition(transition.getName());
                return;
            }
            workspace.applyTransition(transition.getName(),
                    transition.transformToScmLifeCycleTransition());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to add transition, " + e.getMessage(), e);
        }
    }

    @Override
    public void updateWsTransition(String workspaceName, String oldTransition,
            OmTransitionBasic transition) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, con);
            workspace.updateTransition(oldTransition,
                    transition.transformToScmLifeCycleTransition());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed update transition in workspace, " + e.getMessage(), e);
        }
    }

    @Override
    public void removeTransition(String workspaceName, String transition)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, con);
            workspace.removeTransition(transition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to remove transition, " + e.getMessage(), e);
        }
    }

    @Override
    public void changeTransitionState(String workspaceName, String transition, boolean isEnabled)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(workspaceName, con);
            ScmTransitionSchedule transitionSchedule = workspace.getTransition(transition);
            if (isEnabled) {
                transitionSchedule.enable();
            }
            else {
                transitionSchedule.disable();
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to change transition state, " + e.getMessage(), e);
        }
    }
}
