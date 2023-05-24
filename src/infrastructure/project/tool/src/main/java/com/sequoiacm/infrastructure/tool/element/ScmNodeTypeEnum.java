package com.sequoiacm.infrastructure.tool.element;

import com.sequoiacm.infrastructure.tool.common.ScmDeployPriority;

public enum ScmNodeTypeEnum {
    SERVICECENTER("service-center", "1", "sequoiacm-cloud-servicecenter-", ScmDeployPriority.SERVICE_CENTER
            .getDeployPriority()),
    GATEWAY("gateway", "2", "sequoiacm-cloud-gateway-", ScmDeployPriority.GATEWAY
            .getDeployPriority()),
    AUTHSERVER("auth-server", "3", "sequoiacm-cloud-authserver-", ScmDeployPriority.AUTH_SERVER
            .getDeployPriority()),
    SERVICETRACE("service-trace", "20", "sequoiacm-cloud-servicetrace-", ScmDeployPriority.SERVICE_TRACE
            .getDeployPriority()),
    ADMINSERVER("admin-server", "21", "sequoiacm-cloud-adminserver-", ScmDeployPriority.ADMIN_SERVER
            .getDeployPriority()),
    CONFIGSERVER("config-server", "1", "sequoiacm-config-server-", ScmDeployPriority.CONFIG_SERVER
            .getDeployPriority()),
    SCHEDULESERVER("schedule-server", "1", "sequoiacm-schedule-server-", ScmDeployPriority.SCHEDULE_SERVER
            .getDeployPriority()),
    FULLTEXTSERVER("fulltext-server", "1", "sequoiacm-fulltext-server-", ScmDeployPriority.FULLTEXT_SERVER
            .getDeployPriority()),
    MQSERVER("mq-server", "1", "sequoiacm-mq-server-", ScmDeployPriority.MQ_SERVER
            .getDeployPriority()),
    S3SERVER("s3-server", "1", "sequoiacm-s3-server-", ScmDeployPriority.S3_SERVER
            .getDeployPriority()),
    OMSERVER("om-server", "1", "sequoiacm-om-omserver-", ScmDeployPriority.OM_SERVER
            .getDeployPriority()),
    CONTENTSERVER("content-server", "1", "sequoiacm-content-server-", ScmDeployPriority.CONTENT_SERVER
            .getDeployPriority());

    private final String name;
    private final String typeNum;
    private final String jarNamePrefix;
    private final int deployPriority;

    ScmNodeTypeEnum(String name, String typeNum, String jarNamePrefix, int deployPriority) {
        this.name = name;
        this.typeNum = typeNum;
        this.jarNamePrefix = jarNamePrefix;
        this.deployPriority = deployPriority;
    }

    public String getName() {
        return name;
    }

    public String getTypeNum() {
        return typeNum;
    }

    public String getJarNamePrefix() {
        return jarNamePrefix;
    }

    public int getDeployPriority() {
        return deployPriority;
    }

    public static ScmNodeTypeEnum getScmNodeByName(String name) {
        for (ScmNodeTypeEnum nodeType : ScmNodeTypeEnum.values()) {
            if (nodeType.getName().equals(name)) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException(name + "not exit scm service");
    }

}
