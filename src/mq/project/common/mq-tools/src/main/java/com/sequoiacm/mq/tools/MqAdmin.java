package com.sequoiacm.mq.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.mq.tools.command.ScmCreateGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmCreateTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmDeleteGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmDeleteTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmListGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmListTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmUpdateTopicToolImpl;
import com.sequoiacm.infrastructure.tool.operator.ScmMqNodeOperator;

public class MqAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("mqadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.MQSERVER, ScmServerScriptEnum.MQSERVER));

        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addCloudParam().addSdbParam().addServerPortParam(8610).get();

        nodeProperties.put(ScmNodeTypeEnum.MQSERVER.getTypeNum(), scmNodeRequiredParamGroup);
        try {
            List<ScmServiceNodeOperator> opList = Collections
                    .<ScmServiceNodeOperator> singletonList(new ScmMqNodeOperator());
            cmd.addTool(new ScmCreateNodeToolImpl(nodeProperties, opList));
            cmd.addTool(new ScmCreateTopicToolImpl());
            cmd.addTool(new ScmCreateGroupToolImpl());
            cmd.addTool(new ScmDeleteTopicToolImpl());
            cmd.addTool(new ScmDeleteGroupToolImpl());
            cmd.addTool(new ScmListTopicToolImpl());
            cmd.addTool(new ScmListGroupToolImpl());
            cmd.addTool(new ScmUpdateTopicToolImpl());
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
