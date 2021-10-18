package com.sequoiacm.test.config;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.common.StringUtil;
import com.sequoiacm.test.common.CommonDefine;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ScmTestToolProps {

    private final static Logger logger = LoggerFactory.getLogger(ScmTestToolProps.class);

    private static volatile ScmTestToolProps INSTANCE;

    private String workPath;
    private LinkedList<HostInfo> hostInfoList = new LinkedList<>();
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
        intiToolProps();
        CommonUtil.assertTrue(hostInfoList.size() > 0, "Please configure the test workers");
        // 若执行机包含本机，则将其“置顶”，保证串行用例可优先在本地执行
        modifyHostOrder();
        logger.info("Initialize the test configure success");
    }

    private void modifyHostOrder() {
        for (int i = 0; i < hostInfoList.size(); i++) {
            if (hostInfoList.get(i).getHostname().equals(CommonDefine.LOCALHOST)) {
                HostInfo localhost = hostInfoList.get(i);
                hostInfoList.remove(i);
                hostInfoList.addFirst(localhost);
                break;
            }
        }
    }

    private void intiToolProps() {
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
                List<String> hostList = StringUtil.string2List(value, ",");
                for (String host : hostList) {
                    HostInfo hostInfo = new HostInfo(host);
                    maxLenOfHostname = Math.max(maxLenOfHostname, hostInfo.getHostname().length());
                    hostInfoList.add(hostInfo);
                }
            }
            else if (key.equals(CommonDefine.WORK_PATH)) {
                if (!value.endsWith("/")) {
                    value += "/";
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
        for (HostInfo hostInfo : hostInfoList) {
            if (hostInfo.isLocalHost()) {
                continue;
            }

            Ssh ssh = null;
            try {
                ssh = SshMgr.getInstance().getSsh(hostInfo);
                String javaHome = ssh.searchEnv("JAVA_HOME");
                hostInfo.resetJavaHome(javaHome);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to search JAVA_HOME, host ="
                        + hostInfo.getHostname() + ", cause by=" + e.getMessage(), e);
            } finally {
                CommonUtil.closeResource(ssh);
            }
        }
    }

    private void checkHostIsReachable() {
        for (HostInfo hostInfo : hostInfoList) {
            if (hostInfo.isLocalHost()) {
                continue;
            }

            Ssh ssh;
            try {
                ssh = SshMgr.getInstance().getSsh(hostInfo);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Host is unreachable:" + hostInfo.getHostname(),
                        e);
            }

            try {
                try {
                    ssh.sudoExec("echo user_is_sudoer > /dev/null");
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("This user may not be sudoer:username="
                            + hostInfo.getUser() + ", host=" + hostInfo.getHostname(), e);
                }

                if (!ssh.isSftpAvailable()) {
                    throw new IllegalArgumentException(
                            "Sftp service may not be available, host=" + hostInfo.getHostname());
                }
            }
            finally {
                CommonUtil.closeResource(ssh);
            }
        }
    }

    public List<HostInfo> getWorkers() {
        return hostInfoList;
    }

    public int getMaxLenOfHostname() {
        return maxLenOfHostname;
    }

    public String getWorkPath() {
        return workPath;
    }

    public Map<String, String> getTestNgParameters() {
        return testNgParameter;
    }

}
