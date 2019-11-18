package com.sequoiacm.schedule.tools.element;

import com.sequoiacm.schedule.tools.common.ScmToolsDefine;
import com.sequoiacm.schedule.tools.exception.ScmExitCode;
import com.sequoiacm.schedule.tools.exception.ScmToolsException;

public enum ScmNodeType {
    SCHEDULE_SERVER(ScmToolsDefine.NODE_TYPE.SCHEDULE_SERVER_NUM, ScmToolsDefine.FILE_NAME.SCHEDULE_SEVER, ScmToolsDefine.FILE_NAME.SCHEDULE_SERVER_JAR_NAME_PREFIX);

    private String type;
    private String name;
    private String jarNamePrefix;

    ScmNodeType(String type, String name, String jarNamePrefix) {
        this.type = type;
        this.name = name;
        this.jarNamePrefix = jarNamePrefix;
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
            case ScmToolsDefine.NODE_TYPE.SCHEDULE_SERVER_NUM:
            case ScmToolsDefine.NODE_TYPE.SCHEDULE_SERVER_STR:
                return SCHEDULE_SERVER;

            default:
                throw new ScmToolsException("unknown type:" + str, ScmExitCode.INVALID_ARG);
        }
    }
}
