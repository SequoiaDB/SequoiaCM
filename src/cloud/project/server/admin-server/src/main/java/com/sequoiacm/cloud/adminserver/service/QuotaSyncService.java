package com.sequoiacm.cloud.adminserver.service;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import org.bson.BSONObject;
import org.springframework.security.core.Authentication;

public interface QuotaSyncService {
    void sync(String type, String name, boolean isFirstSync, Authentication auth)
            throws StatisticsException;

    void cancelSync(String type, String name, Authentication auth) throws StatisticsException;

    void startSyncTask(String type, String name, boolean isFirstSync) throws StatisticsException;

    BSONObject getInnerDetail(String type, String name, Authentication auth)
            throws StatisticsException;
}
