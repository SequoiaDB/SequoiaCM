package com.sequoiacm.om.omserver.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.om.omserver.module.OmBatchOpResult;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmLifeCycleConfigDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmLifeCycleConfigDaoFactory;
import com.sequoiacm.om.omserver.module.lifecycle.OmStageTagDetail;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionBasic;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionFilter;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionSchedule;
import com.sequoiacm.om.omserver.module.lifecycle.OmTransitionWithWs;
import com.sequoiacm.om.omserver.service.ScmLifeCycleService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Component
public class ScmLifeCycleServiceImpl implements ScmLifeCycleService {

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmLifeCycleConfigDaoFactory scmLifeCycleConfigDaoFactory;

    @Override
    public long countStageTag(ScmOmSession session, BSONObject filter)
            throws ScmOmServerException, ScmInternalException {
        return listStageTag(session, filter, 0, -1).size();
    }

    @Override
    public List<OmStageTagDetail> listStageTag(ScmOmSession session, BSONObject filter, long skip,
            int limit) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return scmLifeCycleConfigDao.getStageTagList(filter, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void crateStageTag(ScmOmSession session, String name, String description)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.createStageTag(name, description);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void deleteStageTag(ScmOmSession session, String name)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.deleteStageTag(name);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void addSiteStageTag(ScmOmSession session, String siteName, String stageTag)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.addSiteStageTag(siteName, stageTag);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void removeStageTag(ScmOmSession session, String siteName)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.removeSiteStageTag(siteName);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }

    }

    @Override
    public long countTransition(ScmOmSession session, OmTransitionFilter filter)
            throws ScmOmServerException, ScmInternalException {
        return listTransition(session, filter, 0, -1).size();
    }

    @Override
    public List<OmTransitionWithWs> listTransition(ScmOmSession session, OmTransitionFilter filter,
            long skip, int limit) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return scmLifeCycleConfigDao.getTransitionList(filter, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void createTransition(ScmOmSession session, OmTransitionBasic omTransitionBasic)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.createTransition(omTransitionBasic);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void updateTransition(ScmOmSession session, String oldTransition,
            OmTransitionBasic omTransitionBasic)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.updateTransition(oldTransition, omTransitionBasic);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmBatchOpResult> addTransitionApply(ScmOmSession session, String transition,
            List<String> wsList) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        session.resetServiceEndpoint(preferSite);
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        List<OmBatchOpResult> res = new ArrayList<>();
        for (String workspace : wsList) {
            try {
                scmLifeCycleConfigDao.addTransition(workspace,
                        new OmTransitionBasic(transition));
                res.add(new OmBatchOpResult(workspace, true));
            }
            catch (Exception e) {
                if (e instanceof ScmInternalException) {
                    siteChooser.onException((ScmInternalException) e);
                }
                res.add(new OmBatchOpResult(workspace, false, e.getMessage()));
            }
        }
        return res;
    }

    @Override
    public List<OmBatchOpResult> removeTransitionApply(ScmOmSession session, String transition,
            List<String> wsList) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        session.resetServiceEndpoint(preferSite);
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        List<OmBatchOpResult> res = new ArrayList<>();
        for (String workspace : wsList) {
            try {
                scmLifeCycleConfigDao.removeTransition(workspace, transition);
                res.add(new OmBatchOpResult(workspace, true));
            }
            catch (Exception e) {
                if (e instanceof ScmInternalException) {
                    siteChooser.onException((ScmInternalException) e);
                }
                res.add(new OmBatchOpResult(workspace, false, e.getMessage()));
            }
        }
        return res;
    }

    @Override
    public void deleteTransition(ScmOmSession session, String name)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.deleteTransition(name);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmTransitionSchedule> listTransitionByWs(ScmOmSession session, String workspaceName)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return scmLifeCycleConfigDao.listTransitionByWs(workspaceName);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void addTransition(ScmOmSession session, OmTransitionBasic omTransitionBasic,
            String workspaceName) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.addTransition(workspaceName, omTransitionBasic);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void updateWsTransition(ScmOmSession session, String oldTransition,
            OmTransitionBasic omTransitionBasic, String workspaceName)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.updateWsTransition(workspaceName, oldTransition,
                    omTransitionBasic);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void removeTransition(ScmOmSession session, String workspace, String transition)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.removeTransition(workspace, transition);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void changeTransitionState(ScmOmSession session, String workspaceName, String transition,
            boolean isEnabled) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmLifeCycleConfigDao scmLifeCycleConfigDao = scmLifeCycleConfigDaoFactory
                .createLifeCycleConfigDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            scmLifeCycleConfigDao.changeTransitionState(workspaceName, transition, isEnabled);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }
}
