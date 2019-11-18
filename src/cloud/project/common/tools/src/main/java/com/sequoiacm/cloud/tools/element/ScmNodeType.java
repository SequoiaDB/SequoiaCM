package com.sequoiacm.cloud.tools.element;

import com.sequoiacm.cloud.tools.common.ScmToolsDefine;
import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.cloud.tools.exception.ScmToolsException;

public enum ScmNodeType {
    SERVICE_CENTER(
            ScmToolsDefine.NODE_TYPE.SERVICE_CENTER_NUM,
            ScmToolsDefine.FILE_NAME.SERVICE_CENTER,
            ScmToolsDefine.FILE_NAME.SERVICE_CENTER_JAR_NAME_PREFIX
            ),

    GATEWAY(
            ScmToolsDefine.NODE_TYPE.GATEWAY_NUM,
            ScmToolsDefine.FILE_NAME.GATEWAY,
            ScmToolsDefine.FILE_NAME.GATEWAY_JAR_NAME_PREFIX
            ),

    AUTH_SERVER(
            ScmToolsDefine.NODE_TYPE.AUTH_SERVER_NUM,
            ScmToolsDefine.FILE_NAME.AUTH_SERVER,
            ScmToolsDefine.FILE_NAME.AUTH_SERVER_JAR_NAME_PREFIX, ScmToolsDefine.FILE_NAME.AUTH_SERVER),

    SERVICE_TRACE(
            ScmToolsDefine.NODE_TYPE.SERVICE_TRACE_NUM,
            ScmToolsDefine.FILE_NAME.SERVICE_TRACE,
            ScmToolsDefine.FILE_NAME.SERVICE_TRACE_JAR_NAME_PREFIX
            ),

    ADMIN_SERVER(
            ScmToolsDefine.NODE_TYPE.ADMIN_SERVER_NUM,
            ScmToolsDefine.FILE_NAME.ADMIN_SERVER,
            ScmToolsDefine.FILE_NAME.ADMIN_SERVER_JAR_NAME_PREFIX, ScmToolsDefine.FILE_NAME.ADMIN_SERVER);

    private String type;
    private String name;
    private String jarNamePrefix;
    private String confTemplateNamePrefix = "spring-app";

    ScmNodeType(String type, String name, String jarNamePrefix) {
        this.type = type;
        this.name = name;
        this.jarNamePrefix = jarNamePrefix;
    }

    private ScmNodeType(String type, String name, String jarNamePrefix,
            String confTemplateNamePrefix) {
        this(type, name, jarNamePrefix);
        this.confTemplateNamePrefix = confTemplateNamePrefix;
    }

    public String getConfTemplateNamePrefix() {
        return confTemplateNamePrefix;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getJarNamePrefix() {
        return jarNamePrefix;
    }

    @Override
    public String toString() {
        return super.toString().replace("_", "-");
    }

    public static ScmNodeType getNodeTypeByStr(String str) throws ScmToolsException {
        switch (str) {
            case ScmToolsDefine.NODE_TYPE.SERVICE_CENTER_NUM:
            case ScmToolsDefine.NODE_TYPE.SERVICE_CENTER_STR:
                return SERVICE_CENTER;

            case ScmToolsDefine.NODE_TYPE.GATEWAY_NUM:
            case ScmToolsDefine.NODE_TYPE.GATEWAY_STR:
                return GATEWAY;

            case ScmToolsDefine.NODE_TYPE.AUTH_SERVER_NUM:
            case ScmToolsDefine.NODE_TYPE.AUTH_SERVER_STR:
                return AUTH_SERVER;



            case ScmToolsDefine.NODE_TYPE.SERVICE_TRACE_NUM:
            case ScmToolsDefine.NODE_TYPE.SERVICE_TRACE_STR:
                return SERVICE_TRACE;

            case ScmToolsDefine.NODE_TYPE.ADMIN_SERVER_NUM:
            case ScmToolsDefine.NODE_TYPE.ADMIN_SERVER_STR:
                return ADMIN_SERVER;

            default:
                throw new ScmToolsException("unknown type:" + str, ScmExitCode.INVALID_ARG);
        }
    }
}
