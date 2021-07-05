package com.sequoiacm.cephs3.dataservice;

import com.sequoiacm.cephs3.CephS3Exception;

public interface ConnectionDecider {
    CephS3ConnWrapper getConn() throws CephS3Exception;

    void release(CephS3ConnWrapper conn);

    CephS3ConnWrapper releaseAndTryGetAnotherConn(CephS3ConnWrapper conn);

    void shutdown();
}
