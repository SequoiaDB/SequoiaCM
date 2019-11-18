package com.sequoiacm.schedule.common;

public class ScheduleDefine {
    public static class ScheduleType {
        public static final String COPY_FILE = "copy_file";
        public static final String CLEAN_FILE = "clean_file";
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
    }

    public static class Lock {
        public static final String TASK = "task";
    }

    public static class ScopeType {
        public static final int CURRENT = 1;
        public static final int HISTORY = 2;
        public static final int ALL = 3;
    }
}
