package com.sequoiacm.daemon.common;

import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.exec.ScmExecutor;
import com.sequoiacm.daemon.exec.ScmLinuxExecutorImpl;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileLock;

public class CommonUtils {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);

    public static void closeResource(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (Exception e) {
                logger.warn("Failed to close resource:{}", c, e);
            }
        }
    }

    public static void releaseLock(FileLock lock) {
        if (lock != null) {
            try {
                lock.release();
            }
            catch (IOException e) {
                logger.warn("Failed to release lock:{}", lock, e);
            }
        }
    }

    // 获取jarPath的方法
    public static String getJarPath(Class<?> className) throws ScmToolsException {
        URL url = className.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            String jar = ".jar";
            filePath = URLDecoder.decode(url.getPath(), DaemonDefine.ENCODE_TYPE);
            logger.debug("JarDir={}", filePath);
            int endIdx = filePath.indexOf(jar);
            endIdx += jar.length();
            int startIdx = filePath.indexOf(":") + 1;
            if (startIdx >= endIdx) {
                startIdx = 0;
            }

            filePath = filePath.substring(startIdx, endIdx);
            logger.debug("JarDir={}", filePath);
        }
        catch (Exception e) {
            throw new ScmToolsException("Get path failed:className=" + className.toString(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }

        return new File(filePath).getAbsolutePath();
    }

    public static String getUser() {
        return System.getProperty(DaemonDefine.USER_NAME);
    }

    public static ScmExecutor getExecutor() throws ScmToolsException {
        ScmExecutor executor;
        if (ScmCommon.isLinux()) {
            executor = new ScmLinuxExecutorImpl();
        }
        else {
            throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
        }
        return executor;
    }
}
