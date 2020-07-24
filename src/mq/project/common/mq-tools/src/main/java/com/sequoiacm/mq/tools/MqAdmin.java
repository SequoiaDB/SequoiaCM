package com.sequoiacm.mq.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.mq.tools.command.ScmCreateGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmCreateTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmDeleteGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmDeleteTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmListGroupToolImpl;
import com.sequoiacm.mq.tools.command.ScmListTopicToolImpl;
import com.sequoiacm.mq.tools.command.ScmUpdateTopicToolImpl;
import com.sequoiacm.mq.tools.exception.ScmExitCode;

public class MqAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("mqadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "mq-server", "sequoiacm-mq-server-"));
        try {
            cmd.addTool(new ScmCreateNodeToolImpl(nodeTypes));
            cmd.addTool(new ScmCreateTopicToolImpl());
            cmd.addTool(new ScmCreateGroupToolImpl());
            cmd.addTool(new ScmDeleteTopicToolImpl());
            cmd.addTool(new ScmDeleteGroupToolImpl());
            cmd.addTool(new ScmListTopicToolImpl());
            cmd.addTool(new ScmListGroupToolImpl());
            cmd.addTool(new ScmUpdateTopicToolImpl());
        } catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
