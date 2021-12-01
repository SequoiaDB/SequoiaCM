package com.sequoiacm.common.memorypool;

import com.sequoiacm.infrastructure.common.ApplicationConfig;

public class ScmPoolWrapper implements IMemoryPool {
    private IMemoryPool pool;
    private static ScmPoolWrapper instance;

    public static ScmPoolWrapper getInstance() throws Exception {
        if (instance == null) {
            synchronized (ScmPoolWrapper.class) {
                if (instance == null) {
                    ScmPoolWrapper tmp = new ScmPoolWrapper();
                    tmp.init();
                    instance = tmp;
                }
            }
        }
        return instance;
    }

    private void init() throws Exception {
        boolean enableMemoryPool = getEnableOptionValue();
        pool = enableMemoryPool ? new ScmMemoryPool() : new ScmNoPool();
    }

    private boolean getEnableOptionValue() throws Exception {
        String enableMemoryPoolStr = ApplicationConfig.getInstance().getConfig(
                ScmMemoryPoolDefine.PROPERTY_MEMORYPOOL_ENABLE, ScmMemoryPoolDefine.DEFAULT_ENABLE);
        return Boolean.parseBoolean(enableMemoryPoolStr);
    }

    @Override
    public byte[] getBytes(int size) {
        return pool.getBytes(size);
    }

    @Override
    public void releaseBytes(byte[] b) {
        pool.releaseBytes(b);
    }

    @Override
    public void close() {
        pool.close();
    }
}
