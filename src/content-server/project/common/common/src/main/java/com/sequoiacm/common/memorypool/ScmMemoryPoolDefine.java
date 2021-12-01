package com.sequoiacm.common.memorypool;

public class ScmMemoryPoolDefine {
    // clean task delayed start, unit: seconds(s)
    public static final int CLEAN_TASK_DELAY = 60;
    public static final float CLEAN_FACTOR = 0.1f;
    // if the time distances from the time last assigning jvm more than this
    // interval, clean task will clean byte array, unit: minute(m)
    public static final int CLEAN_TASK_TRIGGER_INTERVAL = 10;
    public static final int LOWEST_CLEAN_NUM = 10;
    // print task interval, unit: seconds(s)
    public static final int PRINT_TASK_INTERVAL = 60;

    public static final String PROPERTY_MEMORYPOOL_ENABLE = "scm.memoryPool.enable";
    // the interval of memoryPool to clean, unit: seconds(s)
    public static final String PROPERTY_MEMORYPOOL_CLEANINTERVAL = "scm.memoryPool.cleanInterval";
    public static final String PROPERTY_MEMORYPOOL_MAXIDLESIZE = "scm.memoryPool.maxIdleSize";
    public static final String PROPERTY_MEMORYPOOL_MAXPOOLSIZE = "scm.memoryPool.maxPoolSize";

    public static final String DEFAULT_ENABLE = "true";
    // the default interval of memoryPool to clean, unit: seconds(s)
    public static final int DEFAULT_CLEANINTERVAL = 60;
    public static final int DEFAULT_MAXIDLESIZE = 10;
    public static final int DEFAULT_MAXPOOLSIZE = 50;
}
