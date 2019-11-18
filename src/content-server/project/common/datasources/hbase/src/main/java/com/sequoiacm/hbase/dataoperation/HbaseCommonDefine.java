package com.sequoiacm.hbase.dataoperation;

public class HbaseCommonDefine {
    public final static String HBASE_DATA_FAMILY = "SCM_FILE_DATA";
    public final static String HBASE_META_FAMILY = "SCM_FILE_META";

    public final static String HBASE_FILE_STATUS_QUALIFIER = "FILE_STATUS";
    public final static String HBASE_FILE_SIZE_QUALIFIER = "FILE_SIZE";
    public final static String HBASE_FILE_PIECE_NUM_PREFIX = "PIECE_NUM_";
    public final static int HBASE_FILE_PIECE_SIZE = 1024 * 1024;

    public static class HbaseFileStatus {
        public static final String HBASE_FILE_STATUS_UNAVAILABLE = "Unavailable";
        public static final String HBASE_FILE_STATUS_AVAILABLE = "Available";
    }

}
