package com.sequoiacm.daemon.command;

import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.manager.ScmManagerWrapper;
import com.sequoiacm.infrastructure.common.printor.ListLine;
import com.sequoiacm.infrastructure.common.printor.ListTable;
import com.sequoiacm.infrastructure.common.printor.ScmCommonPrintor;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.Options;

import java.util.ArrayList;
import java.util.List;

public class ScmListToolImpl extends ScmTool {

    private ScmManagerWrapper executor;
    private ScmHelpGenerator hp;
    private Options options;

    public ScmListToolImpl() throws ScmToolsException {
        super("list");
        hp = new ScmHelpGenerator();
        options = new Options();
        executor = ScmManagerWrapper.getInstance();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmCommandUtil.parseArgs(args, options);

        ListTable t = new ListTable();
        List<ScmNodeInfo> nodeList = executor.listNodeInfo();
        for (ScmNodeInfo n : nodeList) {
            ListLine l = new ListLine();
            l.addItem(n.getServerType().getType());
            l.addItem(n.getPort() + "");
            l.addItem(n.getStatus());
            l.addItem(n.getConfPath());
            t.addLine(l);
        }
        List<String> header = new ArrayList<>();
        header.add(DaemonDefine.SERVER_TYPE);
        header.add(DaemonDefine.PORT);
        header.add(DaemonDefine.STATUS);
        header.add(DaemonDefine.CONF_PATH);
        ScmCommonPrintor.print(header, t);

        if (t.size() == 0) {
            throw new ScmToolsException(ScmExitCode.EMPTY_OUT);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
