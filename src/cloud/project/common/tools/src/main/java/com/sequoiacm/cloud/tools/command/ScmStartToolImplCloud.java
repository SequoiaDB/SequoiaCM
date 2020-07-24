package com.sequoiacm.cloud.tools.command;

import java.util.List;
import com.sequoiacm.infrastructure.tool.command.ScmStartToolImpl;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmStartToolImplCloud extends ScmStartToolImpl {

    public ScmStartToolImplCloud(ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super(nodeTypes);
        SLEEP_TIME = 1000;
    }

}
