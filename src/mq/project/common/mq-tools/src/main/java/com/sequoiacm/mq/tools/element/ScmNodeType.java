package com.sequoiacm.mq.tools.element;

import com.sequoiacm.mq.tools.common.ScmToolsDefine;
import com.sequoiacm.mq.tools.exception.ScmExitCode;
import com.sequoiacm.mq.tools.exception.ScmToolsException;

public enum ScmNodeType {
    MQ_SERVER(ScmToolsDefine.NODE_TYPE.MQ_SERVER_NUM, ScmToolsDefine.FILE_NAME.MQ_SERVER, ScmToolsDefine.FILE_NAME.MQ_SERVER_JAR_NAME_PREFIX);

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
            case ScmToolsDefine.NODE_TYPE.MQ_SERVER_NUM:
            case ScmToolsDefine.NODE_TYPE.MQ_SERVER_STR:
                return MQ_SERVER;

            default:
                throw new ScmToolsException("unknown type:" + str, ScmExitCode.INVALID_ARG);
        }
    }
}
