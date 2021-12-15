package com.sequoiacm.cloud.adminserver.dao;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.model.BreakpointFileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.BreakpointFileStatisticsDataKey;

public interface BreakpointFileStatisticsDao {

    long getTotalUploadTime(String fileName, String workspace, long createTime)
            throws ScmMetasourceException;

    void saveBreakpointFileRecord(BreakpointFileStatisticsData record) throws ScmMetasourceException;

    void incrTotalUploadTime(BreakpointFileStatisticsDataKey key, long uploadTime)
            throws ScmMetasourceException;

    void deleteBreakpointFileRecord(String fileName, String workspace, long createTime)
            throws ScmMetasourceException;

    void clearRecords(long maxStayDay) throws ScmMetasourceException;

    boolean exist(BreakpointFileStatisticsDataKey key) throws ScmMetasourceException;
}
