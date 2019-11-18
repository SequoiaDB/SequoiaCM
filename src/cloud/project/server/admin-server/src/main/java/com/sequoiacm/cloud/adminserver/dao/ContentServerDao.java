package com.sequoiacm.cloud.adminserver.dao;

import java.util.List;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;

public interface ContentServerDao {
    public List<ContentServerInfo> queryAll() throws StatisticsException;
}
