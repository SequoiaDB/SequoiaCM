package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmServiceCenterDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

import java.util.List;

public class ScmServiceCenterDaoImpl implements ScmServiceCenterDao {
    private ScmOmSession session;

    public ScmServiceCenterDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public List<ScmServiceInstance> getServiceList() throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            return ScmSystem.ServiceCenter.getServiceInstanceList(conn, null);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get service list, " + e.getMessage(), e);
        }
    }
}
