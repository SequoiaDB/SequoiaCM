package com.sequoiacm.schedule.common;

public class ScheduleDefine {
    public static class ScheduleType {
        public static final String COPY_FILE = "copy_file";
        public static final String CLEAN_FILE = "clean_file";
        public static final String MOVE_FILE = "move_file";
        public static final String RECYCLE_SPACE = "recycle_space";
        public static final String INTERNAL_SCHEDULE = "internal_schedule";
    }

    public static String APPLICATION_PROPERTIES_LOCATION = "spring.config.location";
    public static String LOGGING_CONFIG = "logging.config";

    public static String SCHEDULE_ELETOR_PATH = "/scm/vote/schedule/leader";

    public static class TaskRunningFlag {
        public static final int SCM_TASK_INIT = 1;
        public static final int SCM_TASK_RUNNING = 2;
    }

    public static class TaskType {
        public static final int SCM_TASK_COPY_FILE = 1;
        public static final int SCM_TASK_CLEAN_FILE = 2;
        public static final int SCM_TASK_MOVE_FILE = 3;
        public static final int SCM_TASK_RECYCLE_SAPCE = 4;
    }

    public static class Lock {
        public static final String TASK = "task";
    }

    public static class ScopeType {
        public static final int CURRENT = 1;
        public static final int HISTORY = 2;
        public static final int ALL = 3;
    }

    public static class DataCheckLevel {
        public static final String STRICT = "strict";
        public static final String WEEK = "week";
    }
}
