package com.sequoiacm.diagnose.utils;

import com.sequoiacm.diagnose.common.FileOperator;
import com.sequoiacm.diagnose.config.WorkPathConfig;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import java.util.List;

public class FileOperateUtils {

    public static void appendCompareResult(List<String> resultList) throws ScmToolsException {
        String s = covertList2String(resultList);
        if (null == s) {
            return;
        }
        FileOperator.getInstance()
                .write2File(WorkPathConfig.getInstance().getCompareResultFilePath(), s);
    }

    public static void appendNullMd5ToFile(String fileId, int majorVersion, int minorVersion)
            throws ScmToolsException {
        String result = fileId + "," + majorVersion + "," + minorVersion;
        FileOperator.getInstance().write2File(WorkPathConfig.getInstance().getNullMd5FilePath(),
                result);
    }

    public static void appendResidueIdList(List<String> residueList) throws ScmToolsException {
        String s = covertList2String(residueList);
        if (null == s) {
            return;
        }
        FileOperator.getInstance().write2File(WorkPathConfig.getInstance().getResidueIdFilePath(),
                s);
    }

    public static void appendErrorIdList(List<String> errorList) throws ScmToolsException {
        String s = covertList2String(errorList);
        if (null == s) {
            return;
        }
        FileOperator.getInstance()
                .write2File(WorkPathConfig.getInstance().getResidueErrorFilePath(), s);
    }

    private static String covertList2String(List<String> list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
