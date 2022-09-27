package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmConfPropsParam;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmConfigPropsDao {

    ScmUpdateConfResultSet updateProperties(ScmOmSession session, OmConfPropsParam config)
            throws ScmInternalException;
}
