package com.sequoiacm.common.checksum;

import java.lang.reflect.Field;
import java.util.zip.Adler32;

public class ScmAdler32 extends Adler32 {

    public ScmAdler32() {
    }

    public ScmAdler32(long initValue) throws ChecksumException {
        Field adlerField;
        try {
            adlerField = Adler32.class.getDeclaredField("adler");
            adlerField.setAccessible(true);
            adlerField.setInt(this, (int) initValue);
            adlerField.setAccessible(false);
        } catch (NoSuchFieldException e) {
            throw new ChecksumException("No 'adler' field", e);
        } catch (IllegalAccessException e) {
            throw new ChecksumException("Failed to set 'adler' field", e);
        }
    }
}
