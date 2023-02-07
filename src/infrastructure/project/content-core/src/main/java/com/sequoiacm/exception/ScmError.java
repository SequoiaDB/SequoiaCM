package com.sequoiacm.exception;

/**
 * SCM Error
 */
public enum ScmError {

    UNKNOWN_ERROR(-1, "Unknown error"),

    // common error 1xx
    INVALID_ARGUMENT(-101, "Invalid argument"),
    INVALID_ID(-102, "Invalid id"),
    SYSTEM_ERROR(-103, "System error"),
    LOCK_ERROR(-104, "Lock error"),
    MISSING_ARGUMENT(-105, "Missing argument"),
    OUT_OF_BOUND(-106, "Out of bound"),
    OPERATION_UNSUPPORTED(-107, "Unsupported operation"),
    OPERATION_FORBIDDEN(-108, "Operation forbidden"),
    OPERATION_UNAUTHORIZED(-109, "Unauthorized operation"),
    OPERATION_TIMEOUT(-110, "Operation timeout"),
    RESOURCE_CONFLICT(-111, "Resource conflict"),

    ATTRIBUTE_FORMAT_ERROR(-130, "Attribute format error"),
    NO_OPERAND_FOR_KEY(-131, "No operand for key"),
    SESSION_CLOSED(-132, "Session has been Closed"),
    NETWORK_IO(-133, "Network IO Exception"),
    COMMIT_UNCERTAIN_STATE(-134, "Transaction Commit Uncertain State"),

    // v2 local login get salt error
    SALT_NOT_EXIST(-135, "Salt is not exist"),
    FIND_SALT_FAILED(-136, "Cannot get salt"),

    // business related error 2xx
    SERVER_RELOAD_CONF_FAILED(-201, "Reload configure failed"),
    SERVER_NOT_IN_WORKSPACE(-202, "Server is Not In WorkSpace"),
    SERVER_NOT_EXIST(-203, "Server is not exist"),
    SITE_NOT_EXIST(-204, "Site is not exist"),
    WORKSPACE_NOT_EXIST(-205, "Workspace is not exist"),
    METASOURCE_ERROR(-206, "Metasource Error"),
    WORKSPACE_NOT_EMPTY(-207, "Workspace not empty"),
    WORKSPACE_EXIST(-208, "Workspace already exist"),

    METASOURCE_RECORD_EXIST(-209, "record exist"),

    METASOURCE_TABLE_NOT_EXIST(-210, "table not exist"),

    TASK_DUPLICATE(-220, "Task is duplicate"),
    TASK_NOT_EXIST(-221, "Task is not exist"),
    EXCEED_MAX_CONCURRENT_TASK(-222, "Exceed max concurrent task"),

    DIR_EXIST(-240, "Directory already exists"),
    DIR_NOT_FOUND(-241, "Directory not found"),
    DIR_NOT_EMPTY(-242, "Directory not empty"),
    DIR_MOVE_TO_SUBDIR(-243, "Can not move dir to a subdir of itself"),
    DIR_FEATURE_DISABLE(-244, "Directory is disable"),

    BATCH_NOT_FOUND(-250, "Batch not found"),
    BATCH_FILE_SAME_NAME(-251, "The batch already attach a file with same name"),
    BATCH_EXIST(-252, "Batch already exists"),

    FILE_EXIST(-261, "File already exists"),
    FILE_NOT_FOUND(-262, "File not found"),
    FILE_TABLE_NOT_FOUND(-263, "File table Not Found"),
    FILE_VERSION_MISMATCHING(-264, "Client file version is not current version"),
    FILE_IN_SPECIFIED_BATCH(-265, "File already exists in the specified batch"),
    FILE_IN_ANOTHER_BATCH(-266, "File already exists for another batch"),
    FILE_NOT_IN_BATCH(-267, "File does not exist in the batch"),
    FILE_CLASS_UNDEFINED(-268, "File class is Undefined"),
    BATCH_CLASS_UNDEFINED(-269, "Batch class is Undefined"),
    FILE_IN_ANOTHER_BUCKET(-270, "File already exists for another bucket"),

    PRIVILEGE_GRANT_FAILED(-280, "Grant privilege failed"),
    PRIVILEGE_REVOKE_FAILED(-281, "Revoke privilege failed"),


    // error 3xx
    // unused

    // datasource relate error 4xx
    DATA_ERROR(-401, "Data source error"),
    DATA_NOT_EXIST(-402, "Data is not exist"),
    DATA_TYPE_ERROR(-403, "Data type error"),
    DATA_WRITE_ERROR(-404, "Write data error"),
    DATA_READ_ERROR(-405, "Read data error"),
    DATA_DELETE_ERROR(-406, "Delete data error"),
    DATA_UNAVAILABLE(-407, "Data unavailable"),
    DATA_EXIST(-408, "Data exist"),
    DATA_CORRUPTED(-409, "Data is corrupted"),
    DATA_BREAKPOINT_WRITE_ERROR(-410, "Write breakpoint file data error"),
    DATA_IS_IN_USE(-411, "Data is in use"),
    STORE_SPACE_IS_NOT_EXIST(-412, "Store space is not exist"),
    DATA_PIECES_INFO_OVERFLOW(-413, "Data piece info overflow"),
    DATA_STORAGE_QUOTA_EXCEEDED(-414, "data storage quota exceeded"),

    // metadata error 5xx
    METADATA_CHECK_ERROR(-501, "Properties check error"),
    METADATA_CLASS_EXIST(-502, "Class already exists"),
    METADATA_CLASS_NOT_EXIST(-503, "Class is not exist"),
    METADATA_ATTR_EXIST(-504, "Attr already exists"),
    METADATA_ATTR_NOT_EXIST(-505, "Attr Is not exist"),
    METADATA_ATTR_NOT_IN_CLASS(-506, "Class is not attached with this attr"),
    METADATA_ATTR_ALREADY_IN_CLASS(-507, "Class is already attached with this attr"),
    METADATA_ATTR_DELETE_FAILED(-508, "Attr are attached with certain classes"),

    // client related error 6xx
    FILE_IO(-601, "File IO Exception"),
    FILE_NOT_EXIST(-602, "File is not exist"), // use for local file not found
    FILE_IS_DIRECTORY(-603, "File Is Directory"),
    FILE_ALREADY_EXISTS(-604, "File already exists"),
    FILE_CREATE_FAILED(-605, "File Create Failed"),
    FILE_DELETE_FAILED(-606, "File Delete Failed"),
    FILE_PERMISSION(-607, "File Permission Error"),
    FILE_IS_CLOSED(-608, "File Is Closed"),
    INPUT_STREAM_CLOSED(-609, "InputStream Is Closed"),
    OUTPUT_STREAM_CLOSED(-610, "OutputStream Is Closed"),
    FILE_INVALID_CUSTOMTAG(-611, "File customTag invalid"),
    FILE_CUSTOMTAG_TOO_LARGE(-612, "File customTag too large"),

    CONFIG_SERVER_ERROR(-700, "Config server error"),
    WORKSPACE_CACHE_EXPIRE(-701, "Workspace cache is expire"),

    // -800 already use in hengfeng branch

    BUCKET_EXISTS(-850, "Bucket already exists"),
    BUCKET_NOT_EXISTS(-851, "Bucket is not exist"),
    BUCKET_NOT_EMPTY(-852, "Bucket is not empty"),
    BUCKET_INVALID_CUSTOMTAG(-853, "Bucket customTag is invalid"),
    BUCKET_CUSTOMTAG_TOO_LARGE(-854, "Bucket customTag too large"),
    BUCKET_CUSTOMTAG_NOT_EXIST(-855, "Bucket customTag not exist"),

    FULL_TEXT_INDEX_ALREADY_CREATED(-900, "full text index already created"),
    FULL_TEXT_INDEX_IS_DELETING(-901, "full text index is deleting"),
    FULL_TEXT_INDEX_IS_CREATING(-902, "full text index is creating"),
    FILE_NOT_MEET_WORKSPACE_INDEX_MATCHER(-903, "scm file not meet the matcher of workspace fulltext"),
    FULL_TEXT_INDEX_DISABLE(-904, "full text index is disable"),
    FULL_TEXT_INDEX_CREATE_ERROR(-905, "failed to create full text index"),

    S3_REGION_NOT_EXIST(-950, "region not exist"),

    // http related error
    HTTP_BAD_REQUEST(400, "Bad Request"),
    HTTP_UNAUTHORIZED(401, "Unauthorized"),
    HTTP_PAYMENT_REQUIRED(402, "Payment Required"),
    HTTP_FORBIDDEN(403, "Forbidden"),
    HTTP_NOT_FOUND(404, "Not Found"),
    HTTP_METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    HTTP_NOT_ACCEPTABLE(406, "Not Acceptable"),
    HTTP_INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    HTTP_SERVICE_UNAVAILABLE(503, "Service Unavailable");

    private int errorCode;
    private String desc;

    ScmError(int errorCode, String desc) {
        this.errorCode = errorCode;
        this.desc = desc;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return desc;
    }

    public String getErrorType() {
        return name();
    }

    @Override
    public String toString() {
        return name() + "(" + this.errorCode + ")" + ":" + this.desc;
    }

    public static ScmError getScmError(int errorCode) {
        for (ScmError value : ScmError.values()) {
            if (value.getErrorCode() == errorCode) {
                return value;
            }
        }

        return UNKNOWN_ERROR;
    }
}