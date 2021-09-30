package com.sequoiacm.cloud.tools;

import java.util.HashMap;

import com.sequoiacm.cloud.tools.command.ScmCleanSysTableToolImpl;
import com.sequoiacm.cloud.tools.command.ScmCreateNodeToolImplCloud;
import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("scmcloudadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        nodeTypes.add(new ScmNodeType("1", "service-center", "sequoiacm-cloud-servicecenter-", ScmServerScriptEnum.SERVICECENTER));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup1 = ScmNodeRequiredParamGroup.newBuilder()
                .addServerPortParam(8800).addCloudParam().get();
        nodeProperties.put("1", scmNodeRequiredParamGroup1);

        nodeTypes.add(new ScmNodeType("2", "gateway", "sequoiacm-cloud-gateway-",ScmServerScriptEnum.GATEWAY));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup2 = ScmNodeRequiredParamGroup.newBuilder()
                .addServerPortParam(8080).addCloudParam().get();
        nodeProperties.put("2", scmNodeRequiredParamGroup2);

        nodeTypes.add(new ScmNodeType("3", "auth-server", "sequoiacm-cloud-authserver-",ScmServerScriptEnum.AUTHSERVER));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup3 = ScmNodeRequiredParamGroup.newBuilder()
                .addServerPortParam(8810).addCloudParam().addSdbParam().get();
        nodeProperties.put("3", scmNodeRequiredParamGroup3);

        nodeTypes.add(new ScmNodeType("20", "service-trace", "sequoiacm-cloud-servicetrace-",ScmServerScriptEnum.SERVICETRACE));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup4 = ScmNodeRequiredParamGroup.newBuilder()
                .addServerPortParam(8890).addCloudParam().get();
        nodeProperties.put("20", scmNodeRequiredParamGroup4);

        nodeTypes.add(new ScmNodeType("21", "admin-server", "sequoiacm-cloud-adminserver-",ScmServerScriptEnum.ADMINSERVER));
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup5 = ScmNodeRequiredParamGroup.newBuilder()
                .addServerPortParam(8900).addCloudParam().addSdbParam().addZkParam().get();
        nodeProperties.put("21", scmNodeRequiredParamGroup5);

        try {
            cmd.addTool(new ScmCreateNodeToolImplCloud(nodeProperties, nodeTypes));
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
