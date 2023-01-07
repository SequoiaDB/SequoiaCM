package com.sequoiacm.config.tools;

import java.util.Collections;
import java.util.List;

import com.sequoiacm.infrastructure.tool.operator.ScmConfNodeOperator;
import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.command.ScmListToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStartToolImpl;
import com.sequoiacm.infrastructure.tool.command.ScmStopToolImpl;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;

public class ConfCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("confctl");

        try {
            List<ScmServiceNodeOperator> opList = Collections
                    .<ScmServiceNodeOperator> singletonList(new ScmConfNodeOperator());
            cmd.addTool(new ScmStartToolImpl(opList));
            cmd.addTool(new ScmStopToolImpl(opList));
            cmd.addTool(new ScmListToolImpl(opList));
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }

        cmd.execute(args);
    }
}
