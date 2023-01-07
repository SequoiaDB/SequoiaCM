package com.sequoiacm.infrastructure.tool.operator;

import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.DefaultNodeOperator;

public class ScmS3NodeOperator extends DefaultNodeOperator {
    public ScmS3NodeOperator() throws ScmToolsException {
        super(new ScmNodeType(ScmNodeTypeEnum.S3SERVER, ScmServerScriptEnum.S3SERVER),
                "/internal/v1/health?action=actuator", "/health?action=actuator");
    }
}
