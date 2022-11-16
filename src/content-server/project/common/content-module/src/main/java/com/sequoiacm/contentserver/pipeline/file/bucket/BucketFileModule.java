package com.sequoiacm.contentserver.pipeline.file.bucket;

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
public class BucketFileModule implements FileMetaOperatorRelationModule {
    @Autowired
    private OverwriteFileBucketFilter overwriteFileBucketFilter;
    @Autowired
    private AddFileVersionBucketFilter addFileVersionBucketFilter;
    @Autowired
    private DeleteFileVersionBucketFilter deleteFileVersionBucketFilter;

    @Autowired
    private UpdateFileMetaBucketFilter updateFileMetaBucketFilter;
    @Autowired
    private DeleteFileBucketFilter deleteFileBucketFilter;

    @Autowired
    private CreateFileBucketFilter createFileBucketFilter;

    @Override
    public Filter<CreateFileContext> createFileFilter() {
        return createFileBucketFilter;
    }

    @Override
    public Filter<OverwriteFileContext> overwriteFileFilter() {
        return overwriteFileBucketFilter;
    }

    @Override
    public Filter<AddFileVersionContext> addFileVersionFilter() {
        return addFileVersionBucketFilter;
    }

    @Override
    public Filter<DeleteFileContext> deleteFileFilter() {
        return deleteFileBucketFilter;
    }

    @Override
    public Filter<DeleteFileVersionContext> deleteFileVersionFilter() {
        return deleteFileVersionBucketFilter;
    }

    @Override
    public Filter<UpdateFileMetaContext> updateFileMetaFilter() {
        return updateFileMetaBucketFilter;
    }

    @Override
    public int priority() {
        return 0;
    }
}
