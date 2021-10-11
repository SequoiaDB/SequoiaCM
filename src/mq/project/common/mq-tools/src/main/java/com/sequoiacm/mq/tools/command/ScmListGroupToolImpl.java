package com.sequoiacm.mq.tools.command;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;

import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupDetail;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.tools.MqAdmin;
import com.sequoiacm.mq.tools.common.ListLine;
import com.sequoiacm.mq.tools.common.ListTable;
import com.sequoiacm.mq.tools.common.ScmCommonPrintor;
import com.sequoiacm.mq.tools.exception.ScmExitCode;

public class ScmListGroupToolImpl extends MqToolBase {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";

    public ScmListGroupToolImpl() throws ScmToolsException {
        super("listgroup");
        ops.addOption(hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME,
                "specified group name to get the group detail.", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);

        if (!cl.hasOption(OPT_SHORT_NAME)) {
            listAllGroup(cl);
            return;
        }
        AdminClient client = getAdminClient(cl);
        try {
            ConsumerGroupDetail group = client.getGroup(cl.getOptionValue(OPT_SHORT_NAME));
            ListTable listTable = new ListTable();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (ConsumerPartitionInfo p : group.getConsumerPartitionInfos()) {
                ListLine line = new ListLine();
                line.addItem(p.getPartitionNum() + "");
                line.addItem(p.getConsumer());
                line.addItem(p.getLastDeliveredId() + "");
                line.addItem(sdf.format(new Date(p.getLastRequestTime())));
                line.addItem(ScmCommon.listToString(p.getPendingMsgs()));
                listTable.addLine(line);
            }
            List<String> header = Arrays.asList("PartitionNum", "Consumer", "LastDeleveredId",
                    "LastRequestTime", "PendingMsg");
            System.out.println("Topic:" + group.getTopic() + ", Group:" + group.getName());
            ScmCommonPrintor.print(header, listTable);
        }
        catch (MqException e) {
            if (e.getError() == MqError.CONSUMER_GROUP_NOT_EXIST) {
                throw new ScmToolsException("group not exist:" + cl.getOptionValue(OPT_SHORT_NAME),
                        ScmExitCode.SCM_NOT_EXIST_ERROR, e);
            }
            throw new ScmToolsException("failed to list group, cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to list group, cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    private void listAllGroup(CommandLine cl) throws ScmToolsException {
        AdminClient client = getAdminClient(cl);
        try {
            List<ConsumerGroupDetail> groups = client.listGroup();
            ListTable listTable = new ListTable();
            for (ConsumerGroupDetail g : groups) {
                ListLine line = new ListLine();
                line.addItem(g.getTopic());
                line.addItem(g.getName());
                listTable.addLine(line);
            }
            List<String> header = Arrays.asList("Topic", "Group");
            ScmCommonPrintor.print(header, listTable);
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to list group, cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

}
