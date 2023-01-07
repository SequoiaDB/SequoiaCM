package com.sequoiacm.schedule.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmSchNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
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
            List<ScmServiceNodeOperator> opList = Collections
                    .<ScmServiceNodeOperator> singletonList(new ScmSchNodeOperator());
            cmd.addTool(new ScmCreateNodeToolImplSchedule(nodeProperties, opList));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);

        cmd.execute(args);
    }
}