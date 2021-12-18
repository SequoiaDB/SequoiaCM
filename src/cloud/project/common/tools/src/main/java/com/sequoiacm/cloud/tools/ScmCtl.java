package com.sequoiacm.cloud.tools;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmListToolImpl;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import com.sequoiacm.cloud.tools.command.ScmStartToolImplCloud;
import com.sequoiacm.cloud.tools.command.ScmStopToolImplCloud;

public class ScmCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("scmcloudctl");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "service-center", "sequoiacm-cloud-servicecenter-",
                ScmServerScriptEnum.SERVICECENTER, false));
        nodeTypes.add(new ScmNodeType("2", "gateway", "sequoiacm-cloud-gateway-", ScmServerScriptEnum.GATEWAY));
        nodeTypes.add(new ScmNodeType("3", "auth-server", "sequoiacm-cloud-authserver-", ScmServerScriptEnum.AUTHSERVER));
        nodeTypes.add(new ScmNodeType("20", "service-trace", "sequoiacm-cloud-servicetrace-",
                ScmServerScriptEnum.SERVICETRACE, false));
        nodeTypes.add(new ScmNodeType("21", "admin-server", "sequoiacm-cloud-adminserver-", ScmServerScriptEnum.ADMINSERVER));

        try {
            cmd.addTool(new ScmStartToolImplCloud(nodeTypes));
            cmd.addTool(new ScmStopToolImplCloud(nodeTypes));
            cmd.addTool(new ScmListToolImpl(nodeTypes));
        } catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }

}
