package com.sequoiacm.deploy.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum ServiceType {
    ZOOKEEPER("zookeeper", true, InstallPackType.ZOOKEEPER, -1, null),
    SERVICE_CENTER("service-center", true, InstallPackType.CLOUD, "jars/sequoiacm-cloud-servicecenter-*.jar", 0, "scmcloudctl.sh"),
    SERVICE_TRACE("service-trace", false, InstallPackType.CLOUD, "jars/sequoiacm-cloud-servicetrace-*.jar", 1, "scmcloudctl.sh"),
    AUTH_SERVER("auth-server", true, InstallPackType.CLOUD, "jars/sequoiacm-cloud-authserver-*.jar", 2, "scmcloudctl.sh"),
    MQ_SERVER("mq-server", false, InstallPackType.MQ_SERVER, "jars/sequoiacm-mq-server-*.jar", 2, "mqctl.sh"),
    CONFIG_SERVER("config-server", true, InstallPackType.CONFIG_SERVER, "jars/sequoiacm-config-server-*.jar", 3, "confctl.sh"),
    SCHEDULE_SERVER("schedule-server", false, InstallPackType.SCHEDULE_SERVER, "jars/sequoiacm-schedule-server-*.jar", 3, "schctl.sh"),
    GATEWAY("gateway", true, InstallPackType.CLOUD, "jars/sequoiacm-cloud-gateway-*.jar", 4, "scmcloudctl.sh"),
    CONTENT_SERVER("content-server", true, InstallPackType.CONTENTSERVER, "lib/sequoiacm-content-server-*.jar", 5, "scmctl.sh"),
    ADMIN_SERVER("admin-server", false, InstallPackType.CLOUD, "jars/sequoiacm-cloud-adminserver-*.jar", 10, "scmcloudctl.sh"),
    OM_SERVER("om-server", false, InstallPackType.OM_SERVER, "jars/sequoiacm-om-omserver-*.jar", 10, "omctl.sh"),
    FULLTEXT_SERVER("fulltext-server", false, InstallPackType.FULLTEXT_SERVER, "jars/sequoiacm-fulltext-server-*.jar", 10, "ftctl.sh"),
    S3_SERVER("s3-server", false, InstallPackType.S3_SERVER, "jars/sequoiacm-s3-server-*.jar", 10, "s3ctl.sh"),
    // Daemon need to start last and clean first
    DAEMON("daemon", false, InstallPackType.DAEMON, "jars/sequoiacm-daemon-tools-*.jar", 200, "scmd.sh"),
    NON_SERVICE("non-service", false, InstallPackType.NON_SERVICE, "sequoiacm-driver-*.tar.gz", 300, null) {

        @Override
        public String toString() {
            return super.toString() + getInstllPack().getDirs();
        }
    };

    private static List<ServiceType> typeSortByPriority;
    private static List<ServiceType> requiredServiceType;
    static {
        typeSortByPriority = Arrays.asList(ServiceType.values());
        Collections.sort(typeSortByPriority, new Comparator<ServiceType>() {

            @Override
            public int compare(ServiceType o1, ServiceType o2) {
                return o1.getPriority() - o2.getPriority();
            }
        });

        requiredServiceType = new ArrayList<>();
        for (ServiceType type : ServiceType.values()) {
            if (type.isRequire) {
                requiredServiceType.add(type);
            }

        }
    }

    private String type;
    private InstallPackType instllPack;

    // deploy order
    private int priority;
    private boolean isRequire;
    private String jar;
    private String startStopScript;

    private ServiceType(String type, boolean isRequire, InstallPackType installPack,
                        int deployPriority, String startStopScript) {
        this.type = type;
        this.isRequire = isRequire;
        this.instllPack = installPack;
        this.priority = deployPriority;
        this.startStopScript = startStopScript;
    }

    private ServiceType(String type, boolean isRequire, InstallPackType installPack, String jar,
                        int deployPriority, String startStopScript) {
        this.type = type;
        this.isRequire = isRequire;
        this.instllPack = installPack;
        this.jar = jar;
        this.priority = deployPriority;
        this.startStopScript = startStopScript;
    }

    public String getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isRequire() {
        return isRequire;
    }

    public InstallPackType getInstllPack() {
        return instllPack;
    }

    public String getStartStopScript() {
        return startStopScript;
    }

    public static ServiceType getType(String type) {
        for (ServiceType value : ServiceType.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

    public static ServiceType getTypeWithCheck(String type) {
        ServiceType serviceType = getType(type);
        if (serviceType == null) {
            throw new IllegalArgumentException("unknown service type:" + type);
        }
        return serviceType;
    }

    public static List<ServiceType> getAllTyepSortByPriority() {
        return new ArrayList<>(typeSortByPriority);
    }

    public static List<ServiceType> getRequiredService() {
        return new ArrayList<>(requiredServiceType);
    }

    public static List<ServiceType> getServiceTypes(InstallPackType installPackType) {
        List<ServiceType> res = new ArrayList<>();
        for (ServiceType value : ServiceType.values()) {
            if (value.getInstllPack().equals(installPackType)) {
                res.add(value);
            }
        }
        return res;
    }

    public String getJarFileSuffix() {
        return instllPack.getUntarDirName() + "/" + jar;
    }

    public String getJar() {
        return jar;
    }
}
