package com.sequoiacm.s3.tools.command;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.s3.tools.S3Admin;
import com.sequoiacm.s3.tools.common.ScmCloudNodeCreator;
import com.sequoiacm.s3.tools.common.ScmCommandUtil;
import com.sequoiacm.s3.tools.common.ScmHelpGenerator;
import com.sequoiacm.s3.tools.element.ScmNodeType;
import com.sequoiacm.s3.tools.exception.ScmToolsException;

public class ScmCreateNodeToolImpl implements ScmTool {
    private final String OPT_SHORT_CUSTOM_PROP = "D";
    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(ScmCreateNodeToolImpl.class.getName());

    public ScmCreateNodeToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        String dOptDesc = "specify properties";
        Option op = hp.createDOption(OPT_SHORT_CUSTOM_PROP, dOptDesc);
        ops.addOption(op);
        ScmCommandUtil.addTypeOption(ops, hp, true, false);
    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        S3Admin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String nodeType = cl.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE);

        ScmNodeType typEnum = ScmNodeType.getNodeTypeByStr(nodeType);

        Properties nodeConf;
        if (cl.hasOption(OPT_SHORT_CUSTOM_PROP)) {
            nodeConf = cl.getOptionProperties(OPT_SHORT_CUSTOM_PROP);
        }
        else {
            nodeConf = new Properties();
        }

        ScmCloudNodeCreator creator = new ScmCloudNodeCreator(typEnum, nodeConf);
        creator.create();
    }
}
