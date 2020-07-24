package com.sequoiacm.config.tools;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmHelpToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.config.tools.command.ScmListSubscribersImpl;
import com.sequoiacm.config.tools.command.ScmUnsubscribeImpl;

import java.util.ArrayList;
import java.util.List;

public class ConfAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("ConfAdmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "config-server", "sequoiacm-config-server-"));
        try {
            cmd.addTool(new ScmCreateNodeToolImpl(nodeTypes));
            cmd.addTool(new ScmUnsubscribeImpl());
            cmd.addTool(new ScmListSubscribersImpl());
            cmd.addTool(new ScmUnsubscribeImpl());
        } catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        // admin 日志
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
