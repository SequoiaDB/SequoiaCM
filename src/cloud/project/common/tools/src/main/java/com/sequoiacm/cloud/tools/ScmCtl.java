package com.sequoiacm.cloud.tools;

import java.util.Arrays;
import java.util.List;

import com.sequoiacm.cloud.tools.command.ScmStartToolImplCloud;
import com.sequoiacm.cloud.tools.command.ScmStopToolImplCloud;
import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmListToolImpl;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmAdminNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmAuthNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmGatewayNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceCenterNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmTraceNodeOperator;

public class ScmCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("scmcloudctl");
        try {
            List<ScmServiceNodeOperator> opList = Arrays.<ScmServiceNodeOperator> asList(
                    new ScmServiceCenterNodeOperator(), new ScmGatewayNodeOperator(),
                    new ScmAuthNodeOperator(), new ScmAdminNodeOperator(),
                    new ScmTraceNodeOperator());
            cmd.addTool(new ScmStartToolImplCloud(opList));
            cmd.addTool(new ScmStopToolImplCloud(opList));
            cmd.addTool(new ScmListToolImpl(opList));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }

}
