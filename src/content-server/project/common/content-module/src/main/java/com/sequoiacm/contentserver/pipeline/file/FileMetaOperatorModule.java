package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;

public interface FileMetaOperatorModule {
    Filter<CreateFileContext> createFileFilter();

    Filter<OverwriteFileContext> overwriteFileFilter();

    Filter<AddFileVersionContext> addFileVersionFilter();

    Filter<DeleteFileContext> deleteFileFilter();

    Filter<DeleteFileVersionContext> deleteFileVersionFilter();

    Filter<UpdateFileMetaContext> updateFileMetaFilter();

    int priority();
}
