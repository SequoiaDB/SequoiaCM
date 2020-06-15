package com.sequoiadb.infrastructure.map.client;

import java.io.IOException;
import java.io.InputStream;

import com.sequoiadb.infrastructure.map.CommonHelper;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

import feign.Response;

public class CloseableHttpResponseInputStream extends InputStream {
    private Response response;
    private InputStream inputStream;
    private boolean isEOF = false;

    public CloseableHttpResponseInputStream(Response response) throws ScmMapServerException {
        this.response = response;
        try {
            this.inputStream = response.body().asInputStream();
        }
        catch (IOException e) {
            throw new ScmMapServerException(ScmMapError.NETWORK_IO,
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
        CommonHelper.close(inputStream);
        CommonHelper.close(response);
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
