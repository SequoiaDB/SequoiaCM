package com.sequoiacm.infrastructure.tool.element;

import com.sequoiacm.infrastructure.tool.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.util.ArrayList;

public class ScmNodeTypeList extends ArrayList<ScmNodeType> {

    public ScmNodeType getNodeTypeByStr(String str) throws ScmToolsException {
        for (ScmNodeType nodeType : this) {
            if (nodeType.getName().equals(str) || nodeType.getType().equals(str)) {
                return nodeType;
            }
        }

        throw new ScmToolsException("unknown type:" + str, ScmExitCode.INVALID_ARG);
    }
}
