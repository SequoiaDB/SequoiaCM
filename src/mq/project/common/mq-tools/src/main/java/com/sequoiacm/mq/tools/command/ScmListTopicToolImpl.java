package com.sequoiacm.mq.tools.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;

import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.TopicDetail;
import com.sequoiacm.mq.tools.MqAdmin;
import com.sequoiacm.mq.tools.common.ListLine;
import com.sequoiacm.mq.tools.common.ListTable;
import com.sequoiacm.mq.tools.common.ScmCommonPrintor;
import com.sequoiacm.mq.tools.exception.ScmExitCode;

public class ScmListTopicToolImpl extends MqToolBase {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";

    public ScmListTopicToolImpl() throws ScmToolsException {
        super("listtopic");
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "topic name.", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String name = cl.getOptionValue(OPT_LONG_NAME);
        List<TopicDetail> topics = null;
        AdminClient client = getAdminClient(cl);
        try {
            if (name == null) {
                topics = client.listTopic();
            }
            else {
                topics = new ArrayList<>(1);
                topics.add(client.getTopic(name));
            }
        }
        catch (MqException e) {
            if (e.getError() == MqError.TOPIC_NOT_EXIST) {
                throw new ScmToolsException("topic not exist:" + name, ScmExitCode.SCM_NOT_EXIST_ERROR,
                        e);
            }
            throw new ScmToolsException(
                    "failed to list topic:" + name + ", cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        catch (Exception e) {
            throw new ScmToolsException(
                    "failed to list topic:" + name + ", cause by:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }

        ListTable listTable = new ListTable();
        for (TopicDetail t : topics) {
            ListLine line = new ListLine();
            line.addItem(t.getName());
            line.addItem(t.getLatestMsgId() + "");
            line.addItem(t.getMessageTableName());
            line.addItem(t.getPartitionCount() + "");
            line.addItem(ScmCommon.listToString(t.getConsumerGroup()));
            listTable.addLine(line);
        }
        List<String> header = Arrays.asList("Name", "LatestMsgId", "MessageTable", "PartitionCount",
                "ConsumerGroup");
        ScmCommonPrintor.print(header, listTable);
    }

}
