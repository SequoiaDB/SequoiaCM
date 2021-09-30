package com.sequoiacm.config.tools;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.*;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ConfCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("confctl");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "config-server", "sequoiacm-config-server-", ScmServerScriptEnum.CONFIGSERVER));
        try {
            cmd.addTool(new ScmStartToolImpl(nodeTypes));
            cmd.addTool(new ScmStopToolImpl(nodeTypes));
            cmd.addTool(new ScmListToolImpl(nodeTypes));
        } catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }

        cmd.execute(args);
    }
}
