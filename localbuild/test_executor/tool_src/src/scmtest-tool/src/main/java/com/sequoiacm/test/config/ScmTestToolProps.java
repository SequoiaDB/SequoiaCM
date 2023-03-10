package com.sequoiacm.test.config;

import com.sequoiacm.test.common.BashUtil;
import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.common.StringUtil;
import com.sequoiacm.test.common.CommonDefine;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.module.Worker;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ScmTestToolProps {

    private final static Logger logger = LoggerFactory.getLogger(ScmTestToolProps.class);

    private static volatile ScmTestToolProps INSTANCE;

    private String workPath;
    private Map<HostInfo, Integer> hostCounter = new HashMap<>();
    private LinkedList<Worker> workers = new LinkedList<>();
    private Map<String, String> testNgParameter = new HashMap<>();
    private int maxLenOfHostname;

    public static ScmTestToolProps getInstance() {
        if (INSTANCE == null) {
            synchronized (ScmTestToolProps.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScmTestToolProps();
                }
            }
        }
        return INSTANCE;
    }

    private ScmTestToolProps() {
        logger.info("Initializing the test configure...");
        initToolProps();
        CommonUtil.assertTrue(hostCounter.size() > 0, "Please configure the test workers");
        initWorkers();
        logger.info("Initialize the test configure success");
    }

    private void initWorkers() {
        Set<HostInfo> hosts = hostCounter.keySet();
        for (HostInfo host : hosts) {
            String currentHostWorkPath = workPath;
            String pathSeparator = CommonDefine.LINUX_PATH_SEPARATOR;
            if (host.isLocalHost() && BashUtil.isWindowsSystem()) {
                currentHostWorkPath = LocalPathConfig.EXEC_PATH;
                pathSeparator = File.separator;
            }
            currentHostWorkPath += "work-path";
            Integer hostCount = hostCounter.get(host);
            for (int i = 1; i <= hostCount; i++) {
                Worker worker = new Worker(host, i, currentHostWorkPath, pathSeparator);
                maxLenOfHostname = Math.max(maxLenOfHostname, worker.getName().length());
                // 若执行机包含本机，则将其“置顶”，保证串行用例可优先在本地执行
                if (host.isLocalHost() && i == 1) {
                    workers.addFirst(worker);
                }
                else {
                    workers.addLast(worker);
                }
            }
        }
    }

    private void initToolProps() {
        Properties props = new Properties();
        String filePath = LocalPathConfig.TOOL_CONF_PATH;
        if (filePath != null) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(filePath);
                props.load(is);
            }
            catch (Exception e) {
                throw new IllegalArgumentException(
                        "Failed to parse scmtest.properties file:" + filePath, e);
            }
            finally {
                CommonUtil.closeResource(is);
            }
        }

        Enumeration en = props.propertyNames();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            String value = props.getProperty(key);
            if (key.equals(CommonDefine.WORKERS)) {
                hostCounter.clear();
                List<String> hostList = StringUtil.string2List(value, ",");
                for (String host : hostList) {
                    HostInfo hostInfo = new HostInfo(host);
                    Integer countOfCurrentHost = hostCounter.getOrDefault(hostInfo, 0) + 1;
                    hostCounter.put(hostInfo, countOfCurrentHost);
                }
            }
            else if (key.equals(CommonDefine.WORK_PATH)) {
                if (!value.endsWith(CommonDefine.LINUX_PATH_SEPARATOR)) {
                    value += CommonDefine.LINUX_PATH_SEPARATOR;
                }
                this.workPath = value;
            }
            else if (key.startsWith(CommonDefine.PARAM_PREFIX)) {
                key = StringUtil.subStringAfter(key, CommonDefine.PARAM_PREFIX);
                this.testNgParameter.put(key, value);
            }
            else {
                throw new IllegalArgumentException(
                        "There are illegal parameters in the configuration file:" + filePath
                                + ", key:" + key);
            }
        }
    }

    public void check() {
        logger.info("Checking the configuration...");
        checkHostIsReachable();
        checkJavaHome();
        logger.info("Check the configuration success");
    }

    private void checkJavaHome() {
        Set<HostInfo> hosts = hostCounter.keySet();
        for (HostInfo host : hosts) {
            if (host.isLocalHost()) {
                continue;
            }

            Ssh ssh = null;
            try {
                ssh = SshMgr.getInstance().getSsh(host);
                String javaHome = ssh.searchEnv("JAVA_HOME");
                host.resetJavaHome(javaHome);
            }
            catch (IOException e) {
                throw new IllegalArgumentException("Failed to search JAVA_HOME, host ="
                        + host.getHostname() + ", cause by=" + e.getMessage(), e);
            }
            finally {
                CommonUtil.closeResource(ssh);
            }
        }
    }

    private void checkHostIsReachable() {
        Set<HostInfo> hosts = hostCounter.keySet();
        for (HostInfo host : hosts) {
            if (host.isLocalHost()) {
                continue;
            }

            Ssh ssh;
            try {
                ssh = SshMgr.getInstance().getSsh(host);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Host is unreachable:" + host.getHostname(), e);
            }

            try {
                try {
                    ssh.sudoExec("echo user_is_sudoer > /dev/null");
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("This user may not be sudoer:username="
                            + host.getUser() + ", host=" + host.getHostname(), e);
                }

                if (!ssh.isSftpAvailable()) {
                    throw new IllegalArgumentException(
                            "Sftp service may not be available, host=" + host.getHostname());
                }
            }
            finally {
                CommonUtil.closeResource(ssh);
            }
        }
    }

    public List<Worker> getWorkers() {
        return workers;
    }

    public int getMaxLenOfHostname() {
        return maxLenOfHostname;
    }

    public Map<String, String> getTestNgParameters() {
        return testNgParameter;
    }

}
