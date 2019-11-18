package com.sequoiacm.common.checksum;

import java.lang.reflect.Field;
import java.util.zip.CRC32;

public class ScmCRC32 extends CRC32 {

    public ScmCRC32() {
    }

    public ScmCRC32(long initValue) throws ChecksumException {
        Field crcField;
        try {
            crcField = CRC32.class.getDeclaredField("crc");
            crcField.setAccessible(true);
            crcField.setInt(this, (int) initValue);
            crcField.setAccessible(false);
        } catch (NoSuchFieldException e) {
            throw new ChecksumException("No 'crc' field", e);
        } catch (IllegalAccessException e) {
            throw new ChecksumException("Failed to set 'crc' field", e);
        }
    }
}
