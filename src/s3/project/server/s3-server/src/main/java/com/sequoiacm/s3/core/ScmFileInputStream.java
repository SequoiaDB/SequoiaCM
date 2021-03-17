package com.sequoiacm.s3.core;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.ContenServerService;
import feign.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import static com.sequoiacm.s3.remote.ScmContentServerClient.checkResponse;

public class ScmFileInputStream extends InputStream {

    private InputStream is;
    private ContenServerService service;
    private ScmSession session;
    private String ws;
    private String fileId;

    private long offset = 0L;
    // 当前共读取的文件内容长度
    private long currentReadLength = 0L;
    // 本次请求所读取的文件内容长度
    private long currentPartLength = 0L;
    // 客户端请求的文件内容总长度（-1表示读取至文件结尾）
    private long totalRequestLength = 0L;

    public ScmFileInputStream(ContenServerService service, ScmSession session, String ws, String fileId,
            long off, long len) throws ScmFeignException, S3ServerException {
        this.service = service;
        this.session = session;
        this.ws = ws;
        this.fileId = fileId;
        this.offset = off;
        this.totalRequestLength = len;
        try {
            nextPartOfFile();
        } catch (IOException e) {
            throw new S3ServerException(S3Error.SCM_DOWNLOAD_FILE_FAILED,
                    "failed to download scm file:ws=" + ws + ",fileId=" + fileId, e);
        }
    }

    @Override
    public int read() throws IOException {
        throw new IOException("this method does not support being called.");
    }

    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        int tempLength = is.read(buf, 0, len);
        if (tempLength == -1) {
            // 若totalRequestLength构造入参为-1，不处理数据长度问题，直接返回-1
            if (totalRequestLength == -1) {
                return -1;
            }
            // 判断当前共读取文件内容长度是否已经达到预期
            if (currentReadLength >= totalRequestLength) {
                return -1;
            }
            try {
                // 关闭上一个输入流，接着获取下一段文件内容，并更新streamRemainLength字段
                close();
                nextPartOfFile();
                // 若获取文件内容长度为-1，且当前共读取的文件内容长度（currentReadLength）尚未达到请求的总长度，抛出异常：文件损坏或被修改
                if (currentPartLength == -1) {
                    throw new IOException("the file may be damaged or modified: expected length:"
                            + totalRequestLength + ",actual length:" + currentReadLength + ",ws="
                            + ws + ", file=" + fileId);
                }
            }
            catch (ScmFeignException | IOException e) {
                throw new IOException(
                        "failed to get scm file:ws=" + ws + ", file=" + fileId + ",cause by:", e);
            }
            // 返回0，让调用者继续调用本方法读取内容
            return 0;
        }
        offset += tempLength;
        currentReadLength += tempLength;
        return tempLength;
    }

    private void nextPartOfFile() throws ScmFeignException, IOException {
        Response resp = service.downloadFile(session.getSessionId(), session.getUserDetail(), ws,
                fileId, offset, totalRequestLength - currentReadLength);
        checkResponse("downloadFile", resp);
        is = resp.body().asInputStream();
        LinkedList<String> data_length = (LinkedList<String>) resp.headers().get("data_length");
        currentPartLength = Long.valueOf(data_length.get(0));
    }

    public void close() throws IOException {
        if (is != null) {
            is.close();
        }
    }
}
