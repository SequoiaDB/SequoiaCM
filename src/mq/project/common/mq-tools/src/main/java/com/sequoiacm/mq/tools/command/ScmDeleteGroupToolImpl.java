package com.sequoiacm.mq.tools.command;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;

import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.tools.MqAdmin;
import com.sequoiacm.mq.tools.exception.ScmExitCode;

public class ScmDeleteGroupToolImpl extends MqToolBase {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";

    public ScmDeleteGroupToolImpl() throws ScmToolsException {
        super("deletegroup");
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "group name.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String name = cl.getOptionValue(OPT_LONG_NAME);
        AdminClient client = getAdminClient(cl);
        try {
            client.deleteGroup(name);
            System.out.println("delete group success:" + name);
        }
        catch (MqException e) {
            throw new ScmToolsException(
                    "failed to delete group:" + name + ", cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

}
