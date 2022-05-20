package com.sequoiacm.daemon.lock;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScmFileResource {
    private Gson gson = new Gson();
    private RandomAccessFile raf;
    private FileChannel channel;
    private File file;
    private String backUpPath;
    private static final Logger logger = LoggerFactory.getLogger(ScmFileResource.class);

    public ScmFileResource(File file) throws ScmToolsException {
        try {
            this.file = file;
            this.raf = new RandomAccessFile(this.file, "rw");
            this.channel = raf.getChannel();
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to find file,file:" + file.getAbsolutePath(),
                    ScmExitCode.FILE_NOT_FIND, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed init file resource,file:" + file.getAbsolutePath(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public ScmFileResource(File file, String backUpPath) throws ScmToolsException {
        try {
            this.backUpPath = backUpPath;
            this.file = file;
            this.raf = new RandomAccessFile(this.file, "rw");
            this.channel = raf.getChannel();
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to find file,file:" + file.getAbsolutePath(),
                    ScmExitCode.FILE_NOT_FIND, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed init file resource,file:" + file.getAbsolutePath(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public List<ScmNodeInfo> readFile() throws ScmToolsException {
        List<ScmNodeInfo> nodeList = null;
        try {
            // 将监控表的内容转成节点列表
            nodeList = readFileAndTurnToNodeInfo();
        }
        catch (ScmToolsException e) {
            // 如果转换失败就通过备份表恢复，并再读一次监控表
            recoverFile();
            nodeList = readFileAndTurnToNodeInfo();
        }
        return nodeList;
    }

    private List<ScmNodeInfo> readFileAndTurnToNodeInfo() throws ScmToolsException {
        Charset charset = Charset.forName(DaemonDefine.ENCODE_TYPE);
        CharsetDecoder decoder = charset.newDecoder();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder();
        try {
            raf.seek(0);
            while (channel.read(buffer) != -1) {
                buffer.flip();
                // 将刚读入buffer的内容按照utf-8解码
                CharBuffer cb = decoder.decode(buffer);
                sb.append(cb);
                buffer.clear();
            }

            String jsonList = sb.toString();
            if (jsonList.length() == 0) {
                return new ArrayList<>();
            }
            else {
                return gson.fromJson(jsonList, new TypeToken<ArrayList<ScmNodeInfo>>() {
                }.getType());
            }
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "Failed to parse json file to list, please exec the command again, if failed, please delete both files, "
                    + "and restart daemon to init source file again,source file: "
                    + file.getAbsolutePath() + "backup file:" + backUpPath,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public void writeFile(List<ScmNodeInfo> nodeList) throws ScmToolsException {
        boolean isWriteSuccess = false;
        try {
            // 备份监控表
            backUpFile();
            String json = gson.toJson(nodeList);
            // 清空监控表的内容
            raf.setLength(0);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            byte[] jsonData = json.getBytes(DaemonDefine.ENCODE_TYPE);
            for (int i = 0; i < jsonData.length;) {
                buffer.put(jsonData, i, Math.min(jsonData.length - i, buffer.limit()));
                buffer.flip();
                i += channel.write(buffer);
                buffer.clear();
            }
            // 强制将通道里尚未写入磁盘的数据强制写到磁盘上
            channel.force(true);
            isWriteSuccess = true;
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmToolsException("Failed to parse nodeList to json file",
                    ScmExitCode.INVALID_ARG, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to parse nodeList to json file",
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            if (!isWriteSuccess) {
                recoverFile();
            }
        }
    }

    private void backUpFile() throws ScmToolsException {
        if (backUpPath == null) {
            throw new ScmToolsException("Failed to backup file caused by backup filepath is null",
                    ScmExitCode.INVALID_ARG);
        }
        File backUpFile = new File(backUpPath);
        FileOutputStream fo = null;
        FileChannel backUpChannel = null;
        try {
            if (!backUpFile.exists()) {
                ScmCommon.createFile(backUpPath);
            }
            fo = new FileOutputStream(backUpPath);
            backUpChannel = fo.getChannel();
            channel.transferTo(0, channel.size(), backUpChannel);
            logger.info("Back up file success, file:{},backup:{}", file.getAbsolutePath(),
                    backUpPath);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "Failed to back up file, please exec the command again, file:"
                            + file.getAbsolutePath() + ",backup:" + backUpFile,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            CommonUtils.closeResource(backUpChannel);
            CommonUtils.closeResource(fo);
        }
    }

    private void recoverFile() throws ScmToolsException {
        FileInputStream backUpInput = null;
        FileChannel backUpChannel = null;
        try {
            backUpInput = new FileInputStream(backUpPath);
            backUpChannel = backUpInput.getChannel();
            raf.setLength(0);
            backUpChannel.transferTo(0, backUpChannel.size(), channel);
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("Failed to recover file, caused by source file is damaged, "
                    + "but backup file isn't exist,please delete source file and restart daemon again "
                    + "to init source file again, source file:" + file.getAbsolutePath()
                    + ", backup file:" + backUpPath, ScmExitCode.FILE_NOT_FIND, e);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "Failed to recover file, caused by source file is damaged,but copy backup file failed, "
                            + "please change the backup file name to source file name, source file:"
                            + file.getAbsolutePath() + ", backup file:" + backUpPath,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            CommonUtils.closeResource(backUpChannel);
            CommonUtils.closeResource(backUpInput);
        }
    }

    public void clearFileResource() {
        if (file.exists()) {
            try {
                raf.setLength(0);
            }
            catch (IOException e) {
                logger.error(
                        "Failed to empty file,file:{},please delete or empty the file before try the command again",
                        file.getAbsolutePath(), e);
            }
        }
    }

    public void releaseFileResource() {
        CommonUtils.closeResource(channel);
        CommonUtils.closeResource(raf);
    }

    public ScmFileLock createLock() {
        ScmFileLock lock = new ScmFileLock(channel);
        return lock;
    }
}
