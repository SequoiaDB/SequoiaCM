package com.sequoiacm.common.checksum;

import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public final class ChecksumFactory {
    private ChecksumFactory() {
    }

    public static Checksum getChecksum(ChecksumType type, long initValue)
            throws ChecksumException {
        if (type == null) {
            throw new ChecksumException("ChecksumType is null");
        }

        switch (type) {
            case NONE:
                return new ChecksumNull();
            case CRC32:
                return new ScmCRC32(initValue);
            case ADLER32:
                return new ScmAdler32(initValue);
            default:
                throw new ChecksumException("Invalid checksum type: " + type.name());
        }
    }

    public static Checksum getChecksum(ChecksumType type) throws ChecksumException {
        if (type == null) {
            throw new ChecksumException("ChecksumType is null");
        }

        switch (type) {
            case NONE:
                return new ChecksumNull();
            case CRC32:
                return new CRC32();
            case ADLER32:
                return new Adler32();
            default:
                throw new ChecksumException("Invalid checksum type: " + type.name());
        }
    }
}
