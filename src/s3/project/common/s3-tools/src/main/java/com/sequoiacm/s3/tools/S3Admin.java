package com.sequoiacm.s3.tools;

import java.util.HashMap;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.element.*;
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
        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.S3SERVER, ScmServerScriptEnum.S3SERVER));

        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addCloudParam().addServerPortParam(8002).addMetaDataParam().addZkParam()
                .addParam(ScmNodeRequiredParam.keyParamInstance("scm.content-module.site",
                        "-Dscm.content-module.site=rootsite"))
                .addParam(ScmNodeRequiredParam.keyParamInstance("spring.application.name",
                        "-Dspring.application.name=rootsite-s3"))
                .get();

        nodeProperties.put(ScmNodeTypeEnum.S3SERVER.getTypeNum(), scmNodeRequiredParamGroup);
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
