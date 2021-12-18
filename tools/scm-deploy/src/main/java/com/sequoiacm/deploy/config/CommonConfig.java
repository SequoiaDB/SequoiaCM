package com.sequoiacm.deploy.config;

import java.io.File;
import java.util.List;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.module.JavaVersion;
import com.sequoiacm.deploy.module.SiteInfo;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class CommonConfig {
    private String basePath = SystemApplicationProperty.getInstance().getString("common.basePath",
            ".") + File.separator;

    private String installPackPath = basePath + "package";
    private String tempPath = basePath + "sequoiacm-deploy" + File.separator + "temp";

    private String deployConfigFilePath = basePath + "sequoiacm-deploy" + File.separator + "conf"
            + File.separator + "deploy.cfg";

    private String workspaceConfigFilePath = basePath + "sequoiacm-deploy" + File.separator + "conf"
            + File.separator + "workspaces.json";

    private String sdbSystemTableConfFilePath = basePath + "sequoiacm-deploy" + File.separator
            + "bindata" + File.separator + "system.json";
    private String sdbAuditTableConfFilePath = basePath + "sequoiacm-deploy" + File.separator
            + "bindata" + File.separator + "audit.json";

    private String contentServerBaseDeployFilePath = basePath + "sequoiacm-deploy" + File.separator
            + "bindata" + File.separator + "contentserver_base.json";
    private String baseServiceTemplateFilePath = basePath + "sequoiacm-deploy" + File.separator
            + "bindata" + File.separator + "base_service_template.json";

    private String zkDeployScript = basePath + "sequoiacm-deploy" + File.separator + "bin"
            + File.separator + "deploy_zk.py";

    private String hystrixConfigPath = basePath + "sequoiacm-deploy" + File.separator + "bindata"
            + File.separator + "hystrix.json";

    private int waitServiceReadyTimeout = 3 * 60 * 1000;

    private String regionName = "DefaultRegion";
    private JavaVersion requireJavaVersion = new JavaVersion("1.8");
    private BSONObject hystrixConf;

    private static volatile CommonConfig instance;

    public static CommonConfig getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (CommonConfig.class) {
            if (instance != null) {
                return instance;
            }
            instance = new CommonConfig();
            return instance;
        }
    }

    private CommonConfig() {
        SystemApplicationProperty p = SystemApplicationProperty.getInstance();
        installPackPath = p.getString("common.installPackPath", installPackPath);
        baseServiceTemplateFilePath = p.getString("common.baseServiceTemplateFilePath",
                baseServiceTemplateFilePath);
        contentServerBaseDeployFilePath = p.getString("common.contentServerBaseDeployFilePath",
                contentServerBaseDeployFilePath);
        deployConfigFilePath = p.getString("common.deployConfigFilePath", deployConfigFilePath);
        regionName = p.getString("common.regionName", regionName);
        String requireJavaVersionStr = p.getString("common.requireJavaVersion",
                requireJavaVersion.toString());
        requireJavaVersion = new JavaVersion(requireJavaVersionStr);
        sdbAuditTableConfFilePath = p.getString("common.sdbAuditTableConfFilePath",
                sdbAuditTableConfFilePath);
        sdbSystemTableConfFilePath = p.getString("common.sdbSystemTableConfFilePath",
                sdbSystemTableConfFilePath);
        tempPath = p.getString("common.tempPath", tempPath);
        waitServiceReadyTimeout = p.getInt("common.waitServiceReadyTimeout",
                waitServiceReadyTimeout);
        workspaceConfigFilePath = p.getString("common.workspaceConfigFilePath",
                workspaceConfigFilePath);
        zkDeployScript = p.getString("common.zkDeployScript", zkDeployScript);
    }

    public String getBasePath() {
        return basePath;
    }

    public JavaVersion getRequireJavaVersion() {
        return requireJavaVersion;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getZkDeployScript() {
        return zkDeployScript;
    }

    public String getWorkspaceConfigFilePath() {
        return workspaceConfigFilePath;
    }

    public void setWorkspaceConfigFilePath(String workspaceConfigFilePath) {
        this.workspaceConfigFilePath = workspaceConfigFilePath;
    }

    public String getBaseServiceTemplateFilePath() {
        return baseServiceTemplateFilePath;
    }

    public void setBaseServiceTemplateFilePath(String baseServiceTemplateFilePath) {
        this.baseServiceTemplateFilePath = baseServiceTemplateFilePath;
    }

    public int getWaitServiceReadyTimeout() {
        return waitServiceReadyTimeout;
    }

    public void setWaitServiceReadyTimeout(int waitServiceReadyTimeout) {
        this.waitServiceReadyTimeout = waitServiceReadyTimeout;
    }

    public String getContentServerBaseDeployFilePath() {
        return contentServerBaseDeployFilePath;
    }

    public void setContentServerBaseDeployFilePath(String contentServerBaseDeployFilePath) {
        this.contentServerBaseDeployFilePath = contentServerBaseDeployFilePath;
    }

    public String getSdbAuditTableConfFilePath() {
        return sdbAuditTableConfFilePath;
    }

    public void setSdbAuditTableConfFilePath(String sdbAuditTableConfFilePath) {
        this.sdbAuditTableConfFilePath = sdbAuditTableConfFilePath;
    }

    public String getSdbSystemTableConfFilePath() {
        return sdbSystemTableConfFilePath;
    }

    public void setSdbSystemTableConfFilePath(String sdbSystemTableConfFilePath) {
        this.sdbSystemTableConfFilePath = sdbSystemTableConfFilePath;
    }

    public String getDeployConfigFilePath() {
        return deployConfigFilePath;
    }

    public void setDeployConfigFilePath(String deployConfigFilePath) {
        this.deployConfigFilePath = deployConfigFilePath;
    }

    public String getInstallPackPath() {
        return installPackPath;
    }

    public void setInstallPackPath(String installPackPath) {
        this.installPackPath = installPackPath;
    }

    public String getTempPath() {
        File file = new File(tempPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new IllegalArgumentException("failed to create tmp directory:" + tempPath);
            }
        }
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public String getHystrixConfigPath() {
        return hystrixConfigPath;
    }

    public void setHystrixConfigPath(String hystrixConfigPath) {
        this.hystrixConfigPath = hystrixConfigPath;
    }
}
