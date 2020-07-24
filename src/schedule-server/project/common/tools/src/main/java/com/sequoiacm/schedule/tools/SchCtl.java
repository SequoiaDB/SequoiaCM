package com.sequoiacm.schedule.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmListToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStartToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStopToolImpl;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class SchCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("schctl");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "schedule-server", "sequoiacm-schedule-server-"));
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
