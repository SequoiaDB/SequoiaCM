package com.sequoiacm.infrastructure.statistics.common;

public class ScmStatisticsDefine {

    // 网关条件这个请求头提示下游服务这次操作需要被统计
    public static final String STATISTICS_HEADER = "x-scm-statistics";

    // 下游服务通过这个响应头，告知网关额外的统计信息
    public static final String STATISTICS_EXTRA_HEADER = "x-statistics-extra";


    public static final String REST_PARAM_CONDITION = "condition";
    public static final String REST_FIELD_AVG_RESP_TIME = "avg_response_time";
    public static final String REST_FIELD_MAX_RESP_TIME = "max_response_time";
    public static final String REST_FIELD_MIN_RESP_TIME = "min_response_time";
    public static final String REST_FIELD_AVG_TRAFFIC_SIZE = "avg_traffic_size";
    public static final String REST_FIELD_REQ_COUNT = "request_count";
    public static final String REST_FIELD_FAIL_COUNT = "fail_count";
    public static final String REST_FIELD_USER = "user";
    public static final String REST_FIELD_WORKSPACE = "workspace";
    public static final String REST_FIELD_BEGIN = "begin";
    public static final String REST_FIELD_END = "end";
    public static final String REST_FIELD_TIME_ACCURACY = "time_accuracy";

    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
}
