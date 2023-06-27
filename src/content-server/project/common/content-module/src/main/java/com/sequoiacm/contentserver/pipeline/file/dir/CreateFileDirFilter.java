package com.sequoiacm.contentserver.pipeline.file.dir;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaExistException;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateFileDirFilter implements Filter<CreateFileContext> {
    private static final Logger logger = LoggerFactory.getLogger(CreateFileDirFilter.class);

    @Override
    public PipelineResult executionPhase(CreateFileContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());

        FileMeta fileMeta = context.getFileMeta();
        if (fileMeta.getDirId() == null || fileMeta.getDirId().trim().isEmpty()) {
            fileMeta.setDirId(CommonDefine.Directory.SCM_ROOT_DIR_ID);
        }

        if (!wsInfo.isEnableDirectory()) {
            if (!context.getFileMeta().getDirId().equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "directory feature is disable, can not specified directory id for create file");
            }
            return PipelineResult.success();
        }

        try {
            MetaRelAccessor dirAccessor = contentModule.getMetaService().getMetaSource()
                    .getRelAccessor(wsInfo.getName(), context.getTransactionContext());
            BSONObject dirRel = ScmMetaSourceHelper
                    .createRelInsertorByFileInsertor(fileMeta.toRecordBSON());
            dirAccessor.insert(dirRel);
        }
        catch (ScmMetasourceException e) {
            if (e.getScmError() == ScmError.FILE_EXIST) {
                logger.debug("dir relation already exist: dirId={}, file={}", fileMeta.getDirId(),
                        fileMeta.getName(), e);
                String fileId = findFile(wsInfo.getName(), fileMeta.getDirId(), fileMeta.getName());
                if (fileId == null) {
                    // 写入关系表时索引冲突，查找冲突文件时又未找到，通知上层 sleep 1s 后重新触发一次 Pipeline，同时把异常带出去，用于停止重试时提示用户
                    return PipelineResult.redo(
                            new ScmServerException(ScmError.FILE_EXIST, "file already exist: dirId="
                                    + fileMeta.getDirId() + ", fileName=" + fileMeta.getName(), e), 1000);
                }
                throw new FileMetaExistException(
                        "failed to create file, create dir relation failed: dirId="
                                + fileMeta.getDirId() + ", fileName=" + fileMeta.getName(),
                        e, fileId);
            }
            throw new ScmServerException(e.getScmError(),
                    "failed to create file, create dir relation failed: dirId="
                            + fileMeta.getDirId() + ", fileName=" + fileMeta.getName(),
                    e);
        }
        return PipelineResult.success();
    }

    private String findFile(String ws, String dirId, String name) throws ScmServerException {
        try {
            MetaRelAccessor rel = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getRelAccessor(ws, null);
            BasicBSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLREL_FILENAME, name);
            matcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, dirId);
            BSONObject relRecord = rel.queryOne(matcher, null, null);
            if (relRecord == null) {
                return null;
            }
            return BsonUtils.getStringChecked(relRecord, FieldName.FIELD_CLREL_FILEID);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to query dir relation: dirId=" + dirId + ", fileName=" + name, e);
        }
    }
}
