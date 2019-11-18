package com.sequoiacm.cephswift;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;

public class CephSwiftException extends ScmDatasourceException {

    /**
     *
     */
    public static final int STATUS_CODE_NOTFOUND = 404;
    public static final String ERR_ENTITY_ALREADY_EXISTS = "ENTITY_ALREADY_EXISTS";
    public static final String ERR_ENTITY_DOES_NOT_EXIST = "ENTITY_DOES_NOT_EXIST";
    public static final String ERR_ENTITY_IS_CORRUPTED = "ENTITY_IS_CORRUPTED";
    public static final String ERR_OPERATION_UNSUPPORTED = "OPERATION_UNSUPPORTED";

    private static final long serialVersionUID = -7953012356041286287L;
    private int httpStatusCode = 0;
    private String errorCode = "";

    public CephSwiftException(int status, String errorCode, String message) {
        super(message);
        this.httpStatusCode = status;
        this.errorCode = errorCode;
        setScmError();
    }

    public CephSwiftException(String message) {
        super(message);
    }

    public CephSwiftException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        setScmError();
    }

    public CephSwiftException(int status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = status;
        this.errorCode = errorCode;
        setScmError();
    }

    public CephSwiftException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        if (httpStatusCode == 0 && errorCode.equals("")) {
            return super.toString();
        }
        return super.toString() + ", ceph swift erorrStatus=" + httpStatusCode + ",errorCode=" + errorCode;
    }

    public int getSwiftStatusCode() {
        return httpStatusCode;
    }

    public String getSwiftErrorCode() {
        return errorCode;
    }

    private void setScmError() {
        if (STATUS_CODE_NOTFOUND == httpStatusCode
                && ERR_ENTITY_DOES_NOT_EXIST.equals(errorCode)) {
            scmError = ScmError.DATA_NOT_EXIST;
        } else if (ERR_ENTITY_IS_CORRUPTED.equals(errorCode)) {
            scmError = ScmError.DATA_CORRUPTED;
        } else if (ERR_ENTITY_ALREADY_EXISTS.equals(errorCode)) {
            scmError = ScmError.DATA_EXIST;
        } else if (ERR_OPERATION_UNSUPPORTED.equals(errorCode)) {
            scmError = ScmError.OPERATION_UNSUPPORTED;
        }
    }
}
