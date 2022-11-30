package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.datasource.dataoperation.ScmDataRemovingSpaceRecycler;
import com.sequoiacm.datasource.dataoperation.ScmSpacePartitionInfo;
import com.sequoiacm.datasource.dataoperation.ScmSpaceRecyclingInfo;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.common.CommonDefine;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiacm.sequoiadb.metaoperation.MetaDataOperator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SdbDataRemovingSpaceRecyclerImpl implements ScmDataRemovingSpaceRecycler {

    private static final Logger logger = LoggerFactory
            .getLogger(SdbDataRemovingSpaceRecyclerImpl.class);

    private static final int RETAIN_PARTITION_COUNT = 2;
    private static final int MAX_ALLOW_RECYCLING_COUNT = 3;

    private String wsName;
    private String siteName;
    private final Map</* ws_version */Integer, ScmLocation> locations;
    private final SdbDataService service;
    private final MetaDataOperator metaDataOperator;

    private final List<ScmSpacePartitionInfo> partitionInfoList = new ArrayList<>();
    private final Set<String> deletedCsSet = new HashSet<>();
    private final Set<String> deleteFailedCsSet = new HashSet<>();
    private ScmLockManager lockManager;

    public SdbDataRemovingSpaceRecyclerImpl(MetaSource metaSource, String wsName, String siteName,
                                            Map<Integer, ScmLocation> locations, SdbDataService service, ScmLockManager lockManager) {
        this.metaDataOperator = new MetaDataOperator(metaSource, wsName, siteName,
                service.getSiteId());
        this.wsName = wsName;
        this.siteName = siteName;
        this.locations = locations;
        this.service = service;
        this.lockManager = lockManager;
    }

    @Override
    public void notifyFileDataRemoving(BSONObject fileInfo) {
        long dataCreateTime = BsonUtils.getLong(fileInfo,
                FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(BsonUtils.getArray(fileInfo, FieldName.FIELD_CLFILE_FILE_SITE_LIST));
        ScmLocation location = locations
                .get(fileLocationMap.get(service.getSiteId()).getWsVersion());
        if (location != null) {
            String csName = ((SdbDataLocation) location).getDataCsName(wsName, new Date(dataCreateTime));
            ScmSpacePartitionInfo partitionInfo = new ScmSpacePartitionInfo(csName);
            if (!partitionInfoList.contains(partitionInfo)) {
                partitionInfoList.add(partitionInfo);
            }
        }
        if (partitionInfoList.size() > RETAIN_PARTITION_COUNT) {
            tryDeletePartitions(partitionInfoList.size() - RETAIN_PARTITION_COUNT,
                    MAX_ALLOW_RECYCLING_COUNT);
        }
    }

    private void tryDeletePartitions(int count, int maxAllowRecyclingCount) {
        Iterator<ScmSpacePartitionInfo> iterator = partitionInfoList.iterator();
        while (iterator.hasNext() && --count >= 0) {
            ScmSpacePartitionInfo partition = iterator.next();
            if (partition.getRecyclingCount() >= maxAllowRecyclingCount) {
                continue;
            }
            partition.setRecyclingCount(partition.getRecyclingCount() + 1);
            String csName = partition.getCsName();
            try {
                boolean deleted = SdbCsRecycleHelper.deleteCsIfEmpty(csName, siteName, service,
                        metaDataOperator, lockManager);
                if (deleted) {
                    iterator.remove();
                    deletedCsSet.add(csName);
                    deleteFailedCsSet.remove(csName);
                }
            } catch (Exception e) {
                logger.error("failed to try delete collectionSpace:{}", csName, e);
                deleteFailedCsSet.add(csName);
            }
        }
    }

    @Override
    public void notifyComplete() {
        tryDeletePartitions(partitionInfoList.size(), Integer.MAX_VALUE);
    }

    @Override
    public ScmSpaceRecyclingInfo getRecyclingInfo() {
        ScmSpaceRecyclingInfo scmSpaceRecyclingInfo = new ScmSpaceRecyclingInfo();
        scmSpaceRecyclingInfo.setFailedCount(deleteFailedCsSet.size());
        scmSpaceRecyclingInfo.setSuccessCount(deletedCsSet.size());
        BasicBSONList deletedCsBsonList = new BasicBSONList();
        deletedCsBsonList.addAll(deletedCsSet);
        scmSpaceRecyclingInfo.setInfo(new BasicBSONObject(
                CommonDefine.SPACE_RECYCLING_REMOVED_COLLECTION_SPACE, deletedCsBsonList));
        return scmSpaceRecyclingInfo;
    }
}
