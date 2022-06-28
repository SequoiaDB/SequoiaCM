package com.sequoiacm.config.tools;

import java.util.HashMap;

import com.sequoiacm.config.tools.command.ScmDeleteConfigImpl;
import com.sequoiacm.config.tools.command.ScmListSubscribersImpl;
import com.sequoiacm.config.tools.command.ScmUnsubscribeImpl;
import com.sequoiacm.config.tools.command.ScmUpdateConfigImpl;
import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ConfAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("confadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "config-server", "sequoiacm-config-server-", ScmServerScriptEnum.CONFIGSERVER));
        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addCloudParam().addSdbParam().addZkParam().addServerPortParam(8190).get();

        nodeProperties.put("1", scmNodeRequiredParamGroup);
        try {
            cmd.addTool(new ScmCreateNodeToolImpl(nodeProperties, nodeTypes));
            cmd.addTool(new ScmUnsubscribeImpl());
            cmd.addTool(new ScmListSubscribersImpl());
            cmd.addTool(new ScmUnsubscribeImpl());
            cmd.addTool(new ScmUpdateConfigImpl());
            cmd.addTool(new ScmDeleteConfigImpl());
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }

        // admin 日志
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}