package com.sequoiacm.client.dispatcher;

import java.io.IOException;
import java.io.InputStream;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;

public class CloseableHttpResponseInputStream extends InputStream {
    private CloseableHttpResponseWrapper response;
    private InputStream inputStream;
    private boolean isEOF = false;

    public CloseableHttpResponseInputStream(CloseableHttpResponseWrapper response)
            throws ScmException {
        this.response = response;
        try {
            this.inputStream = response.getEntity().getContent();
        } catch (Exception e) {
            throw new ScmSystemException(
                    "Failed to get content from response", e);
        }
    }

    @Override
    public int read() throws IOException {
        int ret = inputStream.read();
        if (ret == -1) {
            isEOF = true;
        }
        return ret;
    }

    @Override
    public int read(byte b[]) throws IOException {
        int ret = inputStream.read(b);
        if (ret == -1) {
            isEOF = true;
        }
        return ret;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int ret = inputStream.read(b, off, len);
        if (ret == -1) {
            isEOF = true;
        }
        return ret;
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() {

        if (!isEOF) {
            // inputStream is not empty, close the connection
            response.closeResponse();
        }
        else {
            // make sure reuse connection
            response.consumeEntity();
        }
        
        // no need close inputStream
    }

    @Override
    public void mark(int readLimit) {
        inputStream.mark(readLimit);
    }

    @Override
    public void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}
