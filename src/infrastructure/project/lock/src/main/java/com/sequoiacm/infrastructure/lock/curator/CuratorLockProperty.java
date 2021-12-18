package com.sequoiacm.infrastructure.lock.curator;

public class CuratorLockProperty {
    // Curator Lock properties
    public static final long LOCK_WAITTIME = 500;
    public static final long TRYLOCK_WAITTIME = 100;
    public static final long SMALLSLEEPTIME = 1000;
    public static final long BIGSLEEPTIME = 10000;

    // Curator client retryPolicy properties
    public static final int MAXRETRIES = 2;
    public static final int BASESLEEPTIMEMS = 1000;

    public static final int SLEEPMSBETWEENRETRIES = 1000;

    public static final int SESSIONTIMEOUTMS = 60000;
    public static final int CONNECTIONTIMEOUTMS = 6000;

    public static final int ZK_CONNECTION_TIMEOUTMS = 3000;
    public static final String LOCK_PATH_SEPERATOR = "/";
}
