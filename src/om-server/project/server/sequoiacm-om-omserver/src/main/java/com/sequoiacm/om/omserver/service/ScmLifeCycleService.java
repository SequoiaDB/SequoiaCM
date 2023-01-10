package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBatchOpResult;
import com.sequoiacm.om.omserver.module.lifecycle.OmStageTagDetail;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionFilter;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionBasic;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionSchedule;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionWithWs;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.util.List;

public interface ScmLifeCycleService {

    long countStageTag(ScmOmSession session, BSONObject filter)
            throws ScmOmServerException, ScmInternalException;

    List<OmStageTagDetail> listStageTag(ScmOmSession session, BSONObject filter, long skip,
            int limit) throws ScmOmServerException, ScmInternalException;

    void crateStageTag(ScmOmSession session, String name, String description)
            throws ScmOmServerException, ScmInternalException;

    void deleteStageTag(ScmOmSession session, String name)
            throws ScmOmServerException, ScmInternalException;

    void addSiteStageTag(ScmOmSession session, String siteName, String stageTag)
            throws ScmOmServerException, ScmInternalException;

    void removeStageTag(ScmOmSession session, String siteName)
            throws ScmOmServerException, ScmInternalException;

    long countTransition(ScmOmSession session, OmTransitionFilter filter)
            throws ScmOmServerException, ScmInternalException;

    List<OmTransitionWithWs> listTransition(ScmOmSession session, OmTransitionFilter filter,
            long skip, int limit) throws ScmOmServerException, ScmInternalException;

    void createTransition(ScmOmSession session, OmTransitionBasic omTransitionBasic)
            throws ScmOmServerException, ScmInternalException;

    void updateTransition(ScmOmSession session, String oldTransition,
            OmTransitionBasic omTransitionBasic) throws ScmOmServerException, ScmInternalException;

    List<OmBatchOpResult> addTransitionApply(ScmOmSession session, String transition,
            List<String> wsList) throws ScmOmServerException, ScmInternalException;

    List<OmBatchOpResult> removeTransitionApply(ScmOmSession session, String transition,
            List<String> wsList) throws ScmOmServerException, ScmInternalException;

    void deleteTransition(ScmOmSession session, String name)
            throws ScmOmServerException, ScmInternalException;

    List<OmTransitionSchedule> listTransitionByWs(ScmOmSession session, String workspaceName)
            throws ScmOmServerException, ScmInternalException;

    void addTransition(ScmOmSession session, OmTransitionBasic omTransitionBasic,
            String workspaceName) throws ScmOmServerException, ScmInternalException;

    void updateWsTransition(ScmOmSession session, String oldTransition,
            OmTransitionBasic omTransitionBasic, String workspaceName)
            throws ScmOmServerException, ScmInternalException;

    void removeTransition(ScmOmSession session, String workspaceName, String transition)
            throws ScmOmServerException, ScmInternalException;

    void changeTransitionState(ScmOmSession session, String workspaceName, String transition,
            boolean isEnabled) throws ScmOmServerException, ScmInternalException;
}
