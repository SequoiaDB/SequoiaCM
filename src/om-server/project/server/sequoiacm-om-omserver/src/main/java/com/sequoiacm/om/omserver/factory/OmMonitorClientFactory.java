package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.remote.OmMonitorFeignClient;

public interface OmMonitorClientFactory {

    OmMonitorFeignClient getClient(String managementUrl);
}
