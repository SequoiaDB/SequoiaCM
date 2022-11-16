package com.sequoiacm.contentserver.pipeline.file.batch;

import com.sequoiacm.contentserver.pipeline.file.FileMetaOperatorRelationModule;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BatchFileModule implements FileMetaOperatorRelationModule {

    @Autowired
    private DeleteFileBatchFilter deleteFileBatchFilter;

    @Autowired
    private DeleteFileVersionBatchFilter deleteFileVersionBatchFilter;

    @Autowired
    private UpdateFileMetaBatchFilter updateFileMetaBatchFilter;

    @Autowired
    private OverwriteFileBatchFilter overwriteFileBatchFilter;
    @Override
    public Filter<CreateFileContext> createFileFilter() {
        return null;
    }

    @Override
    public Filter<OverwriteFileContext> overwriteFileFilter() {
        return overwriteFileBatchFilter;
    }

    @Override
    public Filter<AddFileVersionContext> addFileVersionFilter() {
        return null;
    }

    @Override
    public Filter<DeleteFileContext> deleteFileFilter() {
        return deleteFileBatchFilter;
    }

    @Override
    public Filter<DeleteFileVersionContext> deleteFileVersionFilter() {
        return deleteFileVersionBatchFilter;
    }

    @Override
    public Filter<UpdateFileMetaContext> updateFileMetaFilter() {
        return updateFileMetaBatchFilter;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }
}
