package com.sequoiacm.fulltext.tools;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmListToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStartToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStopToolImpl;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class FulltextCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("ftctl");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "fulltext-server", "sequoiacm-fulltext-server-"));
        try {
            cmd.addTool(new ScmStartToolImpl(nodeTypes));
            cmd.addTool(new ScmStopToolImpl(nodeTypes));
            cmd.addTool(new ScmListToolImpl(nodeTypes));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
