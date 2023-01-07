package com.sequoiacm.infrastructure.tool.operator;

import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.DefaultNodeOperator;

public class ScmServiceCenterNodeOperator extends DefaultNodeOperator {
    public ScmServiceCenterNodeOperator() throws ScmToolsException {
        super(new ScmNodeType(ScmNodeTypeEnum.SERVICECENTER, ScmServerScriptEnum.SERVICECENTER));
    }
}
