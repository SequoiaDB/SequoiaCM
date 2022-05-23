package com.sequoiacm.mappingutil.exec;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import com.sequoiacm.mappingutil.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;

import java.io.File;

public class FileIdCursorFromLocalFile implements FileIdCursor {

    private ScmFileResource fileResource;
    private long lineNum;

    public FileIdCursorFromLocalFile(String filePath, long skip)
            throws ScmToolsException {
        try {
            this.fileResource = ScmResourceFactory.getInstance()
                    .createFileResource(new File(filePath));
            this.lineNum = skip;
            // 跳过指定行数
            while (skip-- > 0) {
                this.fileResource.readLine();
            }
        }
        catch (Exception e) {
            if (fileResource != null) {
                fileResource.release();
            }
            throw new ScmToolsException("Failed to init id cursor", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public long getMarker() {
        return this.lineNum;
    }

    @Override
    public ScmId getNext() throws ScmToolsException {
        String fileIdStr = fileResource.readLine();
        try {
            ScmId fileId = new ScmId(fileIdStr);
            lineNum++;
            return fileId;
        }
        catch (ScmException e) {
            throw new ScmToolsException("The format of the file id is illegal, id=" + fileIdStr,
                    ScmExitCode.INVALID_ARG, e);
        }
    }

    @Override
    public boolean hasNext() throws ScmToolsException {
        return !fileResource.isEof();
    }

    @Override
    public void close() {
        fileResource.release();
    }
}
