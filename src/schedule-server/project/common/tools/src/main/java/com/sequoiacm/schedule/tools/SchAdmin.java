package com.sequoiacm.schedule.tools;

import java.util.HashMap;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.*;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.schedule.tools.command.ScmCreateNodeToolImplSchedule;

public class SchAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("schadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.SCHEDULESERVER,
                ScmServerScriptEnum.SCHEDULESERVER));

        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addServerPortParam(8180).addCloudParam().addZkParam().get();

        nodeProperties.put(ScmNodeTypeEnum.SCHEDULESERVER.getTypeNum(), scmNodeRequiredParamGroup);
        try {
            cmd.addTool(new ScmCreateNodeToolImplSchedule(nodeProperties, nodeTypes));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);

        cmd.execute(args);
    }
}