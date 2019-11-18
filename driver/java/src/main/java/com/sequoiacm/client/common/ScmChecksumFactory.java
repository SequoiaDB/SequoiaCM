package com.sequoiacm.client.common;

import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.common.checksum.ChecksumException;
import com.sequoiacm.common.checksum.ChecksumNull;
import com.sequoiacm.common.checksum.ScmAdler32;
import com.sequoiacm.common.checksum.ScmCRC32;

/**
 * The class of ScmChecksumFactory.
 */
public final class ScmChecksumFactory {
    private ScmChecksumFactory() {
    }

    /**
     * Gets an instance of Checksum by type and init value.
     *
     * @param type
     *            the type of checksum.
     * @param initValue
     *            init value.
     * @return instance of Checksum.
     * @throws ScmException
     *             if error happens.
     */
    public static Checksum getChecksum(ScmChecksumType type, long initValue) throws ScmException {
        if (type == null) {
            throw new ScmInvalidArgumentException("ChecksumType is null");
        }

        switch (type) {
            case NONE:
                return new ChecksumNull();
            case CRC32:
                try {
                    return new ScmCRC32(initValue);
                }
                catch (ChecksumException e) {
                    throw new ScmSystemException("Failed to create CRC32 checksum", e);
                }
            case ADLER32:
                try {
                    return new ScmAdler32(initValue);
                }
                catch (ChecksumException e) {
                    throw new ScmSystemException("Failed to create ADLER32 checksum", e);
                }
            default:
                throw new ScmInvalidArgumentException("Invalid checksum type: " + type.name());
        }
    }

    /**
     * Get an instance of checksum by type.
     *
     * @param type
     *            checksum type,
     * @return instance of Checksum.
     * @throws ScmException
     *             if error happens.
     */
    public static Checksum getChecksum(ScmChecksumType type) throws ScmException {
        if (type == null) {
            throw new ScmInvalidArgumentException("ChecksumType is null");
        }

        switch (type) {
            case NONE:
                return new ChecksumNull();
            case CRC32:
                return new CRC32();
            case ADLER32:
                return new Adler32();
            default:
                throw new ScmInvalidArgumentException("Invalid checksum type: " + type.name());
        }
    }
}
