package com.sequoiacm.om.omserver.module;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import com.sequoiacm.om.omserver.common.CommonUtil;

public class OmFileContent implements Closeable {
    private String fileName;
    private long fileLength;
    private InputStream fileContent;

    public OmFileContent(String fileName, long fileLength, InputStream fileContent) {
        this.fileContent = fileContent;
        this.fileLength = fileLength;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    public InputStream getFileContent() {
        return fileContent;
    }

    @Override
    public void close() throws IOException {
        CommonUtil.closeResource(fileContent);
    }

}
