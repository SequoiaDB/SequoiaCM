package com.sequoiacm.contentserver.pipeline.file.core;

import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OverwriteFileCoreFilter implements Filter<OverwriteFileContext> {
    private static final Logger logger = LoggerFactory.getLogger(OverwriteFileCoreFilter.class);

    @Autowired
    private FileMetaFactory fileMetaFactory;
    @Autowired
    private CreateFileCoreFilter createFileCoreFilter;

    @Override
    public void preparePhase(OverwriteFileContext context) throws ScmServerException {
        if (context.isOverwrittenFileConflict()) {
            return;
        }

        // 检查预期被覆盖的文件是否与正在创建的文件 ID 冲突
        FileMeta overwrittenFile = context.getOverwrittenFile();
        if (overwrittenFile == null) {
            return;
        }

        if (overwrittenFile.getId().equals(context.getFileMeta().getId())) {
            context.setOverwrittenFileConflict(true);
        }
    }

    @Override
    public PipelineResult executionPhase(OverwriteFileContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());

        try {
            // 若预期被覆盖的文件与正在创建的文件冲突，则删除被覆盖的文件
            // 这个标记为 false 的情况：
            // 1. 创建文件，发现存在 A 文件冲突；
            // 2. 对 A 文件加锁，发起覆盖文件流程
            // 1、2 步骤的间隙，有人修改了 A 文件，就会导致步骤2覆盖的处理过程中，发现 A 文件又不与正在创建的文件冲突了
            if (context.isOverwrittenFileConflict()) {
                MetaFileAccessor currentFileAccessor = contentModule.getMetaService()
                        .getMetaSource().getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                                context.getTransactionContext());
                BSONObject deletedLatestVersionBSON = currentFileAccessor
                        .delete(context.getOverwrittenFile().getId(), -1, -1);
                if (deletedLatestVersionBSON != null) {
                    FileMeta deletedLatestVersion = fileMetaFactory
                            .createFileMetaByRecord(wsInfo.getName(), deletedLatestVersionBSON);
                    context.getDeleteVersion().add(deletedLatestVersion);
                    if (!deletedLatestVersion.isFirstVersion()) {
                        List<FileMeta> deletedHistoryVersion = queryAndDeleteHistoryVersion(
                                context.getTransactionContext(), wsInfo, deletedLatestVersion);
                        context.getDeleteVersion().addAll(deletedHistoryVersion);
                    }
                }

            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to create file, remove conflict file failed: ws=" + wsInfo.getName()
                            + ", fileId=" + context.getOverwrittenFile().getId(),
                    e);
        }
        return createFileCoreFilter.executionPhase(context);
    }

    private List<FileMeta> queryAndDeleteHistoryVersion(TransactionContext trans,
            ScmWorkspaceInfo wsInfo, FileMeta deletedLatestVersion)
            throws ScmServerException, ScmMetasourceException {
        MetaFileHistoryAccessor historyFileAccessor = ScmContentModule.getInstance()
                .getMetaService().getMetaSource()
                .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), trans);
        List<FileMeta> ret = new ArrayList<>();
        MetaCursor cursor = historyFileAccessor.queryAndDeleteWithCursor(
                deletedLatestVersion.getId(), deletedLatestVersion.toRecordBSON(), null, null);
        try {
            while (cursor.hasNext()) {
                ret.add(fileMetaFactory.createFileMetaByRecord(wsInfo.getName(), cursor.getNext()));
            }
        }
        finally {
            cursor.close();
        }
        return ret;
    }

}
