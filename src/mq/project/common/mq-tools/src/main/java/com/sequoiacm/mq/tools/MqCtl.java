package com.sequoiacm.mq.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmListToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStartToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStopToolImpl;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mq.tools.command.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.mq.tools.exception.ScmExitCode;

public class MqCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("mqctl");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "mq-server", "sequoiacm-mq-server-", ScmServerScriptEnum.MQSERVER));

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
