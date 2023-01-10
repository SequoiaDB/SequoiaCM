package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.lifecycle.OmStageTagDetail;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionFilter;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionBasic;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionSchedule;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionWithWs;
import org.bson.BSONObject;

import java.util.List;

public interface ScmLifeCycleConfigDao {

    List<OmStageTagDetail> getStageTagList(BSONObject condition, long skip, int limit)
            throws ScmInternalException;

    void createStageTag(String name, String description) throws ScmInternalException;

    void deleteStageTag(String name) throws ScmInternalException;

    void addSiteStageTag(String siteName, String stageTag) throws ScmInternalException;

    void removeSiteStageTag(String siteName) throws ScmInternalException;

    List<OmTransitionWithWs> getTransitionList(OmTransitionFilter filter, long skip, int limit)
            throws ScmInternalException;

    void createTransition(OmTransitionBasic omTransitionBasic) throws ScmInternalException;

    void updateTransition(String oldTransition, OmTransitionBasic omTransitionBasic)
            throws ScmInternalException;

    void deleteTransition(String name) throws ScmInternalException;

    List<OmTransitionSchedule> listTransitionByWs(String workspaceName) throws ScmInternalException;

    void addTransition(String workspace, OmTransitionBasic transition) throws ScmInternalException;

    void updateWsTransition(String workspaceName, String oldTransition,
            OmTransitionBasic omTransitionBasic) throws ScmInternalException;

    void removeTransition(String workspace, String transition) throws ScmInternalException;

    void changeTransitionState(String workspaceName, String transition, boolean isEnabled)
            throws ScmInternalException;
}
