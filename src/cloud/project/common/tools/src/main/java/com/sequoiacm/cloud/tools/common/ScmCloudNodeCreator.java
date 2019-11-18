package com.sequoiacm.cloud.tools.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.tools.ScmCtl;
import com.sequoiacm.cloud.tools.command.ScmCreateNodeToolImpl;
import com.sequoiacm.cloud.tools.element.ScmNodeType;
import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.cloud.tools.exception.ScmToolsException;
import com.sequoiacm.cloud.tools.exec.ScmExecutorWrapper;

public class ScmCloudNodeCreator {
    private final Logger logger = LoggerFactory.getLogger(ScmCloudNodeCreator.class);
    private ScmNodeType type;
    private Properties prop;
    private String auditSdbUrl;
    private String auditSdbUser;
    private String auditSdbPasswd;

    public ScmCloudNodeCreator(ScmNodeType type, Properties prop, String auditSdbUrl,
            String auditSdbUser, String auditSdbPasswd) {
        this.type = type;
        this.prop = prop;
        this.auditSdbUrl = auditSdbUrl;
        this.auditSdbUser = auditSdbUser;
        this.auditSdbPasswd = auditSdbPasswd;
    }

    public void create() throws ScmToolsException {
        String portStr = prop.getProperty(ScmToolsDefine.PROPERTIES.SERVER_PORT);
        if (portStr == null) {
            portStr = loadDefaultPort();
        }
        int port = convertStr2Port(portStr);
        if (port > 65535 || port < 0) {
            throw new ScmToolsException("port out of range:" + port, ScmExitCode.INVALID_ARG);
        }

        ScmExecutorWrapper exe = new ScmExecutorWrapper();
        if (exe.getAllNode().containsKey(port)) {
            throw new ScmToolsException("The port is already occupied,port:" + port + ",conf path:"
                    + exe.getNode(port).getConfPath(), ScmExitCode.SCM_PORT_OCCUPIED);
        }
        createNodeByType(type, port);
        System.out.println("Create node success: " + type + "(" + port + ")");
    }

    private String loadDefaultPort() throws ScmToolsException {
        String resourcefile = type.getName() + "." + ScmToolsDefine.FILE_NAME.APP_PROPS;
        InputStream is = ScmCreateNodeToolImpl.class.getClassLoader()
                .getResourceAsStream(resourcefile);
        if (is == null) {
            throw new ScmToolsException("missing resource file:" + resourcefile,
                    ScmExitCode.SYSTEM_ERROR);
        }
        String port;
        try {
            Properties p = new Properties();
            p.load(is);
            port = p.getProperty(ScmToolsDefine.PROPERTIES.SERVER_PORT);
        }
        catch (IOException e) {
            throw new ScmToolsException("load resource file failed:" + resourcefile,
                    ScmExitCode.IO_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("load resource file failed:" + resourcefile,
                    ScmExitCode.SYSTEM_ERROR, e);
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
        createConf(type.getConfTemplateNamePrefix() + "." + ScmToolsDefine.FILE_NAME.APP_PROPS,
                nodeAppPropsPath, prop);

        Properties logProp = new Properties();
        String logOutputPath = "." + File.separator + ScmToolsDefine.FILE_NAME.LOG + File.separator
                + type.getName() + File.separator + port;
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_PATH_VALUE, logOutputPath);

        String logName = type.getName().replace("-", "") + ".log";
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_NAME_VALUE, logName);
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_NAME_VALUE, logName);
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_URL, auditSdbUrl);
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_USER, auditSdbUser);
        logProp.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_PASSWD, auditSdbPasswd);
        String nodeLogbackPath = nodeConfPath + File.separator + ScmToolsDefine.FILE_NAME.LOGBACK;

        createConf("spring-app." + ScmToolsDefine.FILE_NAME.LOGBACK, nodeLogbackPath, logProp);
    }

    private int convertStr2Port(String portStr) throws ScmToolsException {
        try {
            return Integer.valueOf(portStr);
        }
        catch (NumberFormatException e) {
            throw new ScmToolsException("invalid port:" + portStr, ScmExitCode.INVALID_ARG, e);
        }
    }

    private void createConf(String sampleConf, String outputConfPath, Properties modifier)
            throws ScmToolsException {
        InputStream is = ScmCtl.class.getClassLoader().getResourceAsStream(sampleConf);
        if (is == null) {
            logger.error("missing resource file:" + sampleConf);
            throw new ScmToolsException("missing resource file:" + sampleConf,
                    ScmExitCode.SYSTEM_ERROR);
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
            throw new ScmToolsException(
                    "write config to " + outputConfPath + " occur error:" + e.getMessage(),
                    ScmExitCode.IO_ERROR);
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
            SortedMap<Object, Object> forSort = new TreeMap<>(modifier);
            for (Entry<Object, Object> prop : forSort.entrySet()) {
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
