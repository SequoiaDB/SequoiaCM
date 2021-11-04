package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScmChecksumFactory;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Checksum;

public class ScmBreakpointFileBufferedImpl extends ScmBreakpointFileImpl {

    private static final int CAPACITY = 5 * 1024 * 1024;

    private byte[] buffer = new byte[CAPACITY];
    private int offset;

    ScmBreakpointFileBufferedImpl(ScmWorkspace workspace, BSONObject obj, int breakpointSize)
            throws ScmException {
        super(workspace, obj, breakpointSize);
    }

    ScmBreakpointFileBufferedImpl(ScmWorkspace workspace, String fileName,
            ScmBreakpointFileOption op) throws ScmException {
        super(workspace, fileName, op);
    }

    @Override
    public long getChecksum() {
        Checksum cs;
        try {
            cs = ScmChecksumFactory.getChecksum(getChecksumType(), super.getChecksum());
        }
        catch (ScmException e) {
            throw new RuntimeException("Failed to get checksum", e);
        }
        cs.update(buffer, 0, offset);
        return cs.getValue();
    }

    @Override
    public long getUploadSize() {
        return super.getUploadSize() + offset;
    }

    @Override
    public void incrementalUpload(InputStream dataStream, boolean isLastContent)
            throws ScmException {
        if (dataStream == null) {
            throw new ScmInvalidArgumentException("fileStream is null");
        }

        if (isCompleted()) {
            throw new ScmInvalidArgumentException("The file is already completed");
        }

        if (isLastContent) {
            InputStreamWrapper isWrapper = new InputStreamWrapper(
                    new ByteArrayInputStream(buffer, 0, offset), dataStream);
            offset = 0;
            super.incrementalUpload(isWrapper, true);
            return;
        }

        if (isNew()) {
            super.incrementalUpload(new ByteArrayInputStream("".getBytes()), false);
        }

        try {
            int readLen;
            while ((readLen = dataStream.read(buffer, offset, CAPACITY - offset)) != -1) {
                offset += readLen;
                if (offset == CAPACITY) {
                    super.incrementalUpload(new ByteArrayInputStream(buffer), false);
                    offset = 0;
                }
            }
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO, "incrementUpload failed, workspace="
                    + getWorkspace() + ", fileName=" + getFileName(), e);
        }
    }

    class InputStreamWrapper extends InputStream {

        private List<InputStream> dataStreams;

        public InputStreamWrapper(InputStream... dataStreams) {
            this.dataStreams = Arrays.asList(dataStreams);
        }

        @Override
        public int read() throws IOException {
            throw new IOException("this method does not support being called.");
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, buffer.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            for (InputStream dataStream : dataStreams) {
                int readLen = dataStream.read(b, off, len);
                if (readLen != -1) {
                    return readLen;
                }
            }

            return -1;
        }

        @Override
        public void close() throws IOException {
            for (InputStream dataStream : dataStreams) {
                ScmHelper.closeStream(dataStream);
            }
        }
    }
}
