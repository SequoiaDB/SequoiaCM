package com.sequoiacm.infrastructure.audit;

public class ScmAuditDefine {
    
    public final static String CS_AUDIT = "SCMAUDIT";
    public final static String CL_AUDIT = "AUDIT_LOG_EVENT";

    public static class AuditInfo {
        
        public static final String HOST                         = "host";
        public static final String PORT                         = "port";
        public static final String TYPE                         = "type";
        public static final String USER_TYPE                    = "user_type";
        public static final String USER_NAME                    = "user_name";
        public static final String WORK_SPACE                   = "work_space";
        public static final String FLAG                         = "flag";
        public static final String TIME                         = "time";
        public static final String THREAD                       = "thread";
        public static final String LEVEL                        = "level";
        public static final String MESSAGE                      = "message";
    }
    
    public static class ConnectionConf {
        public static final String CONNECTTIMEOUT               = "connectTimeout";
        public static final String MAXAUTOCONNECTRETRYTIME      = "maxAutoConnectRetryTime";
        public static final String SOCKETTIMEOUT                = "socketTimeout";
        public static final String USENAGLE                     = "useNagle";
        public static final String USESSL                       = "useSSL";
    }
    
    public static class DataSourceConf {
        public static final String MAXCONNECTIONNUM             = "maxConnectionNum";
        public static final String DELTAINCCOUNT                = "deltaIncCount";
        public static final String MAXIDLENUM                   = "maxIdleNum";
        public static final String KEEPALIVETIME                = "keepAliveTime";
        public static final String RECHECKCYCLEPERIOD           = "recheckCyclePeriod";
        public static final String VALIDATECONNECTION           = "validateConnection";
    }
    
    public static class AuditConf {
        public static final String AUDIT_MASK                         = "scm.audit.mask";
        public static final String AUDIT_USERMASK                     = "scm.audit.userMask";
        public static final String AUDIT_USER                         = "scm.audit.user";
        public static final String AUDIT_USERTYPE                     = "scm.audit.userType";
    }

    public static class CheckPoint {
        public static final int TIME                            = 1000*60*60;
    }
}
