package com.sequoiacm.fulltext.tools;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class FulltextAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("ftadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "fulltext-server", "sequoiacm-fulltext-server-"));
        try {
            cmd.addTool(new ScmCreateNodeToolImpl(nodeTypes));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);

        cmd.execute(args);
    }
}
