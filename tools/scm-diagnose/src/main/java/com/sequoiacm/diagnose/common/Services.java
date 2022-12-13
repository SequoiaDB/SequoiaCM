package com.sequoiacm.diagnose.common;

import com.sequoiacm.diagnose.execption.CollectException;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public enum Services {

    Daemon("daemon", "daemon/log", ""),
    AdminServer("admin-server", "sequoiacm-cloud/log/admin-server", ".*adminserver.*"),
    AuthServer("auth-server", "sequoiacm-cloud/log/auth-server", ".*authserver.*"),
    Gateway("gateway", "sequoiacm-cloud/log/gateway", ".*gateway.*"),
    ServiceCenter("service-center", "sequoiacm-cloud/log/service-center", ".*servicecenter.*"),
    ScheduleServer("schedule-server", "sequoiacm-schedule/log/schedule-server", ".*scheduleserver.*"),
    ConfigServer("config-server", "sequoiacm-config/log/config-server", ".*configserver.*"),
    ContentServer("content-server", "sequoiacm-content/log/content-server", ".*contentserver.*"),
    MqServer("mq-server", "sequoiacm-mq/log/mq-server", ".*mqserver.*"),
    FulltextServer("fulltext-server", "sequoiacm-fulltext/log/fulltext-server", ".*fulltextserver.*"),
    S3Server("s3-server", "sequoiacm-s3/log/s3-server", ".*s3server.*"),
    CloudDiskServer("cloud-disk-server", "sequoiacm-cloud-disk/log/cloud-disk-server", ".*clouddiskserver.*");

    private String serviceName;
    private String serviceInstallPath;

    private String match;

    Services(String serviceName, String serviceInstallPath, String match) {
        this.serviceName = serviceName;
        this.serviceInstallPath = serviceInstallPath;
        this.match = match;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceInstallPath() {
        return serviceInstallPath;
    }

    public String getMatch() {
        return match;
    }

    public static Services getServices(String servicesName) throws ScmToolsException {
        for (Services value : Services.values()) {
            if (value.getServiceName().equals(servicesName)) {
                return value;
            }
        }
        throw new ScmToolsException("scm not have this services,servicesName=" + servicesName,
                CollectException.INVALID_ARG);
    }

}
