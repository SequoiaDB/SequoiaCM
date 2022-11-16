package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class FileMetaExistException extends ScmServerException {
    private final String fileId;

    public FileMetaExistException(String message, Throwable cause, String existFileId) {
        super(ScmError.FILE_EXIST, message, cause);
        this.fileId = existFileId;
    }

    public FileMetaExistException(String message, String existFileId) {
        super(ScmError.FILE_EXIST, message);
        this.fileId = existFileId;
    }

    public String getExistFileId() {
        return fileId;
    }
}
