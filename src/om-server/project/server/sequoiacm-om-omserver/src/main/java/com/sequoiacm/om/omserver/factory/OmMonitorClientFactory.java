package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.module.monitor.OmMonitorInstanceInfo;
import com.sequoiacm.om.omserver.remote.OmMonitorClient;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface OmMonitorClientFactory {

    OmMonitorClient getClient(OmMonitorInstanceInfo instanceInfo, ScmOmSession session);
}
