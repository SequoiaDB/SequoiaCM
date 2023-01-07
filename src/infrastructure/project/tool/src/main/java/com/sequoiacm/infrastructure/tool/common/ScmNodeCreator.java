package com.sequoiacm.infrastructure.tool.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperatorGroup;

public class ScmNodeCreator {
    private final Logger logger = LoggerFactory.getLogger(ScmNodeCreator.class);
    private final ScmServiceNodeOperatorGroup nodeOeprators;
    private ScmNodeType type;
    private Properties prop;
    private Map<Object, Object> otherLog = null;
    private boolean loadDefaultPortAble = true;

    private BSONObject hystrixConfig = ScmCommon.parseBsonFromClassPathFile("hystrix.json");

    public ScmNodeCreator(ScmNodeType type, Properties prop,
            ScmServiceNodeOperatorGroup nodeOeprators) throws ScmToolsException {
        this.nodeOeprators = nodeOeprators;
        this.type = type;
        this.prop = prop;
    }

    public ScmNodeCreator(ScmNodeType type, Properties prop,
            ScmServiceNodeOperatorGroup nodeOeprators,
                          Map<Object, Object> otherLog, boolean loadDefaultPortAble) throws ScmToolsException {
        this(type, prop, nodeOeprators);
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
        
        if (nodeOeprators.getAllNode().containsKey(port)) {
            throw new ScmToolsException("The port is already occupied,port:" + port + ",conf path:"
                    + nodeOeprators.getNodeInfo(port)
                            .getConfPath(), ScmBaseExitCode.SCM_ALREADY_EXIST_ERROR);
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
        if (this.type.isNeedHystrixConf()) {
            appendHystrixConfig(modifier);
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

    private void appendHystrixConfig(Properties nodeConf) {
        if (nodeConf == null) {
            return;
        }
        Set<String> eurekaUrls = ScmCommon.getEurekaUrlsFromConfig(nodeConf);
        String rootSiteName = ScmCommon.getRootSiteFromEurekaUrls(eurekaUrls);
        if (rootSiteName != null) {
            BSONObject rootSiteHystrixConf = BsonUtils.getBSONChecked(this.hystrixConfig, "rootSite");
            for (String configKey : rootSiteHystrixConf.keySet()) {
                String configValue = (String) rootSiteHystrixConf.get(configKey);
                String realConfigKey = configKey.replace("$serverName", rootSiteName);
                if (!nodeConf.containsKey(realConfigKey)) {
                    nodeConf.put(realConfigKey, configValue);
                }
            }
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
