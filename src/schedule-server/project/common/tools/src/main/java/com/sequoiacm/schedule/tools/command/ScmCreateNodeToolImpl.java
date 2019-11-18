package com.sequoiacm.schedule.tools.command;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.tools.SchAdmin;
import com.sequoiacm.schedule.tools.common.ScmCloudNodeCreator;
import com.sequoiacm.schedule.tools.common.ScmCommandUtil;
import com.sequoiacm.schedule.tools.common.ScmHelpGenerator;
import com.sequoiacm.schedule.tools.element.ScmNodeType;
import com.sequoiacm.schedule.tools.exception.ScmToolsException;

public class ScmCreateNodeToolImpl implements ScmTool {
    private final String OPT_SHORT_CUSTOM_PROP = "D";
    private final String OPT_LONG_AUDIT_URL = "adurl";
    private final String OPT_LONG_AUDIT_USER = "aduser";
    private final String OPT_LONG_AUDIT_PASSED = "adpasswd";
    private Options ops;
    private ScmHelpGenerator hp;
    private final Logger logger = LoggerFactory.getLogger(ScmCreateNodeToolImpl.class.getName());

    public ScmCreateNodeToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        String dOptDesc = "specify properties";
        ops.addOption(hp.createDOption(OPT_SHORT_CUSTOM_PROP, dOptDesc));
        ops.addOption(hp.createOpt(null, OPT_LONG_AUDIT_URL, "audit to sdb url.", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AUDIT_USER, "audit to sdb user.", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AUDIT_PASSED, "audit to sdb passwd.", true, true,
                false));
        ScmCommandUtil.addTypeOption(ops, hp, true, false);
    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        SchAdmin.checkHelpArgs(args);
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

        String adurl = cl.getOptionValue(OPT_LONG_AUDIT_URL);
        String aduser = cl.getOptionValue(OPT_LONG_AUDIT_USER);
        String adpasswd = cl.getOptionValue(OPT_LONG_AUDIT_PASSED);
        logger.info("adurl="+adurl+", aduser"+aduser+", adpasswd="+adpasswd);
        ScmCloudNodeCreator creator = new ScmCloudNodeCreator(typEnum, nodeConf, adurl, aduser, adpasswd);
        creator.create();
    }
}
