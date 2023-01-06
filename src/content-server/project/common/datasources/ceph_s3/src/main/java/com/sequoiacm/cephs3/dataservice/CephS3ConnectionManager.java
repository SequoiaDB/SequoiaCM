package com.sequoiacm.cephs3.dataservice;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.common.CephS3UserInfo;

/**
 * cephS3 Connection manage class
 */
public interface CephS3ConnectionManager {
    // 返回 null 表示 cephs3 可能已经无法连接或响应不识别
    CephS3ConnWrapper getConn(CephS3UserInfo primaryInfo, CephS3UserInfo standbyInfo)
            throws CephS3Exception;

    void release(CephS3ConnWrapper conn);

    // 返回 null 表示不可重试
    CephS3ConnWrapper releaseAndTryGetAnotherConn(CephS3ConnWrapper conn,
            CephS3UserInfo primaryInfo, CephS3UserInfo standbyInfo) throws CephS3Exception;

    void shutdown();
}
