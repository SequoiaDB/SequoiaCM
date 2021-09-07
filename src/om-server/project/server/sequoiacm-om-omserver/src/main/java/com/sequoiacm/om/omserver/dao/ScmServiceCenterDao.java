package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.om.omserver.exception.ScmInternalException;

import java.util.List;

public interface ScmServiceCenterDao {

    public List<ScmServiceInstance> getServiceList() throws ScmInternalException;
}
