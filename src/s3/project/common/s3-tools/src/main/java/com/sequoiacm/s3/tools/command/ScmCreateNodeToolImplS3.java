package com.sequoiacm.s3.tools.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;

import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmNodeCreator;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmCreateNodeToolImplS3 extends ScmCreateNodeToolImpl {
    private final String OPT_LONG_AUDIT_URL = "adurl";
    private final String OPT_LONG_AUDIT_USER = "aduser";
    private final String OPT_LONG_AUDIT_PASSED = "adpasswd";

    public ScmCreateNodeToolImplS3(HashMap<String, ScmNodeRequiredParamGroup> nodeProperties,
                                   ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super(nodeProperties, nodeTypes);
        ops.addOption(
                hp.createOpt(null, OPT_LONG_AUDIT_URL, "audit to sdb url.", true, true, false));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_AUDIT_USER, "audit to sdb user.", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AUDIT_PASSED, "audit to sdb passwd.", true, true,
                false));
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

        if (nodeConf != null) {
            ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = nodeType2RequireParams.get(typEnum.getType());
            if (scmNodeRequiredParamGroup != null) {
                scmNodeRequiredParamGroup.check(nodeConf);
            }
        }
        String adurl = cl.getOptionValue(OPT_LONG_AUDIT_URL);
        String aduser = cl.getOptionValue(OPT_LONG_AUDIT_USER);
        String adpasswd = cl.getOptionValue(OPT_LONG_AUDIT_PASSED);
        logger.info("adurl=" + adurl + ", aduser" + aduser + ", adpasswd=" + adpasswd);

        Map<Object, Object> otherLog = new HashMap<>();
        otherLog.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_URL, adurl);
        otherLog.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_USER, aduser);
        otherLog.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_PASSWD, adpasswd);
        ScmNodeCreator creator = new ScmNodeCreator(typEnum, nodeConf, nodeTypes, otherLog, true);
        creator.create();
    }
}
