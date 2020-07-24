package com.sequoiacm.cloud.tools;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import com.sequoiacm.cloud.tools.command.ScmCleanSysTableToolImpl;
import com.sequoiacm.cloud.tools.command.ScmCreateNodeToolImplCloud;

public class ScmAdmin {
    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("scmcloudadmin");
        // 初始化节点类型信息
        ScmNodeTypeList nodeTypes = new ScmNodeTypeList();
        nodeTypes.add(new ScmNodeType("1", "service-center", "sequoiacm-cloud-servicecenter-"));
        nodeTypes.add(new ScmNodeType("2", "gateway", "sequoiacm-cloud-gateway-"));
        nodeTypes.add(new ScmNodeType("3", "auth-server", "sequoiacm-cloud-authserver-"));
        nodeTypes.add(new ScmNodeType("20", "service-trace", "sequoiacm-cloud-servicetrace-"));
        nodeTypes.add(new ScmNodeType("21", "admin-server", "sequoiacm-cloud-adminserver-"));
        try {
            cmd.addTool(new ScmCreateNodeToolImplCloud(nodeTypes));
            cmd.addTool(new ScmCleanSysTableToolImpl(nodeTypes));
        } catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }

        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
