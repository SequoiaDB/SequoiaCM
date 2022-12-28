package com.sequoiacm.deploy.module;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;
import org.bson.BSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConfigInfo {
    private List<ServiceType> services;
    private String upgradePackPath;
    private String backupPath;
    public static final ConfCoverter<ConfigInfo> CONVERTER = new ConfCoverter<ConfigInfo>() {
        @Override
        public ConfigInfo convert(BSONObject bson) {
            return new ConfigInfo(bson);
        }
    };

    public ConfigInfo(BSONObject bson) {
        String servicesStr = BsonUtils.getString(bson, ConfFileDefine.CONFIG_SERVICES);
        services = new ArrayList<>();
        if (servicesStr == null) {
            services.addAll(ServiceType.getAllTyepSortByPriority());
            services.remove(ServiceType.ZOOKEEPER);
        } else {
            for (String type : servicesStr.split(",")) {
                services.add(ServiceType.getTypeWithCheck(type));
            }
            services.sort(Comparator.comparing(ServiceType::getPriority));
        }
        upgradePackPath = BsonUtils.getString(bson, ConfFileDefine.CONFIG_UPGRADE_PACK_PATH);
        if (upgradePackPath == null || "".equals(upgradePackPath) || File.separator.equals(CommonUtils.removeRepeatFileSparator(upgradePackPath))) {
            upgradePackPath = "/opt/upgrade/sequoiacm";
        }
        backupPath = BsonUtils.getString(bson, ConfFileDefine.CONFIG_BACKUP_PATH);
        if (backupPath == null || "".equals(backupPath) || File.separator.equals(CommonUtils.removeRepeatFileSparator(backupPath))) {
            backupPath = "/opt/backup/sequoiacm";
        }
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
}
