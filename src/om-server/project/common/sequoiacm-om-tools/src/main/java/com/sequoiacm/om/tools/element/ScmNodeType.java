package com.sequoiacm.om.tools.element;

import com.sequoiacm.om.tools.common.ScmToolsDefine;
import com.sequoiacm.om.tools.exception.ScmExitCode;
import com.sequoiacm.om.tools.exception.ScmToolsException;

public enum ScmNodeType {
    OM_SERVER(ScmToolsDefine.NODE_TYPE.OM_SERVER_NUM, ScmToolsDefine.FILE_NAME.OM_SERVER, ScmToolsDefine.FILE_NAME.OM_SERVER_JAR_NAME_PREFIX);

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
            case ScmToolsDefine.NODE_TYPE.OM_SERVER_NUM:
            case ScmToolsDefine.NODE_TYPE.OM_SERVER_STR:
                return OM_SERVER;

            default:
                throw new ScmToolsException("unknown type:" + str, ScmExitCode.INVALID_ARG);
        }
    }
}
