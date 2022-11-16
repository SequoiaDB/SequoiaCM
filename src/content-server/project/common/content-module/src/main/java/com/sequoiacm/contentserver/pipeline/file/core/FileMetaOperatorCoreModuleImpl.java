package com.sequoiacm.contentserver.pipeline.file.core;

import com.sequoiacm.contentserver.pipeline.file.FileMetaOperatorCoreModule;
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
public class FileMetaOperatorCoreModuleImpl implements FileMetaOperatorCoreModule {

    @Autowired
    private OverwriteFileCoreFilter overwriteFileCoreFilter;
    @Autowired
    private CreateFileCoreFilter createFileCoreFilter;
    @Autowired
    private AddFileVersionCoreFilter addFileVersionCoreFilter;
    @Autowired
    private DeleteFileVersionCoreFilter deleteFileVersionCoreFilter;
    @Autowired
    private UpdateFileMetaCoreFilter updateFileMetaCoreFilter;
    @Autowired
    private DeleteFileCoreFilter deleteFileCoreFilter;
    @Override
    public Filter<CreateFileContext> createFileFilter() {
        return createFileCoreFilter;
    }

    @Override
    public Filter<OverwriteFileContext> overwriteFileFilter() {
        return overwriteFileCoreFilter;
    }

    @Override
    public Filter<AddFileVersionContext> addFileVersionFilter() {
        return addFileVersionCoreFilter;
    }

    @Override
    public Filter<DeleteFileContext> deleteFileFilter() {
        return deleteFileCoreFilter;
    }

    @Override
    public Filter<DeleteFileVersionContext> deleteFileVersionFilter() {
        return deleteFileVersionCoreFilter;
    }

    @Override
    public Filter<UpdateFileMetaContext> updateFileMetaFilter() {
        return updateFileMetaCoreFilter;
    }

    @Override
    public int priority() {
        return 0;
    }
}
