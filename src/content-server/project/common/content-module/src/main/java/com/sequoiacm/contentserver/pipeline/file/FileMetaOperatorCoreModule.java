package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.pipeline.file.core.AddFileVersionCoreFilter;
import com.sequoiacm.contentserver.pipeline.file.core.CreateFileCoreFilter;
import com.sequoiacm.contentserver.pipeline.file.core.DeleteFileCoreFilter;
import com.sequoiacm.contentserver.pipeline.file.core.DeleteFileVersionCoreFilter;
import com.sequoiacm.contentserver.pipeline.file.core.OverwriteFileCoreFilter;
import com.sequoiacm.contentserver.pipeline.file.core.UpdateFileMetaCoreFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public interface FileMetaOperatorCoreModule extends FileMetaOperatorModule {
}

