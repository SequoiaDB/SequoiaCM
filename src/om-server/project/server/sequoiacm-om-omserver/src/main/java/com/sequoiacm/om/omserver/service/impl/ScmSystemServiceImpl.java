package com.sequoiacm.om.omserver.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmSystemDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmSystemDaoFactory;
import com.sequoiacm.om.omserver.service.ScmSystemService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmSystemServiceImpl implements ScmSystemService {

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmSystemDaoFactory scmSystemDaoFactory;

    @Override
    public void setGlobalConfig(ScmOmSession session, String configName, String configValue)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        try {
            session.resetServiceEndpoint(preferSite);
            ScmSystemDao scmSystemDao = scmSystemDaoFactory.createSystemDao(session);
            scmSystemDao.setGlobalConfig(session, configName, configValue);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getGlobalConfig(ScmOmSession session, String configName)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        try {
            session.resetServiceEndpoint(preferSite);
            ScmSystemDao scmSystemDao = scmSystemDaoFactory.createSystemDao(session);
            return scmSystemDao.getGlobalConfig(session, configName);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }
}
