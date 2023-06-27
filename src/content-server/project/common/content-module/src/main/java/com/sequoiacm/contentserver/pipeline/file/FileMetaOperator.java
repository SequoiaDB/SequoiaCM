package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.pipeline.file.module.AddFileMetaVersionResult;
import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileMetaResult;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileResult;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileMetaResult;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class FileMetaOperator {
    private final Pipeline<CreateFileContext> createFilePipeline;
    private final Pipeline<OverwriteFileContext> overwriteFilePipeline;
    private final Pipeline<AddFileVersionContext> addFileVersionPipeline;
    private final Pipeline<DeleteFileContext> deleteFilePipeline;
    private final Pipeline<DeleteFileVersionContext> deleteFileVersionPipeline;
    private final Pipeline<UpdateFileMetaContext> updateFileMetaPipeline;

    private static final int MAX_REDO_COUNT = 3;

    private static final Logger logger = LoggerFactory.getLogger(FileMetaOperator.class);

    @Autowired
    FileMetaOperator(List<FileMetaOperatorRelationModule> relationModuleList,
            List<FileMetaOperatorCoreModule> coreModuleList, FileMetaFactory fileMetaFactory) {
        createFilePipeline = new Pipeline<>();
        overwriteFilePipeline = new Pipeline<>();
        addFileVersionPipeline = new AddFileVersionPipeline(fileMetaFactory);
        deleteFilePipeline = new Pipeline<>();
        deleteFileVersionPipeline = new Pipeline<>();
        updateFileMetaPipeline = new UpdateFileMetaPipeline(fileMetaFactory);

        // 各个 Pipeline filter 顺序：
        // 创建、新增版本、更新元数据、覆盖文件：relationTypeFilter->coreTypeFilter
        // 删除文件、删除版本：coreTypeFilter->relationTypeFilter
        //
        // 同类型 filter 由所属 module 的 priority 进行确定:
        // 创建、新增版本、更新元数据、覆盖文件: 按 priority 正序排
        // 删除文件、删除版本: 按 priority 逆序排

        int relationTypePriority = 1;
        int coreTypePriority = 2;
        for (FileMetaOperatorRelationModule relationModule : relationModuleList) {
            // 正序注册
            registerPipeline(createFilePipeline, relationModule.createFileFilter(),
                    relationTypePriority, relationModule.priority());
            registerPipeline(overwriteFilePipeline, relationModule.overwriteFileFilter(),
                    relationTypePriority, relationModule.priority());
            registerPipeline(addFileVersionPipeline, relationModule.addFileVersionFilter(),
                    relationTypePriority, relationModule.priority());
            registerPipeline(updateFileMetaPipeline, relationModule.updateFileMetaFilter(),
                    relationTypePriority, relationModule.priority());

            // 逆序注册
            registerPipeline(deleteFilePipeline, relationModule.deleteFileFilter(),
                    -relationTypePriority, -relationModule.priority());
            registerPipeline(deleteFileVersionPipeline, relationModule.deleteFileVersionFilter(),
                    -relationTypePriority, -relationModule.priority());
        }
        for (FileMetaOperatorCoreModule coreModule : coreModuleList) {
            // 正序注册
            registerPipeline(createFilePipeline, coreModule.createFileFilter(), coreTypePriority,
                    coreModule.priority());
            registerPipeline(overwriteFilePipeline, coreModule.overwriteFileFilter(),
                    coreTypePriority, coreModule.priority());
            registerPipeline(addFileVersionPipeline, coreModule.addFileVersionFilter(),
                    coreTypePriority, coreModule.priority());
            registerPipeline(updateFileMetaPipeline, coreModule.updateFileMetaFilter(),
                    coreTypePriority, coreModule.priority());

            // 逆序注册
            registerPipeline(deleteFilePipeline, coreModule.deleteFileFilter(), -coreTypePriority,
                    -coreModule.priority());
            registerPipeline(deleteFileVersionPipeline, coreModule.deleteFileVersionFilter(),
                    -coreTypePriority, -coreModule.priority());
        }
        logger.info("createFilePipeline: " + createFilePipeline.getFiltersDesc());
        logger.info("deleteFilePipeline: " + deleteFilePipeline.getFiltersDesc());
        logger.info("deleteFileVersionPipeline: " + deleteFileVersionPipeline.getFiltersDesc());
        logger.info("updateFileMetaPipeline: " + updateFileMetaPipeline.getFiltersDesc());
        logger.info("overwriteFilePipeline: " + overwriteFilePipeline.getFiltersDesc());
        logger.info("addFileVersionPipeline: " + addFileVersionPipeline.getFiltersDesc());

    }

    private <C> void registerPipeline(Pipeline<C> pipeline, Filter<C> filter,
            int filterModuleTypePriority, int filterModulePriority) {
        if (filter == null) {
            return;
        }
        pipeline.addFilter(new FilterComparableWrapper<>(filter, filterModuleTypePriority,
                filterModulePriority));
    }

    private <C> void invokePipelines(List<Pipeline<C>> pipelineList, C context,
            ContextRedoHandler<C> redoHandler) throws ScmServerException, ScmMetasourceException {
        int redoCount = 0;
        PipelineResult res = null;
        while (redoCount++ < MAX_REDO_COUNT) {
            res = invokePipelines(pipelineList, context);
            if (res.getStatus() == PipelineResult.Status.SUCCESS) {
                return;
            }
            if (res.getStatus() == PipelineResult.Status.REDO_PIPELINE) {
                logger.debug("try redo pipeline, silenceTime={}", res.getSilenceTimeMsBeforeRedo(),
                        res.getCause());
                sleep(res);
                redoHandler.beforeRedo(context);
                continue;
            }
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "unknown PipelineResult:" + res);
        }
        if (res == null) {
            // 不可能走到这个分支：MAX_REDO_COUNT <=0
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "invalid MAX_REDO_COUNT: " + MAX_REDO_COUNT);
        }
        if (res.getCause() != null) {
            throw res.getCause();
        }
        throw new ScmServerException(ScmError.SYSTEM_ERROR, "failed to invoke pipeline");
    }

    private void sleep(PipelineResult res) throws ScmServerException {
        if (res.getSilenceTimeMsBeforeRedo() > 0) {
            try {
                Thread.sleep(res.getSilenceTimeMsBeforeRedo());
            }
            catch (InterruptedException e) {
                logger.error("failed to sleep", e);
                throw res.getCause();
            }
        }
    }

    private <C> PipelineResult invokePipelines(List<Pipeline<C>> pipelineList, C context)
            throws ScmServerException {
        for (Pipeline<C> p : pipelineList) {
            PipelineResult res = p.execute(context);
            if (res.getStatus() != PipelineResult.Status.SUCCESS) {
                return res;
            }
        }
        return PipelineResult.success();
    }

    public UpdateFileMetaResult updateFileMeta(String ws, String fileId,
            List<FileMetaUpdater> fileMetaUpdaters, String updateUser, Date updateTime,
            FileMeta currentLatestVersion, ScmVersion expectVersion) throws ScmServerException {
        UpdateFileMetaContext updateFileMetaContext = new UpdateFileMetaContext();
        updateFileMetaContext.setFileId(fileId);
        updateFileMetaContext.setWs(ws);
        updateFileMetaContext.addFileMetaUpdater(fileMetaUpdaters);
        updateFileMetaContext.setUpdateUser(updateUser);
        updateFileMetaContext.setUpdateTime(updateTime);
        updateFileMetaContext.setExpectVersion(expectVersion);
        updateFileMetaContext.setCurrentLatestVersion(currentLatestVersion);

        TransactionContext trans = null;
        try {
            trans = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .createTransactionContext();
            updateFileMetaContext.setTransactionContext(trans);
            trans.begin();

            invokePipelines(Collections.singletonList(updateFileMetaPipeline),
                    updateFileMetaContext, (UpdateFileMetaContext c) -> {
                        c.getTransactionContext().rollback();
                        c.getTransactionContext().begin();
                    });
            trans.commit();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to update file: ws=" + ws + ", fileId=" + fileId, e);
        }
        finally {
            if (trans != null) {
                trans.close();
            }
        }

        UpdateFileMetaResult updateFileMetaResult = new UpdateFileMetaResult();
        updateFileMetaResult
                .setSpecifiedReturnVersion(updateFileMetaContext.getExpectUpdatedFileMeta());
        updateFileMetaResult
                .setLatestVersionAfterUpdate(updateFileMetaContext.getLatestVersionAfterUpdate());
        return updateFileMetaResult;
    }

    public DeleteFileVersionResult deleteFileVersionMeta(String ws, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        DeleteFileVersionContext deleteFileVersionContext = new DeleteFileVersionContext();
        deleteFileVersionContext.setFileId(fileId);
        deleteFileVersionContext.setWs(ws);
        deleteFileVersionContext.setMajorVersion(majorVersion);
        deleteFileVersionContext.setMinorVersion(minorVersion);
        TransactionContext trans = null;
        try {
            trans = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .createTransactionContext();
            deleteFileVersionContext.setTransactionContext(trans);
            trans.begin();

            invokePipelines(Collections.singletonList(deleteFileVersionPipeline),
                    deleteFileVersionContext, (DeleteFileVersionContext c) -> {
                        c.getTransactionContext().rollback();
                        c.getTransactionContext().begin();
                    });
            trans.commit();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file: ws=" + ws + ", fileId=" + fileId, e);
        }
        finally {
            if (trans != null) {
                trans.close();
            }
        }

        DeleteFileVersionResult deleteFileVersionResult = new DeleteFileVersionResult();
        deleteFileVersionResult.setDeletedVersion(deleteFileVersionContext.getDeletedVersion());
        return deleteFileVersionResult;
    }

    public DeleteFileResult deleteFileMeta(String ws, String fileId) throws ScmServerException {
        DeleteFileContext deleteFileContext = new DeleteFileContext();
        deleteFileContext.setFileId(fileId);
        deleteFileContext.setWs(ws);
        TransactionContext trans = null;
        try {
            trans = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .createTransactionContext();
            deleteFileContext.setTransactionContext(trans);
            trans.begin();

            invokePipelines(Collections.singletonList(deleteFilePipeline), deleteFileContext,
                    (DeleteFileContext c) -> {
                        c.getTransactionContext().rollback();
                        c.getTransactionContext().begin();
                    });
            trans.commit();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file: ws=" + ws + ", fileId=" + fileId, e);
        }
        finally {
            if (trans != null) {
                trans.close();
            }
        }

        DeleteFileResult deleteFileResult = new DeleteFileResult();
        deleteFileResult.getDeletedVersion().addAll(deleteFileContext.getDeletedHistoryVersions());
        deleteFileResult.getDeletedVersion().add(deleteFileContext.getDeletedLatestVersion());
        return deleteFileResult;
    }

    public OverwriteFileMetaResult overwriteFileMeta(String ws, final FileMeta fileMeta,
            TransactionCallback transCallback, FileMeta overwrittenFile) throws ScmServerException {
        OverwriteFileContext context = new OverwriteFileContext();
        context.setWs(ws);
        context.setFileMeta(fileMeta.clone());
        context.setOverwrittenFile(overwrittenFile);
        TransactionContext trans = null;
        try {
            trans = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .createTransactionContext();
            context.setTransactionContext(trans);
            trans.begin();

            invokePipelines(Collections.singletonList(overwriteFilePipeline), context,
                    (OverwriteFileContext c) -> {
                        c.getTransactionContext().rollback();
                        c.getTransactionContext().begin();
                        c.setFileMeta(fileMeta.clone());
                    });
            if (transCallback != null) {
                transCallback.beforeTransactionCommit(trans);
            }
            trans.commit();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to overwrite file: ws=" + ws
                    + ", overwrittenFile=" + (overwrittenFile == null ? null : overwrittenFile.getId()), e);
        }
        finally {
            if (trans != null) {
                trans.close();
            }
        }
        OverwriteFileMetaResult res = new OverwriteFileMetaResult();
        res.setDeletedVersion(context.getDeleteVersion());
        res.setNewFile(context.getFileMeta());
        return res;
    }

    public AddFileMetaVersionResult addFileMetaVersion(String ws, FileMeta newVersion,
            FileMeta currentLatestVersion, TransactionCallback transCallback)
            throws ScmServerException {
        AddFileVersionContext context = new AddFileVersionContext();
        context.setFileId(currentLatestVersion.getId());
        context.setWs(ws);
        context.setNewVersion(newVersion);
        context.setCurrentLatestVersion(currentLatestVersion);
        TransactionContext trans = null;
        try {
            trans = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .createTransactionContext();
            context.setTransactionContext(trans);
            trans.begin();

            invokePipelines(Collections.singletonList(addFileVersionPipeline), context,
                    (AddFileVersionContext c) -> {
                        c.getTransactionContext().rollback();
                        c.getTransactionContext().begin();
                        c.setNewVersion(newVersion);
                    });
            if (transCallback != null) {
                transCallback.beforeTransactionCommit(trans);
            }
            trans.commit();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to add file version: ws=" + ws
                    + ", fileId=" + currentLatestVersion.getId(), e);
        }
        finally {
            if (trans != null) {
                trans.close();
            }
        }
        AddFileMetaVersionResult res = new AddFileMetaVersionResult();
        res.setNewVersion(context.getNewVersion());
        res.setDeletedVersion(context.getDeletedVersion());
        return res;
    }

    public CreateFileMetaResult createFileMeta(String ws, final FileMeta fileMeta,
            TransactionCallback transCallback) throws ScmServerException {
        CreateFileContext context = new CreateFileContext();
        context.setWs(ws);
        context.setFileMeta(fileMeta.clone());
        TransactionContext trans = null;
        try {
            trans = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .createTransactionContext();
            context.setTransactionContext(trans);
            trans.begin();

            invokePipelines(Collections.singletonList(createFilePipeline), context,
                    (CreateFileContext c) -> {
                        c.getTransactionContext().rollback();
                        c.getTransactionContext().begin();
                        c.setFileMeta(fileMeta.clone());
                    });

            if (transCallback != null) {
                transCallback.beforeTransactionCommit(trans);
            }
            trans.commit();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to create file: ws=" + ws + ", " + fileMeta.getSimpleDesc(), e);
        }
        finally {
            if (trans != null) {
                trans.close();
            }
        }

        return new CreateFileMetaResult(context.getFileMeta());
    }
}

interface ContextRedoHandler<C> {
    void beforeRedo(C context) throws ScmServerException, ScmMetasourceException;
}

class FilterComparableWrapper<C> implements Comparable<FilterComparableWrapper<C>> {

    private final Filter<C> innerFilter;
    private final int firstPriority;
    private final int secondPriority;

    public FilterComparableWrapper(Filter<C> innerFilter, int firstPriority, int secondPriority) {
        this.innerFilter = innerFilter;
        this.firstPriority = firstPriority;
        this.secondPriority = secondPriority;
    }

    @Override
    public int compareTo(FilterComparableWrapper o) {
        if (firstPriority > o.firstPriority) {
            return 1;
        }
        if (firstPriority < o.firstPriority) {
            return -1;
        }
        if (secondPriority > o.secondPriority) {
            return 1;
        }
        if (secondPriority < o.secondPriority) {
            return -1;
        }
        return 0;
    }

    public Filter<C> getInnerFilter() {
        return innerFilter;
    }

    @Override
    public String toString() {
        return innerFilter.getClass().getSimpleName();
    }
}
