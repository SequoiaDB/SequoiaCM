package com.sequoiacm.s3.tools;

import java.util.HashMap;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.command.InitRegionToolImpl;
import com.sequoiacm.s3.tools.command.RefreshAccesskeyToolImpl;

public class S3Admin {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("s3admin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "s3-server", "sequoiacm-s3-omserver-", ScmServerScriptEnum.S3SERVER));

        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addCloudParam().addServerPortParam(8002).get();

        nodeProperties.put("1", scmNodeRequiredParamGroup);
        try {
            cmd.addTool(new ScmCreateNodeToolImpl(nodeProperties, nodeTypes));
            cmd.addTool(new InitRegionToolImpl());
            cmd.addTool(new RefreshAccesskeyToolImpl());
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
