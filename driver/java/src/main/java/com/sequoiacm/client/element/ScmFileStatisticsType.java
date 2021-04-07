package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;

/**
 * File statistics type
 */
public enum ScmFileStatisticsType {
    /**
     * File upload.
     */
    FILE_UPLOAD,

    /**
     * File download.
     */
    FILE_DOWNLOAD;

    public static ScmFileStatisticsType get(String type) throws ScmException {
        if (ScmStatisticsType.FILE_DOWNLOAD.equals(type)) {
            return FILE_DOWNLOAD;
        }
        else if (ScmStatisticsType.FILE_UPLOAD.equals(type)) {
            return FILE_UPLOAD;
        }
        throw new ScmException(ScmError.SYSTEM_ERROR, "unknown file statistics type:" + type);
    }
}
