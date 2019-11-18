package com.sequoiacm.client.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.exception.ScmError;

abstract class ScmHttpOutputStreamBase implements ScmOutputStream {
    private Logger logger = LoggerFactory.getLogger(ScmHttpOutputStreamBase.class.getName());

    private OutputStream outputStream;
    private InputStream inputStream;
    private InputStream errInputStream;
    private HttpURLConnection connection;
    private boolean isClosed;

    // read response from remote and do something others, do not catch
    // IOException
    protected abstract void processAfterCommit() throws IOException, ScmException;

    // we will throws this exception when failed to decode error response.
    protected abstract ScmException _handleErrorResp(IOException cause);

    protected abstract HttpURLConnection createHttpUrlConnection() throws ScmException;

    protected HttpURLConnection getConnection() throws ScmException {
        if (connection == null) {
            this.connection = createHttpUrlConnection();
        }
        return connection;
    }

    protected OutputStream getOutputStream() throws ScmException {
        if (outputStream == null) {
            try {
                outputStream = getConnection().getOutputStream();
            }
            catch (IOException e) {
                throw new ScmException(ScmError.NETWORK_IO,
                        "get outputstream from connection failed", e);
            }
        }
        return outputStream;
    }

    protected InputStream getErrInputStream() throws ScmException {
        if (errInputStream == null) {
            errInputStream = getConnection().getErrorStream();
        }
        return errInputStream;
    }

    private ScmException handleErrResp(IOException cause) {
        String erroResp;
        try {
            erroResp = readStringFromStream(getErrInputStream());
        }
        catch (Exception e) {
            logger.warn("failed to get error resp", e);
            return _handleErrorResp(cause);
        }

        try {
            BSONObject errBSON = (BSONObject) JSON.parse(erroResp);
            return new ScmException((Integer) errBSON.get("status"),
                    (String) errBSON.get("message"));
        }
        catch (Exception e) {
            logger.warn("failed to decode error resp, errorResp={}", erroResp, e);
            return _handleErrorResp(cause);
        }
    }

    protected String readStringFromStream(InputStream is) throws IOException {
        StringBuilder strSB = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            strSB.append(line);
        }
        reader.close();
        return strSB.toString();
    }

    @Override
    public void write(byte[] b, int off, int len) throws ScmException {
        if (isClosed) {
            throw new ScmException(ScmError.OUTPUT_STREAM_CLOSED, "Stream Closed");
        }

        if (b == null) {
            throw new ScmInvalidArgumentException("byteArray is null");
        }
        if (len < 0) {
            throw new ScmInvalidArgumentException("len must be greater than zero:" + len);
        }
        if (off + len > b.length || off < 0) {
            throw new ScmInvalidArgumentException(
                    "indexOutOfBound,arraySize:" + b.length + ",off:" + off + ",len:" + len);
        }

        if (len == 0) {
            return;
        }

        try {
            OutputStream os = getOutputStream();
            os.write(b, off, len);
        }
        catch (IOException e) {
            throw new ScmException(ScmError.NETWORK_IO, "write data to ouputstream failed", e);
        }
    }

    @Override
    public void write(byte[] b) throws ScmException {
        if (b == null) {
            throw new ScmInvalidArgumentException("byteArray is null");
        }
        write(b, 0, b.length);
    }

    @Override
    public void cancel() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        disconnectHttpUrlConnection();
        closeStreamNoLogging();
    }

    @Override
    public void commit() throws ScmException {
        if (isClosed) {
            throw new ScmException(ScmError.OUTPUT_STREAM_CLOSED, "Stream Closed");
        }

        try {
            OutputStream os = getOutputStream();
            os.flush();
            os.close();

            processAfterCommit();
        }
        catch (IOException e) {
            throw handleErrResp(e);
        }
        finally {
            closeStream();
            disconnectHttpUrlConnection();
            isClosed = true;
        }
    }

    private void closeStream() {
        ScmHelper.closeStream(outputStream);
        ScmHelper.closeStream(inputStream);
        ScmHelper.closeStream(errInputStream);
    }

    private void closeStreamNoLogging() {
        ScmHelper.closeStreamNoLogging(outputStream);
        ScmHelper.closeStreamNoLogging(inputStream);
        ScmHelper.closeStreamNoLogging(errInputStream);
    }

    private void disconnectHttpUrlConnection() {
        if (connection != null) {
            try {
                connection.disconnect();
            }
            catch (Exception e) {
                logger.warn("close httpconnection failed", e);
            }
        }
    }
}
