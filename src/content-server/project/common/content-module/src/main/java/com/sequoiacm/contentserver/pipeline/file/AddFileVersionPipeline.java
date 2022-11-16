package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class AddFileVersionPipeline extends Pipeline<AddFileVersionContext> {
    @Override
    void preInvokeFilter(AddFileVersionContext context) throws ScmServerException {
        super.preInvokeFilter(context);
        ScmContentModule contentModule = ScmContentModule.getInstance();

        if (context.getCurrentLatestVersion() == null) {
            try {
                BSONObject matcher = new BasicBSONObject();
                SequoiadbHelper.addFileIdAndCreateMonth(matcher, context.getFileId());
                BSONObject record = contentModule.getMetaService().getMetaSource()
                        .getFileAccessor(contentModule.getWorkspaceInfoCheckExist(context.getWs())
                                .getMetaLocation(), context.getWs(), null)
                        .queryOne(matcher, null, null);
                if (record == null) {
                    throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                            "failed to add version, file not exist: ws=" + context.getWs()
                                    + ", fileId=" + context.getFileId());
                }
                context.setCurrentLatestVersion(FileMeta.fromRecord(record));
            }
            catch (ScmMetasourceException e) {
                throw new ScmServerException(e.getScmError(),
                        "failed to add file version, query file failed: ws" + context.getWs()
                                + ", fileId=" + context.getFileId(),
                        e);
            }
        }

        // 重置新增版本的全局属性（如bucketID、dirId），生成版本号
        context.getNewVersion().newVersionFrom(context.getCurrentLatestVersion());
    }
}
