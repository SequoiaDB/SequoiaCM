package com.sequoiacm.contentserver.pipeline.file.dir;

import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.pipeline.file.bucket.CreateFileBucketFilter;
import com.sequoiacm.metasource.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;

@Component
public class OverwriteFileDirFilter implements Filter<OverwriteFileContext> {
    private static final Logger logger = LoggerFactory.getLogger(CreateFileBucketFilter.class);

    @Autowired
    private CreateFileDirFilter createFileDirFilter;

    @Override
    public void preparePhase(OverwriteFileContext context) throws ScmServerException {
        if (context.isOverwrittenFileConflict()) {
            return;
        }

        // 检查预期被覆盖的文件是否与正在创建的文件在目录下冲突
        FileMeta overwrittenFile = context.getOverwrittenFile();
        if (overwrittenFile == null) {
            return;
        }

        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(context.getWs());
        if (!wsInfo.isEnableDirectory()) {
            return;
        }

        if (context.getOverwrittenFile().getDirId().equals(context.getFileMeta().getDirId())
                && context.getOverwrittenFile().getName().equals(context.getFileMeta().getName())) {
            context.setOverwrittenFileConflict(true);
        }
    }

    @Override
    public PipelineResult executionPhase(OverwriteFileContext context) throws ScmServerException {
        if (context.getOverwrittenFile() == null) {
            return createFileDirFilter.executionPhase(context);
        }

        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());
        if (!wsInfo.isEnableDirectory()) {
            return PipelineResult.success();
        }

        try {
            // 若预期被覆盖的文件与正在创建的文件冲突，则删除冲突文件的目录关系表记录
            if (context.isOverwrittenFileConflict()) {
                deleteDirRelation(wsInfo.getName(), context.getOverwrittenFile(),
                        context.getTransactionContext());
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete conflict file dir relation: dirId="
                            + context.getOverwrittenFile().getDirId() + ", fileName="
                            + context.getOverwrittenFile().getName());
        }
        return createFileDirFilter.executionPhase(context);
    }

    private void deleteDirRelation(String ws, FileMeta file, TransactionContext trans)
            throws ScmServerException, ScmMetasourceException {
        MetaRelAccessor dirAccessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource().getRelAccessor(ws, trans);
        dirAccessor.deleteRel(file.getId(), file.getDirId(), file.getName());
    }
}
