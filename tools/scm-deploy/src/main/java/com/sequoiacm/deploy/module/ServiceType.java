package com.sequoiacm.deploy.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum ServiceType {
    ZOOKEEPER("zookeeper", true, InstallPackType.ZOOKEEPER, -1),
    SERVICE_CENTER("service-center", true, InstallPackType.CLOUD, 0),
    AUTH_SERVER("auth-server", true, InstallPackType.CLOUD, 1),
    MQ_SERVER("mq-server", false, InstallPackType.MQ_SERVER, 1),
    CONFIG_SERVER("config-server", true, InstallPackType.CONFIG_SERVER, 2),
    SCHEDULE_SERVER("schedule-server", false, InstallPackType.SCHEDULE_SERVER, 3),
    GATEWAY("gateway", true, InstallPackType.CLOUD, 4),
    CONTENT_SERVER("content-server", true, InstallPackType.CONTENTSERVER, 5),
    ADMIN_SERVER("admin-server", false, InstallPackType.CLOUD, 10),
    // TRACE_SERVER("service-trace", false, InstallPackType.CLOUD, 10),
    OM_SERVER("om-server", false, InstallPackType.OM_SERVER, 10),
    FULLTEXT_SERVER("fulltext-server", false, InstallPackType.FULLTEXT_SERVER, 10),
    S3_SERVER("s3-server", false, InstallPackType.S3_SERVER, 10),
    // Daemon need to start last and clean first
    DAEMON("daemon", false, InstallPackType.DAEMON, 200);

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

    private ServiceType(String type, boolean isRequire, InstallPackType installPack,
            int deployPriority) {
        this.type = type;
        this.instllPack = installPack;
        this.priority = deployPriority;
        this.isRequire = isRequire;
    }

    public String getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public InstallPackType getInstllPack() {
        return instllPack;
    }

    public static ServiceType getType(String type) {
        for (ServiceType value : ServiceType.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

    public static List<ServiceType> getAllTyepSortByPriority() {
        return new ArrayList<>(typeSortByPriority);
    }

    public static List<ServiceType> getRequiredService() {
        return new ArrayList<>(requiredServiceType);
    }

    public boolean isRequire() {
        return isRequire;
    }
}
