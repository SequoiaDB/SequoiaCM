package com.sequoiacm.common.memorypool;

public interface IMemoryPool {
    byte[] getBytes(int size);

    void releaseBytes(byte[] b);

    void close();
}
