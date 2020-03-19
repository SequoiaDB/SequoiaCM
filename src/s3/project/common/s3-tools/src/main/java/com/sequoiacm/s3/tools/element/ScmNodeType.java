package com.sequoiacm.s3.tools.element;

import com.sequoiacm.s3.tools.common.ScmToolsDefine;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import com.sequoiacm.s3.tools.exception.ScmToolsException;

public enum ScmNodeType {
    S3_SERVER(ScmToolsDefine.NODE_TYPE.S3_SERVER_NUM, ScmToolsDefine.FILE_NAME.S3_SERVER, ScmToolsDefine.FILE_NAME.S3_SERVER_JAR_NAME_PREFIX);

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
            case ScmToolsDefine.NODE_TYPE.S3_SERVER_NUM:
            case ScmToolsDefine.NODE_TYPE.S3_SERVER_STR:
                return S3_SERVER;

            default:
                throw new ScmToolsException("unknown type:" + str, ScmExitCode.INVALID_ARG);
        }
    }
}
