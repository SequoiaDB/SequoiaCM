package com.sequoiacm.datasource.common;

import com.sequoiacm.datasource.exception.ScmReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ScmInputStreamDataReader {

    private static final Logger logger = LoggerFactory.getLogger(ScmInputStreamDataReader.class);

    private final InputStream inputStream;

    private boolean isEof = false;
    private long currentPosition = 0;

    public ScmInputStreamDataReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public int read(byte[] buff, int offset, int len) throws IOException {
        int size = inputStream.read(buff, offset, len);
        if (size == -1) {
            isEof = true;
            return -1;
        }
        currentPosition += size;
        return size;
    }

    public void seek(long size) throws ScmReaderException, IOException {
        if (size < currentPosition) {
            throw new ScmReaderException(
                    "can not seek back,currentPosition=" + currentPosition + ",seekSize=" + size);
        }
        else if (size == currentPosition) {
            return;
        }
        long actualSize = inputStream.skip(size - currentPosition);

        if (actualSize == size - currentPosition) {
            currentPosition += actualSize;
        }
        else if (actualSize < size - currentPosition) {
            logger.debug("skip " + actualSize + " bytes,expect skip " + (size - currentPosition)
                    + " bytes,currentPosition=" + currentPosition + ",seekSize=" + size
                    + ",do seek again now");
            currentPosition += actualSize;
            seek(size);
        }
        else {
            throw new ScmReaderException("seek failed,expect skip " + (size - currentPosition)
                    + " bytes,actual skip " + actualSize + " bytes");
        }

    }

    public boolean isEof() {
        return isEof;
    }

    public long getCurrentPosition() {
        return currentPosition;
    }
}
