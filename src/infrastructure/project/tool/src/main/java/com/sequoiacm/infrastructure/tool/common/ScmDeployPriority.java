package com.sequoiacm.infrastructure.tool.common;

public enum ScmDeployPriority {
    ZOOKEEPER("zookeeper", -1),
    SERVICE_CENTER("service-center", 0),
    SERVICE_TRACE("service-trace", 1),
    AUTH_SERVER("auth-server", 2),
    MQ_SERVER("mq-server", 2),
    CONFIG_SERVER("config-server", 3),
    SCHEDULE_SERVER("schedule-server", 3),
    GATEWAY("gateway", 4),
    CONTENT_SERVER("content-server", 5),
    ADMIN_SERVER("admin-server", 10),
    OM_SERVER("om-server", 10),
    FULLTEXT_SERVER("fulltext-server", 10),
    S3_SERVER("s3-server", 10),
    SCMSYSTOOLS("scmsystools", 100),
    DAEMON("daemon", 200),
    NON_SERVICE("non-service", 300);

    private final String name;
    private final int deployPriority;

    ScmDeployPriority(String name, int deployPriority) {
        this.name = name;
        this.deployPriority = deployPriority;
    }

    public int getDeployPriority() {
        return deployPriority;
    }

}
