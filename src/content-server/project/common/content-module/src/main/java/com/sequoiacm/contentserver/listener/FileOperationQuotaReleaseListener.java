package com.sequoiacm.contentserver.listener;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.exception.ScmServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileOperationQuotaReleaseListener implements FileOperationListener {

    @Autowired
    private BucketQuotaManager quotaManager;

    @Override
    public void preCreate(ScmWorkspaceInfo ws, FileMeta file) throws ScmServerException {

    }

    @Override
    public void postDelete(ScmWorkspaceInfo ws, List<FileMeta> allFileVersions)
            throws ScmServerException {

        for (FileMeta fileMeta : allFileVersions) {
            if (fileMeta.getBucketId() != null && !fileMeta.isDeleteMarker()) {
                quotaManager.releaseQuota(fileMeta.getBucketId(), fileMeta.getSize(),
                        fileMeta.getCreateTime());
            }
        }

    }

    @Override
    public OperationCompleteCallback postCreate(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {
        return OperationCompleteCallback.EMPTY_CALLBACK;
    }

    @Override
    public OperationCompleteCallback postAddVersion(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {
        return OperationCompleteCallback.EMPTY_CALLBACK;
    }

    @Override
    public void postDeleteVersion(ScmWorkspaceInfo ws, FileMeta fileMeta)
            throws ScmServerException {
        if (fileMeta.getBucketId() != null && !fileMeta.isDeleteMarker()) {
            quotaManager.releaseQuota(fileMeta.getBucketId(), fileMeta.getSize(),
                    fileMeta.getCreateTime());
        }
    }

    @Override
    public OperationCompleteCallback postUpdate(ScmWorkspaceInfo ws,
            FileMeta latestVersionBeforeUpdate, FileMeta latestVersionAfterUpdate)
            throws ScmServerException {
        // 这里处理的是桶内文件解除映射后额度释放的场景。由于目前暂时没有桶内文件从 A 桶挂载到 B 桶的场景，所以这里不识别这种情况
        if (latestVersionBeforeUpdate.getBucketId() != null
                && latestVersionAfterUpdate.getBucketId() == null) {
            quotaManager.releaseQuota(latestVersionBeforeUpdate.getBucketId(),
                    latestVersionAfterUpdate.getSize(), latestVersionAfterUpdate.getCreateTime());
        }
        return OperationCompleteCallback.EMPTY_CALLBACK;
    }

    @Override
    public void preAddVersion(ScmWorkspaceInfo ws, FileMeta newVersionFile)
            throws ScmServerException {

    }
}
