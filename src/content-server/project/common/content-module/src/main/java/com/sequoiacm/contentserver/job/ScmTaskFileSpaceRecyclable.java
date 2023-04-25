package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataRemovingSpaceRecycler;
import com.sequoiacm.datasource.dataoperation.ScmSpaceRecyclingInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import org.bson.BSONObject;

public abstract class ScmTaskFileSpaceRecyclable extends ScmTaskFile {

    private ScmDataRemovingSpaceRecycler dataRemovingSpaceRecycler;
    private boolean isRecycleSpace;

    public ScmTaskFileSpaceRecyclable(ScmTaskManager mgr, BSONObject info, boolean isAsyncCountFile)
            throws ScmServerException {
        super(mgr, info, isAsyncCountFile);
        BSONObject option = BsonUtils.getBSON(info, FieldName.Task.FIELD_OPTION);
        if (option != null) {
            isRecycleSpace = BsonUtils.getBooleanOrElse(option,
                    FieldName.Task.FIELD_OPTION_IS_RECYCLE_SPACE, false);
        }
        if (isRecycleSpace) {
            try {
                ScmContentModule contentModule = ScmContentModule.getInstance();
                this.dataRemovingSpaceRecycler = ScmDataOpFactoryAssit.getFactory()
                        .createDataRemovingSpaceRecycler(getWorkspaceInfo().getName(),
                                contentModule.getLocalSiteInfo().getName(),
                                getWorkspaceInfo().getDataLocationAllVersions(),
                                contentModule.getDataService());
            }
            catch (ScmDatasourceException e) {
                throw new ScmServerException(e.getScmError(ScmError.SYSTEM_ERROR),
                        "failed to create dataRemovingSpaceRecycler", e);
            }
        }

    }

    @Override
    protected void taskComplete() {
        if (dataRemovingSpaceRecycler != null) {
            dataRemovingSpaceRecycler.notifyComplete();
            ScmSpaceRecyclingInfo recyclingInfo = dataRemovingSpaceRecycler.getRecyclingInfo();
            taskInfoContext.recordExtraInfo(recyclingInfo);
        }
    }

    @Override
    protected void doFile(BSONObject fileInfo) throws ScmServerException {
        if (dataRemovingSpaceRecycler != null) {
            dataRemovingSpaceRecycler.notifyFileDataRemoving(fileInfo);
            ScmSpaceRecyclingInfo recyclingInfo = dataRemovingSpaceRecycler.getRecyclingInfo();
            taskInfoContext.recordExtraInfo(recyclingInfo);
        }
    }
}
