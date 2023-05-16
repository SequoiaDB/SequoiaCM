package com.sequoiacm.contentserver.pipeline.file.core;

import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UpdateFileMetaCoreFilter implements Filter<UpdateFileMetaContext> {
    @Autowired
    private FileMetaFactory fileMetaFactory;

    @Override
    public PipelineResult executionPhase(UpdateFileMetaContext context) throws ScmServerException {
        // 根据context生成最新版本、历史版本的 BSON updater
        Updater updater = genUpdater(context);

        try {
            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                    .getWorkspaceInfoCheckExist(context.getWs());
            ContentModuleMetaSource metasource = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource();
            MetaFileHistoryAccessor historyAccessor = metasource.getFileHistoryAccessor(
                    wsInfo.getMetaLocation(), wsInfo.getName(), context.getTransactionContext());
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), context.getTransactionContext());

            // 更新最新版本
            updateLatestVersion(context, updater.latestVersionUpdater, fileAccessor);
            // 更新历史版本（全局更新，每个版本都需要更新相同值）
            updateAllHistoryVersion(context, historyAccessor, updater.historyVersionGlobalUpdater);
            // 更新历史版本（特定更新，每个版本有自己的值）
            updateSpecifiedHistoryVersion(context, updater.historyVersionMapUpdater,
                    historyAccessor);

        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to update file info: ws="
                    + context.getWs() + ", fileId=" + context.getFileId(), e);
        }

        return PipelineResult.success();
    }

    private void updateAllHistoryVersion(UpdateFileMetaContext context,
            MetaFileHistoryAccessor historyAccessor, BSONObject historyVersionUpdater)
            throws ScmMetasourceException, ScmServerException {
        if (historyVersionUpdater == null || historyVersionUpdater.isEmpty()) {
            return;
        }
        BSONObject idMatcher = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(idMatcher, context.getFileId());
        MetaCursor cursor = historyAccessor.queryAndUpdate(idMatcher, historyVersionUpdater);
        try {
            while (cursor.hasNext()) {
                FileMeta fileMeta = fileMetaFactory.createFileMetaByRecord(context.getWs(),
                        cursor.getNext());
                context.recordUpdatedFileMeta(fileMeta);
            }
        }
        finally {
            cursor.close();
        }
    }

    private void updateSpecifiedHistoryVersion(UpdateFileMetaContext context,
            Map<ScmVersion, BSONObject> historyVersionMapUpdater,
            MetaFileHistoryAccessor historyAccessor)
            throws ScmMetasourceException, ScmServerException {
        if (historyVersionMapUpdater == null) {
            return;
        }
        for (Map.Entry<ScmVersion, BSONObject> entry : historyVersionMapUpdater.entrySet()) {
            BSONObject ret = historyAccessor.updateFileInfo(context.getFileId(),
                    entry.getKey().getMajorVersion(), entry.getKey().getMinorVersion(),
                    entry.getValue());
            if (ret == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file version not found: ws=" + context.getWs() + ", fileId="
                                + context.getFileId() + ", majorVersion="
                                + entry.getKey().getMajorVersion() + ", minorVersion="
                                + entry.getKey().getMinorVersion());
            }
            FileMeta fileMeta = fileMetaFactory.createFileMetaByRecord(context.getWs(), ret);
            if (context.getExpectVersion().equals(
                    new ScmVersion(fileMeta.getMajorVersion(), fileMeta.getMinorVersion()))) {
                context.recordUpdatedFileMeta(fileMeta);
            }
        }
    }

    private void updateLatestVersion(UpdateFileMetaContext context, BSONObject latestVersionUpdater,
            MetaFileAccessor fileAccessor) throws ScmMetasourceException, ScmServerException {
        if (latestVersionUpdater != null && !latestVersionUpdater.isEmpty()) {
            BSONObject idMatcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(idMatcher, context.getFileId());
            BSONObject ret = fileAccessor.queryAndUpdate(idMatcher, latestVersionUpdater, null,
                    true);
            if (ret == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "failed to update file, file not found: ws=" + context.getWs() + ", fileId="
                                + context.getFileId());
            }
            FileMeta latestVersionAfterUpdate = fileMetaFactory
                    .createFileMetaByRecord(context.getWs(), ret);
            context.setLatestVersionAfterUpdate(latestVersionAfterUpdate);
            context.recordUpdatedFileMeta(latestVersionAfterUpdate);
            return;
        }
        context.setLatestVersionAfterUpdate(context.getCurrentLatestVersion());
    }

    private Updater genUpdater(UpdateFileMetaContext context) throws ScmServerException {
        BSONObject latestVersionUpdater = new BasicBSONObject();
        Map<ScmVersion, BSONObject> historyVersionMapUpdater = new HashMap<>();
        BSONObject historyVersionGlobalUpdater = new BasicBSONObject();

        for (FileMetaUpdater updater : context.getFileMetaUpdaterList()) {
            if (updater.isGlobal()) {
                updater.injectFileUpdater(historyVersionGlobalUpdater);
                updater.injectFileUpdater(latestVersionUpdater);
                continue;
            }

            if (ScmFileVersionHelper.isLatestVersion(updater.getVersion(),
                    context.getCurrentLatestVersion().getVersion())) {
                updater.injectFileUpdater(latestVersionUpdater);
                continue;
            }

            BSONObject historyVersionUpdater = historyVersionMapUpdater.get(updater.getVersion());
            if (historyVersionUpdater == null) {
                historyVersionUpdater = new BasicBSONObject();
                historyVersionMapUpdater.put(updater.getVersion(), historyVersionUpdater);
            }
            updater.injectFileUpdater(historyVersionUpdater);
        }

        if (context.containAllHistoryVersionUpdater()) {
            BSONObject historyVersionUpdater = null;
            for (BSONObject updater : historyVersionMapUpdater.values()) {
                updater.putAll(historyVersionGlobalUpdater);
                historyVersionUpdater = updater;
            }

            // 检查所有历史版本的 updater，如果所有 updater 一致，后面流程可以优化成一条更新语句压到历史表
            boolean isAllHistoryVersionUpdaterSame = true;
            for (BSONObject updater : historyVersionMapUpdater.values()) {
                if (!updater.equals(historyVersionUpdater)) {
                    isAllHistoryVersionUpdaterSame = false;
                    break;
                }
            }

            if (isAllHistoryVersionUpdaterSame) {
                historyVersionGlobalUpdater = historyVersionUpdater;
                historyVersionMapUpdater.clear();
            }
        }

        return new Updater(latestVersionUpdater, historyVersionMapUpdater,
                historyVersionGlobalUpdater);
    }
}

class Updater {
    BSONObject latestVersionUpdater = new BasicBSONObject();
    Map<ScmVersion, BSONObject> historyVersionMapUpdater = new HashMap<>();
    BSONObject historyVersionGlobalUpdater = new BasicBSONObject();

    public Updater(BSONObject latestVersionUpdater,
            Map<ScmVersion, BSONObject> historyVersionMapUpdater,
            BSONObject historyVersionGlobalUpdater) {
        this.latestVersionUpdater = latestVersionUpdater;
        this.historyVersionMapUpdater = historyVersionMapUpdater;
        this.historyVersionGlobalUpdater = historyVersionGlobalUpdater;
    }

    public BSONObject getLatestVersionUpdater() {
        return latestVersionUpdater;
    }

    public Map<ScmVersion, BSONObject> getHistoryVersionMapUpdater() {
        return historyVersionMapUpdater;
    }

    public BSONObject getHistoryVersionGlobalUpdater() {
        return historyVersionGlobalUpdater;
    }
}
