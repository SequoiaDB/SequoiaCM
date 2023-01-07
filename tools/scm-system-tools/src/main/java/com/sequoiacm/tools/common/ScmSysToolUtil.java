package com.sequoiacm.tools.common;

import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScmSysToolUtil {
    public static Map<ScmNodeType, ScmServiceNodeOperator> initOperators(ScmNodeTypeList allNodeTypes, String installPath) throws ScmToolsException {
        Map<ScmNodeType, ScmServiceNodeOperator> allOperators = new HashMap<>();
        List<ScmServiceNodeOperator> operators = RefUtil
                .initInstancesImplWith(ScmServiceNodeOperator.class);
        for (ScmServiceNodeOperator operator : operators) {
            operator.init(installPath);
            allOperators.put(operator.getNodeType(), operator);
        }

        for (ScmNodeType nodeType : allNodeTypes) {
            if(allOperators.get(nodeType) == null){
                throw new ScmToolsException("Operator for " + nodeType.getName() + " not found",
                        ScmBaseExitCode.SYSTEM_ERROR);
            }
        }

        return allOperators;
    }

    public static ScmNodeTypeList getAllNodeTypes() {
        ScmNodeTypeList allNodeTypes = new ScmNodeTypeList();
        for (ScmNodeTypeEnum type : ScmNodeTypeEnum.values()) {
            allNodeTypes
                    .add(new ScmNodeType(type, ScmServerScriptEnum.getEnumByType(type.getName())));
        }
        return allNodeTypes;
    }
}
