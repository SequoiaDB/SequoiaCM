package com.sequoiacm.cloud.tools.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.command.ScmStopToolImpl;
import com.sequoiacm.infrastructure.tool.common.*;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.exec.ScmExecutorWrapper;
import org.apache.commons.cli.CommandLine;

public class ScmStopToolImplCloud extends ScmStopToolImpl {
    private final String OPT_LONG_TIMEOUT = "timeout";

    public ScmStopToolImplCloud(ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super(nodeTypes);
        SLEEP_TIME = 1000;
        options.addOption(hp.createOpt(null, OPT_LONG_TIMEOUT,
                "sets the stopping timeout in seconds, default:30", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        // 先解析一次命令行 设置 stop timeout
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_LONG_TIMEOUT)) {
            STOP_TIMEOUT = ScmCommandUtil.getTimeout(commandLine, OPT_LONG_TIMEOUT);
        }

        // 再调用父类的 process
        super.process(args);
    }
}
