package com.sequoiacm.fulltext.server.sch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;

public class ScmFileUtil {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileUtil.class);

    // f1的版本大于等于f2返回true， 否则返回false
    public static boolean compareFileVersion(ScmFileInfo f1, ScmFileInfo f2) {
        if (f1.getMajorVersion() > f2.getMajorVersion()) {
            return true;
        }

        if (f1.getMajorVersion() == f2.getMajorVersion()
                && f1.getMinorVersion() >= f2.getMinorVersion()) {
            return true;
        }
        return false;
    }

    public static int travelCursorSilenceForFileCount(ScmEleCursor<ScmFileInfo> cursor) {
        int count = 0;
        try {
            while (cursor.hasNext()) {
                count++;
            }
        }
        catch (Exception e) {
            logger.warn("failed to travel file cursor", e);
        }
        return count;
    }

    public static boolean isFirstVersion(ScmFileInfo f) {
        return f.getMajorVersion() == 1 && f.getMinorVersion() == 0;
    }
}
