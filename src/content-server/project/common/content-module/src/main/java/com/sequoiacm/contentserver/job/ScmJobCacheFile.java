package com.sequoiacm.contentserver.job;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.dao.FileCacheDao;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.infrastructure.lock.ScmLock;

public final class ScmJobCacheFile extends ScmBackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ScmJobCacheFile.class);

    private ScmWorkspaceInfo wsInfo;
    private String fileId;

    private int minorVersion;
    private int majorVersion;

    private String dataId;
    private int remoteSiteId;

    public ScmJobCacheFile(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, String dataId, int remoteSiteId) {
        this.wsInfo = wsInfo;
        this.fileId = fileId;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.dataId = dataId;
        this.remoteSiteId = remoteSiteId;
    }

    @Override
    public int getType() {
        return ServiceDefine.Job.JOB_TYPE_CACHE_FILE;
    }

    @Override
    public String getName() {
        return "JOB_TYPE_CACHE_FILE";
    }

    @Override
    public long getPeriod() {
        return 0;
    }

    @Override
    public void _run() {
        ScmLock fileContentLock = null;
        ScmLock readLock = null;
        try {
            ScmLockPath fileContentLockPath = ScmLockPathFactory.createFileContentLockPath(
                    wsInfo.getName(), ScmContentModule.getInstance().getLocalSiteInfo().getName(),
                    dataId);
            ScmLockPath fileReadLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            readLock = ScmLockManager.getInstance().acquiresReadLock(fileReadLockPath);
            fileContentLock = ScmLockManager.getInstance().acquiresLock(fileContentLockPath);
            BSONObject file = ScmContentModule
                    .getInstance()
                    .getMetaService()
                    .getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, majorVersion,
                            minorVersion, false);
            if (null == file) {
                throw new ScmFileNotFoundException("file is not exist:fileId=" + fileId);
            }
            FileCacheDao fileCache = new FileCacheDao(wsInfo, remoteSiteId);
            fileCache.doCache(file);
        }
        catch (Exception e) {
            logger.error("do cahce job failed:wsName={},fileId={},version={}.{}", wsInfo.getName(),
                    fileId, majorVersion, minorVersion, e);
        }
        finally {
            if (fileContentLock != null) {
                fileContentLock.unlock();
            }
            if (readLock != null) {
                readLock.unlock();
            }
        }
    }
}
