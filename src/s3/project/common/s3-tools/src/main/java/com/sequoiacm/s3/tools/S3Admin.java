package com.sequoiacm.s3.tools;

import java.util.HashMap;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3.tools.command.RefreshAccesskeyToolImpl;
import com.sequoiacm.s3.tools.command.ScmCreateNodeToolImplS3;
import com.sequoiacm.s3.tools.command.SetDefaultRegionToolImpl;
import com.sequoiacm.s3.tools.command.ShowDefaultRegionToolImpl;

public class S3Admin {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("s3admin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "s3-server", "sequoiacm-s3-server-", ScmServerScriptEnum.S3SERVER));

        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addCloudParam().addServerPortParam(8002).addZkParam().get();

        nodeProperties.put("1", scmNodeRequiredParamGroup);
        try {
            cmd.addTool(new ScmCreateNodeToolImplS3(nodeProperties, nodeTypes));
            cmd.addTool(new SetDefaultRegionToolImpl());
            cmd.addTool(new ShowDefaultRegionToolImpl());
            cmd.addTool(new RefreshAccesskeyToolImpl());

        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
