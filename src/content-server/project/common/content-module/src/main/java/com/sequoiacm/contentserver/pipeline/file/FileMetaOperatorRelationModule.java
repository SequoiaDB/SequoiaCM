package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.contentserver.pipeline.file.batch.DeleteFileBatchFilter;
import com.sequoiacm.contentserver.pipeline.file.batch.DeleteFileVersionBatchFilter;
import com.sequoiacm.contentserver.pipeline.file.batch.OverwriteFileBatchFilter;
import com.sequoiacm.contentserver.pipeline.file.batch.UpdateFileMetaBatchFilter;
import com.sequoiacm.contentserver.pipeline.file.bucket.AddFileVersionBucketFilter;
import com.sequoiacm.contentserver.pipeline.file.bucket.CreateFileBucketFilter;
import com.sequoiacm.contentserver.pipeline.file.bucket.DeleteFileBucketFilter;
import com.sequoiacm.contentserver.pipeline.file.bucket.DeleteFileVersionBucketFilter;
import com.sequoiacm.contentserver.pipeline.file.bucket.OverwriteFileBucketFilter;
import com.sequoiacm.contentserver.pipeline.file.bucket.UpdateFileMetaBucketFilter;
import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.pipeline.file.dir.AddFileVersionDirFilter;
import com.sequoiacm.contentserver.pipeline.file.dir.CreateFileDirFilter;
import com.sequoiacm.contentserver.pipeline.file.dir.DeleteFileDirFilter;
import com.sequoiacm.contentserver.pipeline.file.dir.DeleteFileVersionDirFilter;
import com.sequoiacm.contentserver.pipeline.file.dir.OverwriteFileDirFilter;
import com.sequoiacm.contentserver.pipeline.file.dir.UpdateFileMetaDirFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public interface FileMetaOperatorRelationModule extends FileMetaOperatorModule {
}

