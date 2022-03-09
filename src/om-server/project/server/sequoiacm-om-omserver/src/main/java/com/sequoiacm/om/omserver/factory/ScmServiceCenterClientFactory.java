package com.sequoiacm.om.omserver.factory;

import com.sequoiacm.om.omserver.remote.ScmServiceCenterFeignClient;

public interface ScmServiceCenterClientFactory {

    ScmServiceCenterFeignClient getClient(String url);
}
