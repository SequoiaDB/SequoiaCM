package com.sequoiacm.deploy.module;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;
import com.sequoiacm.deploy.parser.KeyValueConverter;
import org.bson.BSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ConfigInfo {
    private List<ServiceType> services;
    private String upgradePackPath;
    private String backupPath;
    private String scmUser;
    private String scmPassword;

    private String scmGateway;
    public static final KeyValueConverter<ConfigInfo> CONVERTER = new KeyValueConverter<ConfigInfo>() {
        @Override
        public ConfigInfo convert(Map<String, String> keyValue) {
            return new ConfigInfo(keyValue);
        }
    };

    public ConfigInfo(Map<String, String> keyValue) {
        String servicesStr = keyValue.get(ConfFileDefine.CONFIG_SERVICES);
        services = new ArrayList<>();
        if (servicesStr == null) {
            services.addAll(ServiceType.getAllTyepSortByPriority());
            services.remove(ServiceType.ZOOKEEPER);
        }
        else {
            for (String type : servicesStr.split(",")) {
                services.add(ServiceType.getTypeWithCheck(type));
            }
            services.sort(Comparator.comparing(ServiceType::getPriority));
        }
        upgradePackPath = keyValue.get(ConfFileDefine.CONFIG_UPGRADE_PACK_PATH);
        if (upgradePackPath == null || "".equals(upgradePackPath)
                || File.separator.equals(CommonUtils.removeRepeatFileSparator(upgradePackPath))) {
            upgradePackPath = "/opt/upgrade/sequoiacm";
        }
        backupPath = keyValue.get(ConfFileDefine.CONFIG_BACKUP_PATH);
        if (backupPath == null || "".equals(backupPath)
                || File.separator.equals(CommonUtils.removeRepeatFileSparator(backupPath))) {
            backupPath = "/opt/backup/sequoiacm";
        }

        scmUser = keyValue.get(ConfFileDefine.CONFIG_SCM_USER);
        scmPassword = keyValue.get(ConfFileDefine.CONFIG_SCM_PASSWORD);
        scmGateway = keyValue.get(ConfFileDefine.CONFIG_SCM_GATEWAY);
    }

    public List<ServiceType> getServices() {
        return services;
    }

    public void setServices(List<ServiceType> services) {
        this.services = services;
    }

    public String getUpgradePackPath() {
        return upgradePackPath;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public void setUpgradePackPath(String upgradePackPath) {
        this.upgradePackPath = upgradePackPath;
    }

    public String getScmPassword() {
        return scmPassword;
    }

    public String getScmUser() {
        return scmUser;
    }

    public String getScmGateway() {
        return scmGateway;
    }
}
