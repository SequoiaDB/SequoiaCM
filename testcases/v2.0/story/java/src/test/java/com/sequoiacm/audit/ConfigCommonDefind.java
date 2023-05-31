package com.sequoiacm.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fanyu on 2018/11/30.
 */
public class ConfigCommonDefind {
    // permission
    public static final String scm_audit_mask = "scm.audit.mask";
    public static final String scm_audit_userMask = "scm.audit.userMask";
    public static final String scm_audit_userType = "scm.audit.userType.";
    public static final String scm_audit_user = "scm.audit.user.";

    // permission
    public static class Permission {
        // the type of int
        public static final String ribbon_maxAutoRetries = "ribbon"
                + ".MaxAutoRetries";
        public static final String ribbon_maxAutoRetriesNextServer = "ribbon"
                + ".MaxAutoRetriesNextServer";
        public static final String ribbon_connectTimeout = "ribbon"
                + ".ConnectTimeout";
        public static final String ribbon_readTimeout = "ribbon.ReadTimeout";
        public static final String hystrix_timeoutInMillisecond = "hystrix.command.default.execution.isolation.thread"
                + ".timeoutInMilliseconds";
        // the type of boolean
        public static final String prefer_same_zone_eureka = "eureka.client"
                + ".prefer-same-zone-eureka";
        public static final String zipkin_enabled = "spring.zipkin.enabled";
        public static final String hystrix_timeout_enabled = "hystrix.command"
                + ".default.execution.timeout.enabled";
        public static final String ribbon_retry = "ribbon"
                + ".OkToRetryOnAllOperations";

        public static List< String > getPermitedList() {
            List< String > permitedList = new ArrayList<>();
            permitedList.add( ribbon_maxAutoRetries );
            permitedList.add( ribbon_maxAutoRetriesNextServer );
            permitedList.add( ribbon_connectTimeout );
            permitedList.add( ribbon_readTimeout );
            permitedList.add( hystrix_timeoutInMillisecond );
            permitedList.add( prefer_same_zone_eureka );
            permitedList.add( zipkin_enabled );
            permitedList.add( hystrix_timeout_enabled );
            permitedList.add( ribbon_retry );
            return permitedList;
        }
    }

    // forbidden
    public static class Forbidden {
        public static final String app_name = "spring.application.name";
        public static final String register_with_eureka = "eureka.client"
                + ".register-with-eureka";
        public static final String fetch_registry = "eureka.client"
                + ".fetch-registry";
        public static final String metadata_map_region = "eureka.instance"
                + ".metadata-map.region";
        public static final String client_region = "eureka.client.region";
        public static final String server_port = "server.port";

        public static List< String > getForbiddenList() {
            List< String > forbiddenList = new ArrayList<>();
            forbiddenList.add( app_name );
            forbiddenList.add( register_with_eureka );
            forbiddenList.add( fetch_registry );
            forbiddenList.add( metadata_map_region );
            forbiddenList.add( client_region );
            forbiddenList.add( server_port );
            return forbiddenList;
        }
    }

    public static class BucketQuota{
        public static final String scm_quota_lowWater_minObjects =  "scm.quota.lowWater.minObjects";
        public static final String scm_quota_lowWater_minSize =  "scm.quota.lowWater.minSize";
    }
}
