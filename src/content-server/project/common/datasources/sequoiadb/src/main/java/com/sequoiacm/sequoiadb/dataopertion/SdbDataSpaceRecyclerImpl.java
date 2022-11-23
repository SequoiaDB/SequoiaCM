package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.dataoperation.ScmDataSpaceRecycler;
import com.sequoiacm.datasource.dataoperation.ScmSpaceRecyclingCallback;
import com.sequoiacm.datasource.dataoperation.ScmSpaceRecyclingInfo;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.common.CommonDefine;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiacm.sequoiadb.metaoperation.MetaDataOperator;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SdbDataSpaceRecyclerImpl implements ScmDataSpaceRecycler {

    private static final Logger logger = LoggerFactory.getLogger(SdbDataSpaceRecyclerImpl.class);

    private String wsName;
    private String siteName;
    private final SdbDataService service;
    private final MetaDataOperator metaDataOperator;
    private List<String> recyclableCsNames;
    private ScmLockManager lockManager;

    public SdbDataSpaceRecyclerImpl(MetaSource metaSource, List<String> allTableNames,
                                    Date recycleBeginningTime, Date recycleEndingTIme, String wsName, String siteName,
                                    SdbDataService service, ScmLockManager lockManager) {
        this.metaDataOperator = new MetaDataOperator(metaSource, wsName, siteName,
                service.getSiteId());
        this.wsName = wsName;
        this.siteName = siteName;
        this.service = service;
        this.recyclableCsNames = getRecyclableCsNames(allTableNames, recycleBeginningTime,
                recycleEndingTIme);
        this.lockManager = lockManager;
    }

    @Override
    public ScmSpaceRecyclingInfo recycle(long maxExecTime, ScmSpaceRecyclingCallback callback) {
        if (callback == null) {
            callback = ScmSpaceRecyclingCallback.DEFAULT_CALLBACK;
        }
        long startTime = System.currentTimeMillis();
        ScmSpaceRecyclingInfo scmSpaceRecyclingInfo = new ScmSpaceRecyclingInfo();
        if (recyclableCsNames == null || recyclableCsNames.size() <= 0) {
            return scmSpaceRecyclingInfo;
        }
        BasicBSONList deletedCsList = new BasicBSONList();
        for (String csName : recyclableCsNames) {
            if (!callback.shouldContinue()) {
                break;
            }
            boolean deleted = false;
            try {
                deleted = SdbCsRecycleHelper.deleteCsIfEmpty(csName, siteName, service,
                        metaDataOperator, lockManager);
                if (deleted) {
                    scmSpaceRecyclingInfo
                            .setSuccessCount(scmSpaceRecyclingInfo.getSuccessCount() + 1);
                    deletedCsList.add(csName);
                }
            }
            catch (Exception e) {
                scmSpaceRecyclingInfo.setFailedCount(scmSpaceRecyclingInfo.getFailedCount() + 1);
                logger.error("failed to try delete collectionSpace:{}", csName, e);
            }

            if (maxExecTime > 0 && System.currentTimeMillis() - startTime > maxExecTime) {
                scmSpaceRecyclingInfo.setTimeout(true);
                break;
            }
        }
        if (deletedCsList.size() > 0) {
            scmSpaceRecyclingInfo.setInfo(new BasicBSONObject(
                    CommonDefine.SPACE_RECYCLING_REMOVED_COLLECTION_SPACE, deletedCsList));
        }
        return scmSpaceRecyclingInfo;
    }

    @Override
    public int getRecyclableTableCount() {
        return recyclableCsNames.size();
    }

    private List<String> getRecyclableCsNames(List<String> allCsNames, Date recycleBeginningTime,
            Date recycleEndingTIme) {
        List<String> recyclableCsNames = new ArrayList<>();

        for (String csName : allCsNames) {
            Date csShardingTime = SdbDataLocation.getCsShardingBeginningTime(csName, wsName);
            if (csShardingTime == null) {
                recyclableCsNames.add(csName);
            }
            else if (csShardingTime.after(recycleBeginningTime)
                    && csShardingTime.before(recycleEndingTIme)) {
                recyclableCsNames.add(csName);
            }
        }
        return recyclableCsNames;
    }
}
