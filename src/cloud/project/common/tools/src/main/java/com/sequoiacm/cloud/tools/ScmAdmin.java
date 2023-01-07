package com.sequoiacm.cloud.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sequoiacm.cloud.tools.command.ScmCleanSysTableToolImpl;
import com.sequoiacm.cloud.tools.command.ScmCreateNodeToolImplCloud;
import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmAdminNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmAuthNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmGatewayNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceCenterNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmTraceNodeOperator;

public class ScmAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("scmcloudadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.SERVICECENTER,
                ScmServerScriptEnum.SERVICECENTER, false));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup1 = ScmNodeRequiredParamGroup
                .newBuilder().addServerPortParam(8800).addSdbParam().addCloudParam().get();
        nodeProperties.put(ScmNodeTypeEnum.SERVICECENTER.getTypeNum(), scmNodeRequiredParamGroup1);

        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.GATEWAY, ScmServerScriptEnum.GATEWAY));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup2 = ScmNodeRequiredParamGroup
                .newBuilder().addServerPortParam(8080).addCloudParam().get();
        nodeProperties.put(ScmNodeTypeEnum.GATEWAY.getTypeNum(), scmNodeRequiredParamGroup2);

        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.AUTHSERVER, ScmServerScriptEnum.AUTHSERVER));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup3 = ScmNodeRequiredParamGroup
                .newBuilder().addServerPortParam(8810).addCloudParam().addSdbParam().get();
        nodeProperties.put(ScmNodeTypeEnum.AUTHSERVER.getTypeNum(), scmNodeRequiredParamGroup3);

        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.SERVICETRACE,
                ScmServerScriptEnum.SERVICETRACE, false));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup4 = ScmNodeRequiredParamGroup
                .newBuilder().addServerPortParam(8890).addCloudParam().get();
        nodeProperties.put(ScmNodeTypeEnum.SERVICETRACE.getTypeNum(), scmNodeRequiredParamGroup4);

        nodeTypes
                .add(new ScmNodeType(ScmNodeTypeEnum.ADMINSERVER, ScmServerScriptEnum.ADMINSERVER));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup5 = ScmNodeRequiredParamGroup
                .newBuilder().addServerPortParam(8900).addCloudParam().addSdbParam().addZkParam()
                .get();
        nodeProperties.put(ScmNodeTypeEnum.ADMINSERVER.getTypeNum(), scmNodeRequiredParamGroup5);

        try {
            List<ScmServiceNodeOperator> opList = Arrays.<ScmServiceNodeOperator> asList(
                    new ScmServiceCenterNodeOperator(), new ScmGatewayNodeOperator(),
                    new ScmAuthNodeOperator(), new ScmAdminNodeOperator(),
                    new ScmTraceNodeOperator());
            cmd.addTool(new ScmCreateNodeToolImplCloud(nodeProperties, opList));
            cmd.addTool(new ScmCleanSysTableToolImpl(nodeTypes));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
