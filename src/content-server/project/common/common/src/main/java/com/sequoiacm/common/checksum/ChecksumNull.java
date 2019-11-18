package com.sequoiacm.common.checksum;

import java.util.zip.Checksum;

public class ChecksumNull implements Checksum {

    public ChecksumNull() {
    }

    @Override
    public long getValue() {
        return 0L;
    }

    @Override
    public void reset() {
    }

    @Override
    public void update(byte[] b, int off, int len) {
    }

    @Override
    public void update(int b) {
    }
}
