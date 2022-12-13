package com.sequoiacm.infrastructure.tool.element;

public enum ScmNodeTypeEnum {
    SERVICECENTER("service-center", "1", "sequoiacm-cloud-servicecenter-"),
    GATEWAY("gateway", "2", "sequoiacm-cloud-gateway-"),
    AUTHSERVER("auth-server", "3", "sequoiacm-cloud-authserver-"),
    SERVICETRACE("service-trace", "20", "sequoiacm-cloud-servicetrace-"),
    ADMINSERVER("admin-server", "21", "sequoiacm-cloud-adminserver-"),
    CONFIGSERVER("config-server", "1", "sequoiacm-config-server-"),
    SCHEDULESERVER("schedule-server", "1", "sequoiacm-schedule-server-"),
    FULLTEXTSERVER("fulltext-server", "1", "sequoiacm-fulltext-server-"),
    MQSERVER("mq-server", "1", "sequoiacm-mq-server-"),
    S3SERVER("s3-server", "1", "sequoiacm-s3-server-"),
    OMSERVER("om-server", "1", "sequoiacm-om-omserver-"),
    CONTENTSERVER("content-server", "1", "sequoiacm-content-server-");

    private final String name;
    private final String typeNum;
    private final String jarNamePrefix;

    ScmNodeTypeEnum(String name, String typeNum, String jarNamePrefix) {
        this.name = name;
        this.typeNum = typeNum;
        this.jarNamePrefix = jarNamePrefix;
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

    public static ScmNodeTypeEnum getScmNodeByName(String name) {
        for (ScmNodeTypeEnum nodeType : ScmNodeTypeEnum.values()) {
            if (nodeType.getName().equals(name)) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException(name + "not exit scm service");
    }

}
