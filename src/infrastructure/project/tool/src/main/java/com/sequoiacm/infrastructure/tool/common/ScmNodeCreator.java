package com.sequoiacm.infrastructure.tool.common;

import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.exec.ScmExecutorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class ScmNodeCreator {
    private final Logger logger = LoggerFactory.getLogger(ScmNodeCreator.class);
    private ScmNodeType type;
    private Properties prop;
    private ScmNodeTypeList nodeTypes;
    private Map<Object, Object> otherLog = null;
    private boolean loadDefaultPortAble = true;

    public ScmNodeCreator(ScmNodeType type, Properties prop, ScmNodeTypeList nodeTypes) {
        this.nodeTypes = nodeTypes;
        this.type = type;
        this.prop = prop;
    }

    public ScmNodeCreator(ScmNodeType type, Properties prop, ScmNodeTypeList nodeTypes,
                          Map<Object, Object> otherLog, boolean loadDefaultPortAble) {
        this(type, prop, nodeTypes);
        this.otherLog = otherLog;
        this.loadDefaultPortAble = loadDefaultPortAble;
    }

    public void create() throws ScmToolsException {
        String portStr = prop.getProperty(ScmToolsDefine.PROPERTIES.SERVER_PORT);
        if (portStr == null) {
            if (loadDefaultPortAble) {
                portStr = loadDefaultPort();
            } else {
                throw new ScmToolsException(
                        "port is not specified:key=" + ScmToolsDefine.PROPERTIES.SERVER_PORT,
                        ScmBaseExitCode.INVALID_ARG);
            }
        }
        int port = convertStr2Port(portStr);
        if (port > 65535 || port < 0) {
            throw new ScmToolsException("port out of range:" + port, ScmBaseExitCode.INVALID_ARG);
        }

        ScmExecutorWrapper exe = new ScmExecutorWrapper(this.nodeTypes);
        if (exe.getAllNode().containsKey(port)) {
            throw new ScmToolsException("The port is already occupied,port:" + port + ",conf path:"
                    + exe.getNode(port).getConfPath(), ScmBaseExitCode.SCM_ALREADY_EXIST_ERROR);
        }
        createNodeByType(type, port);
        System.out.println("Create node success: " + type.getUpperName() + "(" + port + ")");
    }

    private String loadDefaultPort() throws ScmToolsException {
        String resourcefile = type.getName() + "." + ScmToolsDefine.FILE_NAME.APP_PROPS;
        InputStream is = ScmNodeCreator.class.getClassLoader().getResourceAsStream(
                resourcefile);
        if (is == null) {
            throw new ScmToolsException("missing resource file:" + resourcefile,
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        String port;
        try {
            Properties p = new Properties();
            p.load(is);
            port = p.getProperty(ScmToolsDefine.PROPERTIES.SERVER_PORT);
        }
        catch (IOException e) {
            throw new ScmToolsException("load resource file failed:" + resourcefile,
                    ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("load resource file failed:" + resourcefile,
                    ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            close(is);
        }
        return port;
    }

    private void createNodeByType(ScmNodeType type, int port) throws ScmToolsException {
        String nodeConfPath = "." + File.separator + ScmToolsDefine.FILE_NAME.CONF + File.separator
                + type.getName() + File.separator + port;
        ScmCommon.createDir(nodeConfPath);

        String nodeAppPropsPath = nodeConfPath + File.separator
                + ScmToolsDefine.FILE_NAME.APP_PROPS;
        createConf(type.getName() + "." + ScmToolsDefine.FILE_NAME.APP_PROPS, nodeAppPropsPath,
                prop);

        Properties logProp = new Properties();
        String logOutputPath = "." + File.separator + ScmToolsDefine.FILE_NAME.LOG + File.separator
                + type.getName() + File.separator + port;
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_PATH_VALUE, logOutputPath);

        String logName = type.getName().replace("-", "") + ".log";
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_NAME_VALUE, logName);
        if (otherLog != null) {
            Set<Object> keys = this.otherLog.keySet();
            for (Object k : keys) {
                logProp.put(k, otherLog.get(k));
            }
        }
        String nodeLogbackPath = nodeConfPath + File.separator + ScmToolsDefine.FILE_NAME.LOGBACK;

        createConf(type.getName() + "." + ScmToolsDefine.FILE_NAME.LOGBACK, nodeLogbackPath,
                logProp);
    }

    private int convertStr2Port(String portStr) throws ScmToolsException {
        try {
            return Integer.valueOf(portStr);
        }
        catch (NumberFormatException e) {
            throw new ScmToolsException("invalid port:" + portStr, ScmBaseExitCode.INVALID_ARG, e);
        }
    }

    private void createConf(String sampleConf, String outputConfPath, Properties modifier)
            throws ScmToolsException {
        InputStream is = ScmNodeCreator.class.getClassLoader().getResourceAsStream(sampleConf);
        if (is == null) {
            logger.error("missing resource file:" + sampleConf);
            throw new ScmToolsException("missing resource file:" + sampleConf,
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            bw = new BufferedWriter(new FileWriter(outputConfPath));
            if (sampleConf.contains(ScmToolsDefine.FILE_NAME.APP_PROPS)) {
                modifyForAppConf(modifier, br, bw);
            }
            else {
                modifyForLogConf(modifier, br, bw);
            }

        }
        catch (IOException e) {
            logger.error("write config to " + outputConfPath + " occur error", e);
            throw new ScmToolsException("write config to " + outputConfPath + " occur error:"
                    + e.getMessage(), ScmBaseExitCode.SYSTEM_ERROR);
        }
        finally {
            close(bw);
            close(br);
        }
    }

    private void modifyForAppConf(Properties modifier, BufferedReader br, BufferedWriter bw)
            throws IOException {
        String line = null;
        while ((line = br.readLine()) != null) {
            Iterator<Entry<Object, Object>> modifierIt = modifier.entrySet().iterator();
            while (modifierIt.hasNext()) {
                String modifyKey = (String) modifierIt.next().getKey();
                if (line.contains(modifyKey)) {
                    String[] arr = line.split("=");
                    if (arr[0].trim().equals(modifyKey)) {
                        line = modifyKey + "=" + modifier.get(modifyKey);
                        modifierIt.remove();
                        break;
                    }
                }
            }
            bw.write(line);
            bw.newLine();
        }

        if (!modifier.isEmpty()) {
            bw.newLine();
            bw.write("#custom properties");
            bw.newLine();
            for (Entry<Object, Object> prop : modifier.entrySet()) {
                bw.write(prop.getKey() + "=" + prop.getValue());
                bw.newLine();
            }
        }
    }

    private void modifyForLogConf(Properties modifier, BufferedReader br, BufferedWriter bw)
            throws IOException {
        String line = null;
        while ((line = br.readLine()) != null) {
            for (Entry<Object, Object> entry : modifier.entrySet()) {
                if (line.contains(entry.getKey().toString())) {
                    line = line.replace(entry.getKey().toString(), entry.getValue().toString());
                    break;
                }
            }
            bw.write(line);
            bw.newLine();
        }
    }

    private void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        }
        catch (Exception e) {
            logger.warn("close file occur error", e);
        }
    }

}
