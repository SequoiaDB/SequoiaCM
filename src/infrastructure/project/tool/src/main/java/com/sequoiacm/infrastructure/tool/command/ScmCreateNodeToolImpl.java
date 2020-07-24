package com.sequoiacm.infrastructure.tool.command;

import com.sequoiacm.infrastructure.tool.common.ScmNodeCreator;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

public class ScmCreateNodeToolImpl extends ScmTool {
    protected final String OPT_SHORT_CUSTOM_PROP = "D";
    protected Options ops;
    protected ScmHelpGenerator hp;
    protected final Logger logger = LoggerFactory.getLogger(ScmCreateNodeToolImpl.class.getName());
    protected ScmNodeTypeList nodeTypes;

    public ScmCreateNodeToolImpl(ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super("createnode");
        this.nodeTypes = nodeTypes;
        ops = new Options();
        hp = new ScmHelpGenerator();
        String dOptDesc = "specify properties";
        Option op = hp.createDOption(OPT_SHORT_CUSTOM_PROP, dOptDesc);
        ops.addOption(op);
        ScmCommandUtil.addTypeOption(nodeTypes, ops, hp, true, false);
    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String nodeType = cl.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE);

        ScmNodeType typEnum = this.nodeTypes.getNodeTypeByStr(nodeType);

        Properties nodeConf;
        if (cl.hasOption(OPT_SHORT_CUSTOM_PROP)) {
            nodeConf = cl.getOptionProperties(OPT_SHORT_CUSTOM_PROP);
        }
        else {
            nodeConf = new Properties();
        }

        ScmNodeCreator creator = new ScmNodeCreator(typEnum, nodeConf, this.nodeTypes);
        creator.create();
    }



}
