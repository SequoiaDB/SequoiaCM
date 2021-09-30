package com.sequoiacm.daemon.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class TableUtils {

    public static List<ScmNodeInfo> jsonFileToNodeList(File file) throws ScmToolsException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            String jsonList = IOUtils.toString(is, DaemonDefine.ENCODE_TYPE);
            return JSONObject.parseArray(jsonList, ScmNodeInfo.class);
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("File not found:" + file.getAbsolutePath(),
                    ScmExitCode.FILE_NOT_FIND, e);
        }
        catch (IOException e) {
            throw new ScmToolsException(
                    "Failed to parse json file to list,filePath: " + file.getAbsolutePath(),
                    ScmExitCode.IO_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "Failed to parse json file to list,filePath: " + file.getAbsolutePath(),
                    ScmExitCode.COMMON_UNKNOWN_ERROR, e);
        }
        finally {
            CommonUtils.closeResource(is);
        }
    }

    public static void nodeListToJsonFile(List<ScmNodeInfo> nodeList, File file)
            throws ScmToolsException {
        String json = JSON.toJSONString(nodeList);
        Writer writer = null;
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(file);
            writer = new OutputStreamWriter(fo, DaemonDefine.ENCODE_TYPE);
            writer.write(json);
            writer.flush();
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to parse nodeList to json file",
                    ScmExitCode.IO_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to parse nodeList to json file",
                    ScmExitCode.COMMON_UNKNOWN_ERROR, e);
        }
        finally {
            CommonUtils.closeResource(writer);
            CommonUtils.closeResource(fo);
        }
    }

    public static void copyFile(String fromPath, String toPath) throws ScmToolsException {
        File from = new File(fromPath);
        File to = new File(toPath);
        if (!from.exists()) {
            throw new ScmToolsException("Failed to copy file,caused by: " + fromPath + " not exist",
                    ScmExitCode.FILE_NOT_FIND);
        }
        if (!to.exists()) {
            ScmCommon.createFile(toPath);
        }
        try {
            Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to copy file,file:" + fromPath,
                    ScmExitCode.IO_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to copy file,file:" + fromPath,
                    ScmExitCode.COMMON_UNKNOWN_ERROR, e);
        }
    }
}
