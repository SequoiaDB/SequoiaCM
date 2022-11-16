package com.sequoiacm.contentserver.pipeline.file.dir;

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
public class DirFileModule implements FileMetaOperatorRelationModule {
    @Autowired
    private CreateFileDirFilter createFileDirFilter;
    @Autowired
    private OverwriteFileDirFilter overwriteFileDirFilter;

    @Autowired
    private DeleteFileDirFilter deleteFileDirFilter;

    @Autowired
    private AddFileVersionDirFilter addFileVersionDirFilter;

    @Autowired
    private DeleteFileVersionDirFilter deleteFileVersionDirFilter;

    @Autowired
    private UpdateFileMetaDirFilter updateFileMetaDirFilter;

    @Override
    public Filter<CreateFileContext> createFileFilter() {
        return createFileDirFilter;
    }

    @Override
    public Filter<OverwriteFileContext> overwriteFileFilter() {
        return overwriteFileDirFilter;
    }

    @Override
    public Filter<AddFileVersionContext> addFileVersionFilter() {
        return addFileVersionDirFilter;
    }

    @Override
    public Filter<DeleteFileContext> deleteFileFilter() {
        return deleteFileDirFilter;
    }

    @Override
    public Filter<DeleteFileVersionContext> deleteFileVersionFilter() {
        return deleteFileVersionDirFilter;
    }

    @Override
    public Filter<UpdateFileMetaContext> updateFileMetaFilter() {
        return updateFileMetaDirFilter;
    }

    @Override
    public int priority() {
        return 0;
    }
}
