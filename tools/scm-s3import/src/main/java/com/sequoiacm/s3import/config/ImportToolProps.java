package com.sequoiacm.s3import.config;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonUtils;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.util.*;

import static com.sequoiacm.s3import.common.CommonDefine.Prop.*;

public class ImportToolProps {

    private static volatile ImportToolProps instance;

    private S3ServerInfo srcS3 = new S3ServerInfo();
    private S3ServerInfo destS3 = new S3ServerInfo();
    private int batchSize = 500;
    private int maxFailCount = 100;
    private int workCount = 50;
    private boolean strictComparisonMode;

    public static ImportToolProps getInstance() throws ScmToolsException {
        if (instance == null) {
            synchronized (ImportToolProps.class) {
                if (instance == null) {
                    instance = new ImportToolProps();
                }
            }
        }
        return instance;
    }

    private ImportToolProps() throws ScmToolsException {
        intiToolProps();
        resetS3ConnConf();
    }

    private void resetS3ConnConf() {
        S3ConnectionConf s3ConnectConf = srcS3.getS3ConnectConf();
        if (s3ConnectConf.getMaxConnection() < this.workCount) {
            s3ConnectConf.setMaxConnection(this.workCount);
        }
        s3ConnectConf = destS3.getS3ConnectConf();
        if (s3ConnectConf.getMaxConnection() < this.workCount) {
            s3ConnectConf.setMaxConnection(this.workCount);
        }
    }

    private void intiToolProps() throws ScmToolsException {
        Properties props = new Properties();
        String confFilePath = ImportPathConfig.getInstance().getConfPath();
        if (confFilePath != null) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(confFilePath);
                props.load(is);
            }
            catch (Exception e) {
                throw new ScmToolsException("Failed to parse configuration file:" + confFilePath,
                        S3ImportExitCode.INVALID_ARG, e);
            }
            finally {
                CommonUtils.closeResource(is);
            }
        }

        Enumeration en = props.propertyNames();
        while (en.hasMoreElements()) {
            boolean isIllegalKey = false;
            String key = (String) en.nextElement();
            String value = props.getProperty(key);
            if (key.startsWith(SRC_S3_PREFIX) || key.startsWith(DEST_S3_PREFIX)) {
                S3ServerInfo s3Server;
                String subKey;
                if (key.startsWith(SRC_S3_PREFIX)) {
                    subKey = key.substring(SRC_S3_PREFIX.length());
                    s3Server = srcS3;
                }
                else {
                    subKey = key.substring(DEST_S3_PREFIX.length());
                    s3Server = destS3;
                }

                // 客户端连接配置项 src.s3.client.xxx
                if (subKey.startsWith(CONNECT_CONF_PREFIX)) {
                    subKey = subKey.substring(CONNECT_CONF_PREFIX.length());
                    if (!s3Server.getS3ConnectConf().addConnectConf(subKey, value)) {
                        isIllegalKey = true;
                    }
                }
                else {
                    switch (subKey) {
                        case URL:
                            s3Server.setUrl(value);
                            break;
                        case ACCESS_KEY:
                            s3Server.setAccessKey(value);
                            break;
                        case SECRET_KEY:
                            s3Server.setSecretKey(value);
                            break;
                        case KEY_FILE:
                            s3Server.setKeyFilePath(value);
                            break;
                        default:
                            isIllegalKey = true;
                            break;
                    }
                }
            }
            else if (key.equals(BATCH_SITE)) {
                this.batchSize = Integer.parseInt(value);
            }
            else if (key.equals(MAX_FAIL_COUNT)) {
                this.maxFailCount = Integer.parseInt(value);
            }
            else if (key.equals(WORK_COUNT)) {
                this.workCount = Integer.parseInt(value);
            }
            else if (key.equals(STRICT_COMPARISON_MODE)) {
                this.strictComparisonMode = Boolean.parseBoolean(value);
            }
            else {
                isIllegalKey = true;
            }
            if (isIllegalKey) {
                throw new IllegalArgumentException(
                        "There are illegal parameters in the configuration file:" + confFilePath
                                + ", key:" + key);
            }
        }

        checkProp();
        parseKeyFile(srcS3);
        parseKeyFile(destS3);
    }

    private void checkProp() throws ScmToolsException {
        CommonUtils.assertTrue(batchSize >= 1 && batchSize <= 10000, BATCH_SITE
                        + " must be greater than or equals to 1 and less than or equal to 10000: "
                        + batchSize);
        CommonUtils.assertTrue(maxFailCount >= 1,
                MAX_FAIL_COUNT + " must be greater than or equals to 1: " + maxFailCount);
        CommonUtils.assertTrue(workCount >= 1 && workCount <= 100, WORK_COUNT
                        + " must be greater than or equals to 1 and less than or equal to 100: "
                        + workCount);
    }

    public void parseKeyFile(S3ServerInfo s3ServerInfo) {
        // 优先使用密码文件中的密码
        if (!StringUtils.isEmpty(s3ServerInfo.getKeyFilePath())) {
            AuthInfo authInfo = ScmFilePasswordParser.parserFile(s3ServerInfo.getKeyFilePath());
            s3ServerInfo.setAccessKey(authInfo.getUserName());
            s3ServerInfo.setSecretKey(authInfo.getPassword());
        }
    }

    public S3ServerInfo getSrcS3() {
        return srcS3;
    }

    public S3ServerInfo getDestS3() {
        return destS3;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxFailCount() {
        return maxFailCount;
    }

    public int getWorkCount() {
        return workCount;
    }

    public boolean isStrictComparisonMode() {
        return strictComparisonMode;
    }
}
