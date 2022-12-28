package com.sequoiacm.deploy.core;

import com.sequoiacm.deploy.command.SubOption;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;
import com.sequoiacm.deploy.rollbacker.ServiceRollbackerMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ScmRollbacker {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeployer.class);
    private ScmUpgradeStatusInfoMgr upgradeStatusInfoMgr = ScmUpgradeStatusInfoMgr.getInstance();

    public void rollback(boolean dryrun) throws Exception {
        ServiceRollbackerMgr rollbackerMgr = ServiceRollbackerMgr.getInstance();
        // 1. --service指定某个服务,但该服务在upgrade_status中无记录,则需打印
        List<ServiceType> noExistsInStatusService = upgradeStatusInfoMgr
                .getNoExistsInStatusService();
        if (!noExistsInStatusService.isEmpty()) {
            logger.warn(
                    "The specified service does not exist in the upgrade status file,service={}",
                    noExistsInStatusService);
        }

        // 2. upgrade_status中记录着某个服务的记录，但是当前该主机不存在该服务,则需打印
        List<StatusInfo> noExistsServiceStatus = upgradeStatusInfoMgr.getNoExistsServiceStatus();
        if (!noExistsServiceStatus.isEmpty()) {
            logger.warn("The following service does not exist in the host");
            for (StatusInfo statusInfo : noExistsServiceStatus) {
                logger.warn("[" + statusInfo.getHostName() + "]:" + statusInfo.getType());
            }
        }
        // 3. 打印upgrade status服务当前版本与文件中记录的新版本不一致的记录
        List<StatusInfo> inConsistentVersionStatus = upgradeStatusInfoMgr
                .getInConsistentVersionStatus();
        if (!inConsistentVersionStatus.isEmpty()) {
            logger.warn(
                    "The following current version on the host not equal to the new version on upgrade status file");
            for (StatusInfo statusInfo : inConsistentVersionStatus) {
                logger.warn("[" + statusInfo.getHostName() + "]:" + statusInfo.getType() + ":"
                        + "currentVersion - " + statusInfo.getCurrentVersion() + ":" + "newVersion - "
                        + statusInfo.getNewVersion());
            }
        }

        // 4. 打印upgrade status备份不存在的记录
        List<StatusInfo> noExistBackupStatus = upgradeStatusInfoMgr.getNoExistBackupStatus();
        if (!noExistBackupStatus.isEmpty()) {
            logger.warn("The following backup of service does not exist on the host");
            for (StatusInfo statusInfo : noExistBackupStatus) {
                logger.warn("[" + statusInfo.getHostName() + "]:" + statusInfo.getType() + ":"
                        + statusInfo.getBackupPath());
            }
        }
        // 5. 判断是否有需要回滚的服务
        Map<ServiceType, List<StatusInfo>> availableServiceToStatus = upgradeStatusInfoMgr
                .getAvailableServiceToStatus();
        if (availableServiceToStatus.isEmpty()) {
            logger.warn("No service need to rollback");
            return;
        }

        // 6. 打印预备回滚的服务顺序
        logger.info("prepare to rollback services in this order");
        int progress = 0;
        List<ServiceType> serviceTypes = ServiceType.getAllTyepSortByPriority();
        for (int i = serviceTypes.size() - 1; i >= 0; i--) {
            List<StatusInfo> statusInfoList = availableServiceToStatus.get(serviceTypes.get(i));
            if (statusInfoList == null) {
                continue;
            }
            for (StatusInfo statusInfo : statusInfoList) {
                progress++;
                logger.info("[" + statusInfo.getHostName() + "]:" + statusInfo.getType() + ":"
                        + statusInfo.getNewVersion() + " to " + statusInfo.getOldVersion());
            }
        }

        // 7. 让用户确认是否回滚
        boolean isUnattended = CommonConfig.getInstance()
                .getSubOptionValue(SubOption.UNATTENDED) != null;
        if (!isUnattended && !CommonUtils.confirmExecute("rollback")) {
            return;
        }



        // 8. 开始回滚
        int currentProgress = 0;
        logger.info("Rollbacking service{}...({}/{})", dryrun ? "(Dry Run Mode)" : "",
                currentProgress++, progress);

        for (int i = serviceTypes.size() - 1; i >= 0; i--) {
            List<StatusInfo> statusInfoList = availableServiceToStatus.get(serviceTypes.get(i));
            if (statusInfoList == null) {
                continue;
            }
            for (StatusInfo statusInfo : statusInfoList) {
                logger.info("Rollbacking service: rollbacking {} on {} ({}/{})", statusInfo.getType(),
                        statusInfo.getHostName(), currentProgress++, progress);
                if (dryrun) {
                    continue;
                }
                rollbackerMgr.rollback(statusInfo);
            }
        }
        logger.info("Rollback success");
    }
}
