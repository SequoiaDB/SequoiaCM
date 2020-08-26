package com.sequoiacm.infrastructure.fulltext.common;

public class FultextRestCommonDefine {
    public static final String REST_ACTION = "action";
    public static final String REST_ACTION_ENABLE = "enable";
    public static final String REST_FILEMATCHER = "file_matcher";
    public static final String REST_FILE_ID = "file_id";
    public static final String REST_FILE_MAJORVERSION = "file_majorversion";
    public static final String REST_FILE_MINORVERSION = "file_minorversion";
    public static final String REST_WORKSPACE = "workspace";
    public static final String REST_SCOPE = "scope";
    public static final String REST_INDEX_MONDE = "index_mode";
    public static final String REST_INDEX_DATA_LOCATION = "index_data_location";
    public static final String REST_FILES = "files";
    public static final String REST_CONTENT_CONDITION = "content_condition";
    public static final String REST_FILE_CONDITION = "file_condition";
    public static final String REST_FILE_IDX_STATUS = "status";

    public static class FulltextSearchRes {
        public static final String KEY_FILE_BASIC_INFO = "fileBasicInfo";
        public static final String KEY_SCORE = "score";
        public static final String KEY_HIGHLIGHT = "highlight";
    }
}
