package com.sequoiacm.s3.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.s3.tools.command.InitRegionToolImpl;
import com.sequoiacm.s3.tools.command.RefreshAccesskeyToolImpl;





public class S3Admin {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("s3admin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "s3-server", "sequoiacm-s3-omserver-"));
        try {
            cmd.addTool(new ScmCreateNodeToolImpl(nodeTypes));
            cmd.addTool(new InitRegionToolImpl());
            cmd.addTool(new RefreshAccesskeyToolImpl());
        } catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
