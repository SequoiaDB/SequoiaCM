package com.sequoiacm.diagnose.common;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import com.sequoiacm.tools.exception.ScmExitCode;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileOperator {
    private static FileOperator INSTANCE = new FileOperator();
    private Map</* filepath */String, ScmFileResource> fileResourceMap = new HashMap<>();
    private Map</* filepath */String, ReentrantReadWriteLock.WriteLock> fileLockMap = new HashMap<>();

    private FileOperator() {
    }

    public static FileOperator getInstance() {
        return INSTANCE;
    }

    public void addFileResource(String filePath) throws ScmToolsException {
        File file = new File(filePath);
        ScmFileResource fileResource = ScmResourceFactory.getInstance().createFileResource(file);
        fileResourceMap.put(filePath, fileResource);
        fileLockMap.put(filePath, new ReentrantReadWriteLock().writeLock());
    }

    public void write2File(String filePath, String content) throws ScmToolsException {
        ReentrantReadWriteLock.WriteLock writeLock = fileLockMap.get(filePath);
        ScmFileResource fileResource = fileResourceMap.get(filePath);
        if (null == writeLock || null == fileResource) {
            throw new ScmToolsException("failed to write result to file,filePath=" + filePath,
                    ScmExitCode.SYSTEM_ERROR);
        }
        writeLock.lock();
        try {
            fileResource.writeFile(content, true);
        }
        finally {
            writeLock.unlock();
        }
    }

    public void close() {
        Collection<ScmFileResource> fileResourceList = fileResourceMap.values();
        for (ScmFileResource resource : fileResourceList) {
            resource.release();
        }
    }
}
