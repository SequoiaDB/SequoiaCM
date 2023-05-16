package com.sequoiacm.contentserver.pipeline.file.core;

import com.sequoiacm.contentserver.common.FileTableCreator;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaExistException;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateFileCoreFilter implements Filter<CreateFileContext> {
    private static final Logger logger = LoggerFactory.getLogger(CreateFileCoreFilter.class);

    @Override
    public PipelineResult executionPhase(CreateFileContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());

        MetaFileAccessor fileAccessor = contentModule.getMetaService().getMetaSource()
                .getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                        context.getTransactionContext());
        BSONObject fileRecord = context.getFileMeta().toRecordBSON();
        try {
            fileAccessor.insert(fileRecord);
        }
        catch (ScmMetasourceException e) {
            if (e.getScmError() == ScmError.FILE_TABLE_NOT_FOUND) {
                try {
                    FileTableCreator.createSubFileTable(
                            (SdbMetaSource) contentModule.getMetaService().getMetaSource(), wsInfo,
                            fileRecord);
                }
                catch (Exception ex) {
                    throw new ScmServerException(ScmError.METASOURCE_ERROR,
                            "insert file failed, create file table failed:ws=" + wsInfo.getName()
                                    + ", file=" + context.getFileMeta().getId(),
                            ex);
                }
                return PipelineResult.redo(new ScmServerException(e.getScmError(),
                        "failed to create file table:" + context.getFileMeta().getSimpleDesc(), e));
            }
            if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                throw new FileMetaExistException("insert file failed, file exist:ws="
                        + wsInfo.getName() + ", file=" + context.getFileMeta().getId(), e,
                        context.getFileMeta().getId());
            }
            throw new ScmServerException(e.getScmError(),
                    "insert file failed： ws=" + wsInfo.getName()
                            + ", file=" + context.getFileMeta().getId(),
                    e);
        }
        return PipelineResult.success();
    }
}
