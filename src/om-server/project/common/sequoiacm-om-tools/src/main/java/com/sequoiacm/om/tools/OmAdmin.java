package com.sequoiacm.om.tools;

import java.util.HashMap;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.*;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

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
            cmd.addTool(new ScmCreateNodeToolImpl(nodeProperties, nodeTypes));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
