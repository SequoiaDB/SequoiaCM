package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmConfigPropsDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmConfigPropsDaoFactory;
import com.sequoiacm.om.omserver.module.OmConfPropsParam;
import com.sequoiacm.om.omserver.service.ScmConfigPropsService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScmConfigPropsServiceImpl implements ScmConfigPropsService {

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmConfigPropsDaoFactory scmConfigPropsDaoFactory;

    @Override
    public ScmUpdateConfResultSet updateProperties(ScmOmSession session, OmConfPropsParam config)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmConfigPropsDao propertiesDao = scmConfigPropsDaoFactory.createPropertiesDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return propertiesDao.updateProperties(session, config);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }
}