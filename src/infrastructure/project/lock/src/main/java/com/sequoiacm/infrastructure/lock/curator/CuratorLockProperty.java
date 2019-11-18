package com.sequoiacm.infrastructure.lock.curator;

class CuratorLockProperty {
    // Curator Lock properties
    public static final long LOCK_WAITTIME = 500;
    public static final long TRYLOCK_WAITTIME = 100;
    public static final long SMALLSLEEPTIME = 1000;
    public static final long BIGSLEEPTIME = 10000;

    // Curator client retryPolicy properties
    public static final int MAXRETRIES = Integer.MAX_VALUE;
    public static final int BASESLEEPTIMEMS = 1000;
    
    public static final int SLEEPMSBETWEENRETRIES = 1000;
    
    public static final int SESSIONTIMEOUTMS = 60000;
    public static final int CONNECTIONTIMEOUTMS = 10000;
}
