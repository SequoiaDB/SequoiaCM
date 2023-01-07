package com.sequoiacm.infrastructure.tool.command;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmNodeCreator;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperatorGroup;

public class ScmCreateNodeToolImpl extends ScmTool {
    protected final String OPT_SHORT_CUSTOM_PROP = "D";
    protected final ScmServiceNodeOperatorGroup nodeOeprators;
    private final String installPath;
    protected Options ops;
    protected ScmHelpGenerator hp;
    protected final Logger logger = LoggerFactory.getLogger(ScmCreateNodeToolImpl.class.getName());
    protected Map<String, ScmNodeRequiredParamGroup> nodeType2RequireParams;

    public ScmCreateNodeToolImpl(Map<String, ScmNodeRequiredParamGroup> nodeType2RequireParams,
            List<ScmServiceNodeOperator> nodeOperatorList) throws ScmToolsException {
        super("createnode");
        this.nodeOeprators = new ScmServiceNodeOperatorGroup(nodeOperatorList);
        this.installPath = ScmHelper.getPwd() + ".." + File.separator;
        nodeOeprators.init(installPath);
        this.nodeType2RequireParams = nodeType2RequireParams;
        ops = new Options();
        hp = new ScmHelpGenerator();
        String dOptDesc = "specify properties";
        Option op = hp.createDOption(OPT_SHORT_CUSTOM_PROP, dOptDesc);
        ops.addOption(op);
        ScmCommandUtil.addTypeOptionForCreate(nodeType2RequireParams,
                nodeOeprators.getSupportTypes(), ops, hp, true, false, true);
    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String nodeType = cl.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE);

        ScmNodeType typEnum = this.nodeOeprators.getSupportTypes().getNodeTypeByStr(nodeType);

        Properties nodeConf;
        if (cl.hasOption(OPT_SHORT_CUSTOM_PROP)) {
            nodeConf = cl.getOptionProperties(OPT_SHORT_CUSTOM_PROP);
        }
        else {
            nodeConf = new Properties();
        }

        if (nodeConf != null) {
            ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = nodeType2RequireParams.get(typEnum.getType());
            if (scmNodeRequiredParamGroup != null) {
                scmNodeRequiredParamGroup.check(nodeConf);
            }
        }
        ScmNodeCreator creator = new ScmNodeCreator(typEnum, nodeConf, this.nodeOeprators);
        creator.create();
    }

}
