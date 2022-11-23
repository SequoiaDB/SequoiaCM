package com.sequoiacm.datasource.dataoperation;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import org.bson.BSONObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ScmDataOpFactory {
    ScmDataWriter createWriter(int siteId, String wsName, ScmLocation location, ScmService service,
            ScmDataInfo dataInfo) throws ScmDatasourceException;

    ScmDataReader createReader(int siteId, String wsName, ScmLocation location, ScmService service,
            ScmDataInfo dataInfo) throws ScmDatasourceException;

    ScmDataDeletor createDeletor(int siteId, String wsName, ScmLocation location,
            ScmService service, ScmDataInfo dataInfo) throws ScmDatasourceException;

    ScmBreakpointDataWriter createBreakpointWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws ScmDatasourceException;

    ScmSeekableDataWriter createSeekableDataWriter(ScmLocation location, ScmService service,
            String wsName, String fileName, String dataId, Date createTime, boolean createData,
            long writeOffset, BSONObject extraContext) throws ScmDatasourceException;

    ScmDataTableDeletor createDataTableDeletor(List<String> tableNames, ScmService service)
            throws ScmDatasourceException;

    default ScmDataRemovingSpaceRecycler createDataRemovingSpaceRecycler(String wsName,
            String siteName, Map<Integer, ScmLocation> locations, ScmService service)
            throws ScmDatasourceException {
        return new NoOpDataRemovingSpaceRecycler();
    }

    default ScmDataSpaceRecycler createScmDataSpaceRecycler(List<String> tableNames,
                                                            Date recycleBeginningTime, Date recycleEndingTIme, String wsName, String siteName,
                                                            ScmService service) throws ScmDatasourceException {
        return new NoOpDataSpaceRecycler();
    }

    default void init(MetaSource metaSource, ScmLockManager lockManager) {

    }

    default ScmDataInfoFetcher createDataInfoFetcher(int siteId, String wsName,
            ScmLocation location, ScmService service, ScmDataInfo dataInfo)
            throws ScmDatasourceException {
        final ScmDataReader reader = createReader(siteId, wsName, location, service, dataInfo);
        try {
            final long size = reader.getSize();
            return new ScmDataInfoFetcher() {
                @Override
                public long getDataSize() {
                    return size;
                }
            };
        }
        finally {
            reader.close();
        }
    }
}
