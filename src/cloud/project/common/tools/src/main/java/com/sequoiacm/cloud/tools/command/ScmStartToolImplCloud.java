package com.sequoiacm.cloud.tools.command;

import java.util.List;

import com.sequoiacm.infrastructure.tool.command.ScmStartToolImpl;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;

public class ScmStartToolImplCloud extends ScmStartToolImpl {

    public ScmStartToolImplCloud(List<ScmServiceNodeOperator> nodeOperatorList)
            throws ScmToolsException {
        super(nodeOperatorList);
        SLEEP_TIME = 1000;
    }

}
