package com.sequoiacm.cephs3;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;

public class CephS3Exception extends ScmDatasourceException {
    /**
     *
     */
    public static final int STATUS_NOT_FOUND = 404;
    public static final String ERR_CODE_NO_SUCH_BUCKET = "NoSuchBucket";
    public static final String ERR_CODE_NO_SUCH_KEY = "NoSuchKey";
    public static final String ERR_CODE_OBJECT_EXIST = "ObjectExist";
    public static final String ERR_CODE_OPERATION_UNSUPPORTED = "OperationUnsupported";
    public static final String ERR_CODE_BUCKET_EXIST = "BucketAlreadyOwnedByYou";
    public static final String ERR_CODE_NO_SUCH_UPLOAD = "NoSuchUpload";

    private static final long serialVersionUID = -2886008261335449132L;

    private int status = 0;
    private String errorCode = "";

    public CephS3Exception(int status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        setScmError();
    }

    public CephS3Exception(String message) {
        super(message);
    }

    public CephS3Exception(int status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        setScmError();
    }

    public CephS3Exception(int status, String errorCode, String message, ScmError scmError,
            Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.scmError = scmError;
    }

    public CephS3Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public CephS3Exception(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        setScmError();
    }

    @Override
    public String toString() {
        if (status == 0 && errorCode.equals("")) {
            return super.toString();
        }
        return super.toString() + ", ceph s3 errorStatus=" + status + ",errorCode=" + errorCode;

    }

    public int getS3StatusCode() {
        return status;
    }

    public String getS3ErrorCode() {
        return errorCode;
    }

    private void setScmError() {
        if (status == CephS3Exception.STATUS_NOT_FOUND) {
            if (ERR_CODE_NO_SUCH_BUCKET.equals(errorCode) || ERR_CODE_NO_SUCH_KEY.equals(errorCode)
                    || ERR_CODE_NO_SUCH_UPLOAD.equals(errorCode)) {
                scmError = ScmError.DATA_NOT_EXIST;
            }
        }
        else if (ERR_CODE_OBJECT_EXIST.equals(errorCode)) {
            scmError = ScmError.DATA_EXIST;
        }
        else if (ERR_CODE_OPERATION_UNSUPPORTED.equals(errorCode)) {
            scmError = ScmError.OPERATION_UNSUPPORTED;
        }
    }
}
