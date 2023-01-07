package com.sequoiacm.om.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParam;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmOmNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;

public class OmAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("omadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(
                new ScmNodeType(ScmNodeTypeEnum.OMSERVER, ScmServerScriptEnum.OMSERVER, false));

        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addServerPortParam(8081)
                .addParam(ScmNodeRequiredParam.keyParamInstance("scm.omserver.gateway",
                        "-Dscm.omserver.gateway=localhost:8080"))
                .get();

        nodeProperties.put(ScmNodeTypeEnum.OMSERVER.getTypeNum(), scmNodeRequiredParamGroup);
        try {
            List<ScmServiceNodeOperator> opList = Collections
                    .<ScmServiceNodeOperator> singletonList(new ScmOmNodeOperator());
            cmd.addTool(new ScmCreateNodeToolImpl(nodeProperties, opList));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
