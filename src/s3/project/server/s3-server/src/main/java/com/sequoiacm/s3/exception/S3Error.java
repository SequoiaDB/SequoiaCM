package com.sequoiacm.s3.exception;

public enum S3Error {
    // custom error code -- internal error
    INTERNAL_ERROR(-301, "InternalError", "We encountered an internal error. Please try again."),
    OPERATION_NOT_SUPPORTED(-302, "OperationNotSupport.", "Operation not supported."),
    SYSTEM_ERROR(-303, "SystemError.", "System error."),
    METASOUCE_ERROR(-304, "MetaSourceError", "MetaSource Error."),
    INVALID_ARGUMENT(-305, "InvalidArgument", "Invalid argument."),

    // custom error code -- region error
    REGION_NO_SUCH_REGION(-321, "NoSuchRegion", "No such region."),
    REGION_PUT_FAILED(-322, "PutRegionFailed", "Put region failed."),
    REGION_GET_FAILED(-323, "GetRegionFailed", "Get region config failed."),

    // custom error code -- scm error
    SCM_CONTENSERER_NO_INSTANCE(-340, "NoContenServerInstance", "ContentServer instance not found."),
    SCM_CREATE_BREAKPOINT_FILE_FAILED(-341, "CreateBreakpointFileFailed", "Create scm breakpoint file failed."),
    SCM_DOWNLOAD_FILE_FAILED(-342, "DownloadFileFailed", "Download scm file failed."),
    SCM_GET_META_FAILED(-343, "GetMetaFailed", "Get scm meta failed."),
    SCM_AUTH_FAILED(-344, "AuthFailed", "Failed to do auth in authserver"),
    SCM_GET_WS_FAILED(-345, "GetWorkspaceFailed", "Get scm workspace failed."),

    // custom error code -- bucket error
    BUCKET_CREATE_FAILED(-360, "CreateBucketFailed", "Create bucket failed."),
    BUCKET_DELETE_FAILED(-361, "DeleteBucketFailed", "Delete bucket failed."),
    BUCKET_GET_SERVICE_FAILED(-362, "GetServiceFailed", "Get service failed."),
    BUCKET_VERSIONING_SET_FAILED(-363, "PutBucketVersioningFailed", "Put bucket versioning failed."),
    BUCKET_VERSIONING_GET_FAILED(-364, "GetBucketVersioningFailed", "Get bucket versioning failed."),
    BUCKET_LOCATION_GET_FAILED(-365, "GetBucketLocationFailed", "Get bucket location failed."),
    BUCKET_GET_FAILED(-366, "GetBucketFailed", "Get bucket failed."),
    BUCKET_TAGGING_PUT_FAILED(-367, "PutBucketTaggingFailed", "Put bucket tagging failed."),
    BUCKET_TAGGING_GET_FAILED(-368, "GetBucketTaggingFailed", "Get bucket tagging failed."),
    BUCKET_TAGGING_DELETE_FAILED(-369, "DeleteBucketTaggingFailed", "Delete bucket tagging failed."),

    // custom error code -- object error
    OBJECT_PUT_FAILED(-380, "PutObjectFailed", "Put object failed."),
    OBJECT_GET_FAILED(-381, "GetObjectFailed", "Get object failed"),
    OBJECT_DELETE_FAILED(-382, "DeleteObjectFailed", "Delete object failed."),
    OBJECT_LIST_FAILED(-383, "ListObjectsFailed", "List objects failed."),
    OBJECT_LIST_VERSIONS_FAILED(-384, "ListVersionsFailed", "List versions failed."),
    OBJECT_LIST_V1_FAILED(-385, "ListObjectsV1Failed", "List objects V1 failed."),
    OBJECT_COPY_FAILED(-386, "CopyObjectFailed", "Copy object failed"),

    OBJECT_INVALID_TIME(-400, "InvalidArgument", "Time is invalid"),
    OBJECT_IS_IN_USE(-401, "ObjectIsInUse", "The object is in use."),
    OBJECT_PUT_TAGGING_FIELD(-402, "PutObjectTagFailed", "Put object tagging failed"),
    OBJECT_GET_TAGGING_FIELD(-403, "GetObjectTagFailed", "Get object tagging failed"),
    OBJECT_DELETE_TAGGING_FIELD(-404, "DeleteObjectTagFailed", "Delete object tagging failed"),

    // custom error code -- multipart upload error
    PART_INIT_MULTIPART_UPLOAD_FAILED(-420, "InitMultipartUploadFailed", "Init multipart upload failed."),
    PART_UPLOAD_PART_FAILED(-421, "UploadPartFailed", "Upload part failed."),
    PART_LIST_PARTS_FAILED(-422, "ListPartsFailed", "List parts failed."),
    PART_COMPLETE_MULTIPART_UPLOAD_FAILED(-423, "CompleteMultipartUploadFailed", "Complete multipart upload failed."),
    PART_ABORT_MULTIPART_UPLOAD_FAILED(-424, "AbortMultipartUploadFailed", "Abort multipart upload failed."),
    PART_LIST_MULTIPART_UPLOADS_FAILED(-425, "ListMultipartUploadsFailed", "List multipart uploads failed."),

    PART_UPLOAD_CONFLICT(-440, "UploadConflict", "The uploadId is busy, multipart upload operation is forbidden."),
    PART_DIFF_SITE(-441, "DifferentSite", "The current site is different from the site of init multipart upload."),
    PART_SITE_NOT_SUPPORT(-442, "SiteNotSupport", "Multipart upload is not supported in current site."),

    // custom error code -- user error
    USER_UPDATE_FAILED(-460, "UpdateUserFailed", "Update user failed."),

    // AWS error code -- bucket error
    BUCKET_NOT_EXIST(-500, "NoSuchBucket", "The specified bucket does not exist."),
    BUCKET_INVALID_BUCKETNAME(-501, "InvalidBucketName", "The specified bucket name is invalid."),
    BUCKET_ALREADY_EXIST(-502, "BucketAlreadyExists", "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again."),
    BUCKET_ALREADY_OWNED_BY_YOU(-503, "BucketAlreadyOwnedByYou", "Your previous request to create the named bucket succeeded and you already own it."),
    BUCKET_NOT_EMPTY(-504, "BucketNotEmpty", "The bucket you tried to delete is not empty."),
    BUCKET_TOO_MANY_BUCKETS(-505, "TooManyBuckets", "You have attempted to create more buckets than allowed."),
    BUCKET_INVALID_VERSIONING_STATUS(-506, "InvalidVersioningStatus", "The versioning status is invalid."),
    BUCKET_INVALID_LOCATION(-507, "InvalidLocation", "The location is invalid."),
    BUCKET_TAGGING_TOO_LARGE(-508, "BadRequest", "Bucket tag count cannot be greater than 50"),
    BUCKET_INVALID_TAGGING(-509, "InvalidTag", "The TagKey you have provided is invalid"),
    BUCKET_TAGGING_NOT_EXIST(-510, "NoSuchTagSet", "The TagSet does not exist"),
    BUCKET_QUOTA_EXCEEDED(-511,"QuotaExceeded" ,"The bucket quota exceeded." ),

    // AWS error code -- object error
    OBJECT_INVALID_KEY(-520, "InvalidKey", "Invalid Key."),
    OBJECT_KEY_TOO_LONG(-521, "KeyTooLongError", "Your key is too long."),
    OBJECT_METADATA_TOO_LARGE(-522, "MetadataTooLarge", "Your metadata headers exceed the maximum allowed metadata size."),
    OBJECT_NO_SUCH_KEY(-523, "NoSuchKey", "The specified key does not exist."),
    OBJECT_BAD_DIGEST(-524, "BadDigest", "The Content-MD5 you specified did not match what we received."),
    OBJECT_INVALID_ENCODING_TYPE(-525, "InvalidArgument", "Invalid Encoding Method specified in Request"),
    OBJECT_INVALID_TOKEN(-526, "InvalidArgument", "The continuation token provided is incorrect."),
    OBJECT_RANGE_NOT_SATISFIABLE(-527, "InvalidRange", "Requested range not satisfiable."),
    OBJECT_INVALID_DIGEST(-528, "InvalidDigest", " The Content-MD5 you specified was not valid."),
    OBJECT_NO_SUCH_VERSION(-529, "NoSuchVersion", "The specified version does not exist."),
    OBJECT_INVALID_VERSION(-530, "InvalidArgument", "Invalid version id specified"),
    OBJECT_INVALID_RANGE(-531, "InvalidArgument", "Invalid range."),
    OBJECT_INCOMPLETE_BODY(-532, "IncompleteBody", "You did not provide the number bytes specified by the Content-Length HTTP header."),
    OBJECT_TAGGING_TOO_LARGE(-533, "BadRequest", "Object tags cannot be greater than 10"),
    OBJECT_INVALID_TAGGING(-534, "InvalidTag", "The TagKey you have provided is invalid"),
    OBJECT_TAGGING_SAME_KEY(-535, "InvalidTag", "Cannot provide multiple Tags with the same key"),

    // AWS error code -- get object error
    OBJECT_IF_MODIFIED_SINCE_FAILED(-540, "NotModified", "If-Modified-Since not match"),
    OBJECT_IF_UNMODIFIED_SINCE_FAILED(-541, "PreconditionFailed", "If-Unmodified-Since not match"),
    OBJECT_IF_MATCH_FAILED(-542, "PreconditionFailed", "If-Match not match"),
    OBJECT_IF_NONE_MATCH_FAILED(-543, "NotModified", "If-None-Match not match"),

    // AWS error code -- multipart upload error
    PART_NO_SUCH_UPLOAD(-560, "NoSuchUpload", "The specified multipart upload does not exist. The upload ID might be invalid, or the multipart upload might have been aborted or completed."),
    PART_ENTITY_TOO_SMALL(-561, "EntityTooSmall", "Your proposed upload is smaller than the minimum allowed object size. Each part must be at least 5 MB in size, except the last part."),
    PART_INVALID_PART(-562, "InvalidPart", "One or more of the specified parts could not be found. The part might not have been uploaded, or the specified entity tag might not have matched the part's entity tag."),
    PART_INVALID_PARTORDER(-563, "InvalidPartOrder", "The list of parts was not in ascending order. Parts list must be specified in order by part number."),
    PART_INVALID_PARTNUMBER(-564, "InvalidPartNumber", "Part number must be an integer between 1 and 10000, inclusive."),
    PART_ENTITY_TOO_LARGE(-565, "EntityTooLarge", "Your proposed upload is larger than the maximum allowed object size. Each part must be not more 5 GB in size, except the last part."),
    PART_COPY_RANGE_INVALID(-566, "InvalidArgument", "The x-amz-copy-source-range value must be of the form bytes=first-last where first and last are the zero-based offsets of the first and last bytes to copy."),
    PART_COPY_RANGE_NOT_SATISFIABLE(-567, "InvalidRequest", "The specified copy range is invalid for the source object size."),

    // AWS error code -- copy object error
    OBJECT_COPY_INVALID_DIRECTIVE(-580, "InvalidArgument", "Unknown metadata directive."),
    OBJECT_COPY_WITHOUT_CHANGE(-581, "InvalidRequest", "This copy request is illegal because it is trying to copy an object to itself without changing the object's metadata."),
    OBJECT_COPY_DELETE_MARKER(-582, "InvalidRequest", "The source of a copy request may not specifically refer to a delete marker by version id."),
    OBJECT_COPY_INVALID_SOURCE(-583, "InvalidArgument", "Copy source must mention the source bucket and key: sourcebucket/sourcekey."),
    OBJECT_COPY_INVALID_DEST(-584, "InvalidArgument", "You can only specify a copy source header for copy requests."),

    // AWS error code -- authorization error
    INVALID_ACCESSKEYID(-600, "InvalidAccessKeyId", "The AWS access key ID you provided does not exist in our records."),
    SIGNATURE_NOT_MATCH(-601, "SignatureDoesNotMatch", "Signature does not match."),
    ACCESS_DENIED(-602, "AccessDenied", "Access Denied."),
    NO_CREDENTIALS(-603, "CredentialsNotSupported", "This request does not support credentials."),
    INVALID_AUTHORIZATION(-604, "AuthorizationHeaderMalformed", "The authorization header you provided is invalid."),
    REQUEST_TIME_TOO_SKEWED(-605, "RequestTimeTooSkewed", "The difference between the request time and the server's time is too large."),
    ACCESS_EXPIRED(-606, "AccessDenied", "Request has expired"),
    PRE_URL_V2_NEED_QUERY_PARAMETERS(-607, "AccessDenied", "Query-string authentication requires the Signature, X-Amz-Date, Expires and AWSAccessKeyId parameters"),
    PRE_URL_V4_NEED_QUERY_PARAMETERS(-608, "AuthorizationQueryParametersError", "Query-string authentication version 4 requires the X-Amz-Algorithm, X-Amz-Credential, X-Amz-Signature, X-Amz-Date, X-Amz-SignedHeaders, and X-Amz-Expires parameters."),
    NUMBER_X_AMZ_EXPIRES(-609, "AuthorizationQueryParametersError", "X-Amz-Expires should be a number"),
    X_AMZ_EXPIRES_TOO_LARGE(-610, "AuthorizationQueryParametersError", "X-Amz-Expires must be less than a week (in seconds); that is, the given X-Amz-Expires must be less than 604800 seconds"),
    X_AMZ_EXPIRES_NEGATIVE(-611, "AuthorizationQueryParametersError", "X-Amz-Expires must be non-negative"),
    X_AMZ_X_AMZ_DATE_ERROR(-612, "AuthorizationQueryParametersError", "X-Amz-Date must be in the ISO8601 Long Format yyyyMMdd'T'HHmmss'Z'"),
    ACCESS_NEED_VALID_DATE(-613, "AccessDenied", "AWS authorization requires a valid Date or x-amz-date header"),

    // AWS error code -- common error
    METHOD_NOT_ALLOWED(-621, "MethodNotAllowed", "The specified method is not allowed against this resource."),
    MALFORMED_XML(-622, "MalformedXML", "The XML you provided was not well-formed or did not validate against our published schema."),
    NEED_A_KEY(-623, "InvalidRequest", "A key must be specified."),
    PARAMETER_NOT_SUPPORT(-624, "ParameterNotAllowed", "The specified parameter is not supported.");

    private int errIndex;
    private String code;
    private String message;
    private int httpStatus;

    S3Error(int errIndex, String code, String desc) {
        this(errIndex, code, 500, desc);
    }

    S3Error(int errIndex, String code, int httpStatus, String desc) {
        this.errIndex = errIndex;
        this.code = code;
        this.message = desc;
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getErrIndex() {
        return errIndex;
    }

    public String getErrorMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String getErrorType() {
        return name();
    }

    @Override
    public String toString() {
        return name() + "(" + this.code + ")" + ":" + this.message;
    }

    public static S3Error getS3Error(int errorIndex) {
        for (S3Error value : S3Error.values()) {
            if (value.getErrIndex() == errorIndex) {
                return value;
            }
        }

        return INTERNAL_ERROR;
    }
}