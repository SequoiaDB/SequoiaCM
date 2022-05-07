package com.sequoiacm.s3import.fileoperation;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonUtils;
import com.sequoiacm.s3import.exception.S3ImportExitCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class S3ImportFileResource {

    private static final String ENCODE_TYPE = StandardCharsets.UTF_8.name();
    private static final int BUFFER_SIZE = 1024;
    private ByteBuffer buffer;
    private RandomAccessFile raf;
    private FileChannel channel;
    private File file;

    public S3ImportFileResource(File file) throws ScmToolsException {
        try {
            this.file = file;
            this.raf = new RandomAccessFile(this.file, "rw");
            this.channel = raf.getChannel();
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to find file,file:" + file.getAbsolutePath(),
                    S3ImportExitCode.FILE_NOT_FIND, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed init file resource,file:" + file.getAbsolutePath(),
                    S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }

    public String readFile() throws ScmToolsException {
        if (buffer == null) {
            buffer = ByteBuffer.allocate(BUFFER_SIZE);
        }
        Charset charset = Charset.forName(ENCODE_TYPE);
        CharsetDecoder decoder = charset.newDecoder();
        StringBuilder sb = new StringBuilder();
        try {
            raf.seek(0);
            while (channel.read(buffer) != -1) {
                buffer.flip();
                CharBuffer cb = decoder.decode(buffer);
                sb.append(cb);
                buffer.clear();
            }

            return sb.toString();
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to read file", S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }

    public String readLine() throws ScmToolsException {
        try {
            String lineStr = raf.readLine();
            if (lineStr == null) {
                return null;
            }
            // readLine() 内部实现中对读取的字节转换为 char 类型，相当于完成了 ISO-8859-1 解码
            // 需要反编码回文件中原始的字符流
            byte[] originalBytes = lineStr.getBytes(StandardCharsets.ISO_8859_1);
            return new String(originalBytes, ENCODE_TYPE);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to read line", S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }

    public void writeFile(String content) throws ScmToolsException {
        writeFile(content, false);
    }

    public void writeFile(String content, boolean isAppend) throws ScmToolsException {
        if (buffer == null) {
            buffer = ByteBuffer.allocate(BUFFER_SIZE);
        }
        try {
            // 追加写时 seek 到文件末端，且第二行开始需要加换行符
            if (isAppend) {
                raf.seek(file.length());
                if (file.length() > 0) {
                    content = System.lineSeparator() + content;
                }
            }
            else {
                // 清空表里面的内容
                raf.setLength(0);
            }
            byte[] data = content.getBytes(ENCODE_TYPE);
            for (int i = 0; i < data.length;) {
                buffer.put(data, i, Math.min(data.length - i, buffer.limit()));
                buffer.flip();
                i += channel.write(buffer);
                buffer.clear();
            }
            // 将通道里尚未写入磁盘的数据强制写到磁盘上
            channel.force(true);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to write file", S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }

    public S3ImportFileLock createLock() {
        S3ImportFileLock lock = new S3ImportFileLock(channel);
        return lock;
    }

    public void release() {
        CommonUtils.closeResource(channel, raf);
    }
}
