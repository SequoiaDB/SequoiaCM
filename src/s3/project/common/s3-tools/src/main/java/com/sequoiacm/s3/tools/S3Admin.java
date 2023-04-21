package com.sequoiacm.s3.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParam;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.s3.tools.command.RefreshAccesskeyToolImpl;
import com.sequoiacm.s3.tools.command.ScmCreateNodeToolImplS3;
import com.sequoiacm.s3.tools.command.SetDefaultRegionToolImpl;
import com.sequoiacm.s3.tools.command.ShowDefaultRegionToolImpl;
import com.sequoiacm.infrastructure.tool.operator.ScmS3NodeOperator;
import com.sequoiacm.s3.tools.command.quota.CancelSyncQuotaToolImpl;
import com.sequoiacm.s3.tools.command.quota.DisableQuotaToolImpl;
import com.sequoiacm.s3.tools.command.quota.EnableQuotaToolImpl;
import com.sequoiacm.s3.tools.command.quota.QuotaStatusToolImpl;
import com.sequoiacm.s3.tools.command.quota.SyncQuotaToolImpl;
import com.sequoiacm.s3.tools.command.quota.UpdateQuotaToolImpl;

public class S3Admin {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("s3admin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType(ScmNodeTypeEnum.S3SERVER, ScmServerScriptEnum.S3SERVER));

        HashMap<String, ScmNodeRequiredParamGroup> nodeProperties = new HashMap<>();
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addCloudParam().addServerPortParam(8002).addSdbParam().addZkParam()
                .addParam(ScmNodeRequiredParam.keyParamInstance("scm.content-module.site",
                        "-Dscm.content-module.site=rootsite"))
                .addParam(ScmNodeRequiredParam.keyParamInstance("spring.application.name",
                        "-Dspring.application.name=rootsite-s3"))
                .get();

        nodeProperties.put(ScmNodeTypeEnum.S3SERVER.getTypeNum(), scmNodeRequiredParamGroup);
        try {
            List<ScmServiceNodeOperator> opList = Collections
                    .<ScmServiceNodeOperator> singletonList(new ScmS3NodeOperator());
            cmd.addTool(new ScmCreateNodeToolImplS3(nodeProperties, opList));
            cmd.addTool(new SetDefaultRegionToolImpl());
            cmd.addTool(new ShowDefaultRegionToolImpl());
            cmd.addTool(new RefreshAccesskeyToolImpl());
            cmd.addTool(new EnableQuotaToolImpl());
            cmd.addTool(new DisableQuotaToolImpl());
            cmd.addTool(new UpdateQuotaToolImpl());
            cmd.addTool(new QuotaStatusToolImpl());
            cmd.addTool(new SyncQuotaToolImpl());
            cmd.addTool(new CancelSyncQuotaToolImpl());

        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
