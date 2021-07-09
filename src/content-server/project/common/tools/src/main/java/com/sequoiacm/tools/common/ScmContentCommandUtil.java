package com.sequoiacm.tools.common;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.element.LocationMsg;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exec.ScmExecutorWrapper;

public class ScmContentCommandUtil {
    private static final Logger logger = LoggerFactory.getLogger(ScmContentCommandUtil.class);
    public static final String OPT_LONG_RDSURL = "rdsurl";
    public static final String OPT_LONG_RDSUSER = "rdsuser";
    public static final String OPT_LONG_RDSPASSWD = "rdspasswd";

    public static final String OPT_LONG_MDSURL = "mdsurl";
    public static final String OPT_LONG_MDSUSER = "mdsuser";
    public static final String OPT_LONG_MDSPASSWD = "mdspasswd";

    public static final String OPT_SHORT_CUSTOM_PROP = "D";

    public static final String OPT_SHORT_HELP = "h";
    public static final String OPT_LONG_HELP = "help";
    public static final String OPT_LONG_VER = "version";
    public static final String OPT_SHORT_VER = "v";

    public static final String LOCALTION_SITE_NAME = "site";

    public static void addDsOption(Options ops, ScmHelpGenerator hp) throws ScmToolsException {
        ops.addOption(hp.createOpt(null, OPT_LONG_MDSURL,
                "meta datasource url(hostName:port) of root site,\ndefault:system will search sysconf.properties for\nrootsite.url.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_MDSUSER,
                "meta datasource user name of root site, if --" + OPT_LONG_MDSURL
                        + "\nis specified, the default username is empty,\nelse system will search sysconf.properties for\nrootsite.user.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_MDSPASSWD,
                "meta datasource password of root site, if --" + OPT_LONG_MDSURL
                        + "\nis specified, the default passwd is empty,\nelse system will search sysconf.properties for\nrootsite.passwd.",
                false, true, false));

        // invisible options,for backupwards compatibility
        ops.addOption(hp.createOpt(null, OPT_LONG_RDSURL,
                "meta datasource url(hostName:port) of root site,\ndefault:system will search sysconf.properties for\nrootsite.url.",
                false, true, true));
        ops.addOption(hp.createOpt(null, OPT_LONG_RDSUSER,
                "meta datasource user name of root site, if --" + OPT_LONG_RDSURL
                        + "\nis specified, the default username is empty,\nelse system will search sysconf.properties for\nrootsite.user.",
                false, true, true));
        ops.addOption(hp.createOpt(null, OPT_LONG_RDSPASSWD,
                "meta datasource password of root site, if --" + OPT_LONG_RDSURL
                        + "\nis specified, the default passwd is empty,\nelse system will search sysconf.properties for\nrootsite.passwd.",
                false, true, true));

        return;
    }

    public static void addDOption(ScmNodeRequiredParamGroup scmNodeRequiredParamGroup,
            Options ops, ScmHelpGenerator hp) throws ScmToolsException {
        StringBuilder second = new StringBuilder();
        second.append("specify properties, required properties: \r\n");
        for (String str : scmNodeRequiredParamGroup.getExample()) {
            second.append("\t");
            second.append(str);
            second.append(" \r\n");
        }
        ops.addOption(
                hp.createDOption(OPT_SHORT_CUSTOM_PROP, second.toString()));
        return;
    }

    public static CommandLine parseArgs(String[] args, Options options, boolean stopAtNonOption)
            throws ScmToolsException {
        CommandLine commandLine;
        CommandLineParser parser = new DefaultParser();
        try {
            commandLine = parser.parse(options, args, stopAtNonOption);
            return commandLine;
        }
        catch (ParseException e) {
            logger.error("Invalid arg", e);
            throw new ScmToolsException(e.getMessage(), ScmExitCode.INVALID_ARG);
        }
    }

    public static CommandLine parseArgs(String[] args, Options options) throws ScmToolsException {
        return parseArgs(args, options, false);
    }

    public static boolean isContainHelpArg(String[] args) {
        for (String arg : args) {
            if (arg.equals("-" + OPT_SHORT_HELP) || arg.equals("--" + OPT_LONG_HELP)
                    || arg.equals(OPT_LONG_HELP)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNeedPrintVersion(String[] args) {
        if (args.length == 1) {
            if (args[0].equals("-" + OPT_SHORT_VER) || args[0].equals("--" + OPT_LONG_VER)) {
                return true;
            }
        }
        return false;
    }

    public static ScmSdbInfo parseDsOption(CommandLine commandLine) throws ScmToolsException {
        ScmSdbInfo info = new ScmSdbInfo();
        if (commandLine.hasOption(OPT_LONG_MDSURL)) {
            parseSpecifiedDsOp(commandLine, info, OPT_LONG_MDSURL, OPT_LONG_MDSUSER,
                    OPT_LONG_MDSPASSWD);
        }
        // for backupwards compatibility
        else if (commandLine.hasOption(OPT_LONG_RDSURL)) {
            parseSpecifiedDsOp(commandLine, info, OPT_LONG_RDSURL, OPT_LONG_RDSUSER,
                    OPT_LONG_RDSPASSWD);
        }
        else {
            if (commandLine.hasOption(OPT_LONG_MDSUSER)
                    || commandLine.hasOption(OPT_LONG_MDSPASSWD)) {
                throw new ScmToolsException("missing option : --" + OPT_LONG_MDSURL,
                        ScmExitCode.INVALID_ARG);
            }
            // for backupwards compatibility
            if (commandLine.hasOption(OPT_LONG_RDSUSER)
                    || commandLine.hasOption(OPT_LONG_RDSPASSWD)) {
                throw new ScmToolsException("missing option : --" + OPT_LONG_RDSURL,
                        ScmExitCode.INVALID_ARG);
            }

            ScmExecutorWrapper executor = new ScmExecutorWrapper();
            ScmSdbInfo localSdbInfo = executor.getMainSiteSdb();
            if (localSdbInfo == null) {
                logger.error("Can't find data source url of root site in local conf,please set --"
                        + OPT_LONG_MDSURL);
                throw new ScmToolsException(
                        "Can't find data source url of root site in local conf,please set --"
                                + OPT_LONG_MDSURL,
                        ScmExitCode.INVALID_ARG);
            }
            info.setSdbUrl(localSdbInfo.getSdbUrl());
            info.setSdbPasswdFile(localSdbInfo.getSdbPasswdFile());
            info.setSdbUser(localSdbInfo.getSdbUser());
        }

        return info;
    }

    private static void parseSpecifiedDsOp(CommandLine commandLine, ScmSdbInfo info, String urlOp,
            String userOp, String passwdOp) throws ScmToolsException {
        String doption = commandLine.getOptionValue(urlOp);
        info.setSdbUrl(doption);
        if (commandLine.hasOption(userOp) && commandLine.hasOption(passwdOp)) {
            info.setSdbUser(commandLine.getOptionValue(userOp));
            info.setSdbPasswdFile(commandLine.getOptionValue(passwdOp));
        }
        else if (!commandLine.hasOption(userOp) && !commandLine.hasOption(passwdOp)) {
            info.setSdbUser("");
            info.setSdbPasswdFile("");
        }
        else {
            logger.error("Please set --" + userOp + " and --" + passwdOp + " at the same time");
            throw new ScmToolsException(
                    "Please set --" + userOp + " and --" + passwdOp + " at the same time",
                    ScmExitCode.INVALID_ARG);
        }
    }

    public static boolean checkIsContainMainSite(BasicBSONList dataLocationList, ScmMetaMgr mg)
            throws ScmToolsException {
        boolean isContainMainSite = false;
        ScmSiteInfo mainSite = mg.getMainSiteChecked();
        for (Object ele : dataLocationList) {
            BSONObject obj = (BSONObject) ele;
            int siteId = (int) obj.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
            if (mainSite.getId() == siteId) {
                isContainMainSite = true;
            }
        }
        return isContainMainSite;
    }

    public static BasicBSONList parseDataLocation(String dataLocationValue)
            throws ScmToolsException {
        BasicBSONList dataLocationList = null;
        try {
            dataLocationList = (BasicBSONList) JSON.parse(dataLocationValue);
        }
        catch (Exception e) {
            logger.error("Convert " + dataLocationValue + " to BasicBSONList failed,error msg:"
                    + e.getMessage(), e);
            throw new ScmToolsException("Convert " + dataLocationValue
                    + " to BasicBSONList failed,error msg:" + e.getMessage(),
                    ScmExitCode.INVALID_ARG);
        }
        return dataLocationList;
    }

    public static List<LocationMsg> parseLocationOption(String src, ScmMetaMgr mg)
            throws ScmToolsException {
        List<LocationMsg> list = new ArrayList<>();
        String[] siteAndDomainStrs = src.split(",");
        for (String siteAndDomainStr : siteAndDomainStrs) {
            String[] tmp = siteAndDomainStr.split(":");
            if (tmp.length != 2) {
                logger.error("Failed to analyze siteName and domainName from:" + src);
                throw new ScmToolsException("Failed to analyze siteName and domainName from:" + src,
                        ScmExitCode.INVALID_ARG);
            }
            int siteId = mg.getSiteIdByName(tmp[0]);
            list.add(new LocationMsg(siteId, tmp[1]));
        }
        return list;
    }

    public static String getDataOptionDesc() {
        StringBuilder dataOpDescBuilder = new StringBuilder();
        dataOpDescBuilder.append("workspace data location, the value is json, eg:\n");
        dataOpDescBuilder.append("'[\n");
        dataOpDescBuilder.append(
                "    {" + CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME + ":\"sdbSite\","
                        + FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN + ":\"dataDomain\"},\n");
        dataOpDescBuilder.append(
                "    {" + CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME + ":\"hbaseSite\"}\n");
        dataOpDescBuilder.append("]'\n");
        dataOpDescBuilder.append("\n");
        dataOpDescBuilder.append("for sequoiadb site obj, field '"
                + FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS + "' specifies\n");
        dataOpDescBuilder.append("cs&cl options, eg:\n");
        dataOpDescBuilder.append(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS + ":{\n");
        dataOpDescBuilder.append("    collection_space:{LobPageSize:262144},\n");
        dataOpDescBuilder.append("    collection:{ReplSize:-1}\n");
        dataOpDescBuilder.append("}\n");
        dataOpDescBuilder.append("field '" + FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE
                + "' specifies cs&cl sharding\n");
        dataOpDescBuilder.append("type, eg:\n");
        dataOpDescBuilder.append(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE + ":{\n");
        dataOpDescBuilder.append("    collection_space:\"year\",\n");
        dataOpDescBuilder.append("    collection:\"month\"\n");
        dataOpDescBuilder.append("}\n");
        dataOpDescBuilder.append("default cs sharding type is 'year'\n");
        dataOpDescBuilder.append("default cl sharding type is 'month'\n");
        dataOpDescBuilder.append("all optional sharding types:'year', 'quarter',\n");
        dataOpDescBuilder.append("'month', 'none', cl does not support 'none'.\n");
        dataOpDescBuilder.append("\n");
        dataOpDescBuilder.append("for hbase site obj, field '"
                + FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE + "'\n");
        dataOpDescBuilder.append("specifies table sharding type, default:'month',\n");

        // dataOpDescBuilder.append("fields
        // '"+FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_OPERATION_TIMEOUT+"'\n");
        // dataOpDescBuilder.append("'"+FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_PAUSE+"'
        // '"+FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_RETRIES_NUMBER+"'\n");
        // dataOpDescBuilder.append("'"+FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD+"'
        // '"+FieldName.FIELD_CLWORKSPACE_HBASE_RPC_TIMEOUT+"'\n");
        // dataOpDescBuilder.append("specifies hbase conection properties,
        // those\n");
        dataOpDescBuilder.append("field '" + FieldName.FIELD_CLWORKSPACE_HABSE_NAME_SPACE
                + "' specifies a namespace for\n");
        dataOpDescBuilder.append("scm hbase table, default:'default'\n");
        dataOpDescBuilder.append("\n");

        dataOpDescBuilder.append("for hdfs site obj, field '"
                + FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE + "'\n");
        dataOpDescBuilder.append("specifies table sharding type, default:'month',\n");
        dataOpDescBuilder.append("fields '" + FieldName.FIELD_CLWORKSPACE_HDFS_DFS_ROOT_PATH
                + "', default:'/scm',\n");
        dataOpDescBuilder.append("\n");
        dataOpDescBuilder.append("for ceph_swift site obj, field '"
                + FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE + "'\n");
        dataOpDescBuilder.append("specifies container sharding type, default:'month'\n");
        dataOpDescBuilder.append("\n");
        dataOpDescBuilder.append("for ceph_s3 site obj, field '"
                + FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE + "'\n");
        dataOpDescBuilder.append("specifies bucket sharding type, default:'month',\n");
        dataOpDescBuilder.append("field '" + FieldName.FIELD_CLWORKSPACE_CONTAINER_PREFIX
                + "' specifies bucket name\n");
        dataOpDescBuilder.append("prefix, default prefix: wsName-scmfile (wsName\n");
        dataOpDescBuilder.append("will be change to lowercase and '_' will be replace\n");
        dataOpDescBuilder.append("with '-' in wsName.");
        return dataOpDescBuilder.toString();
    }

    public static String getMetaOptionDesc() {
        StringBuilder metaOpDesc = new StringBuilder();
        metaOpDesc.append("workspace meta location, the value is json, eg:\n");
        metaOpDesc.append("'{\n");
        metaOpDesc.append(
                "     " + CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME + ":\"rootSite\",\n");
        metaOpDesc.append(
                "     " + FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN + ":\"metaDomain\",\n");
        metaOpDesc
                .append("     " + FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE + ":\"year\",\n");
        metaOpDesc.append("     " + FieldName.FIELD_CLWORKSPACE_META_OPTIONS + ":{\n");
        metaOpDesc.append("         collection_space:{LobPageSize:262144},\n");
        metaOpDesc.append("         collection:{ReplSize:-1}\n");
        metaOpDesc.append("    }\n");
        metaOpDesc.append("}'\n");
        metaOpDesc.append("default meta sharding type is 'year', all supported\n");
        metaOpDesc.append("types:'month', 'year', 'quarter'.\n");
        metaOpDesc.append("field '" + FieldName.FIELD_CLWORKSPACE_META_OPTIONS
                + "' specifies meta cs&cl options.");
        return metaOpDesc.toString();
    }

    public static List<String> parseListUrls(String gatewayUrl) {
        List<String> urls = new ArrayList<>();
        String[] arr = gatewayUrl.split(",");
        for (String url : arr) {
            urls.add(url);
        }
        return urls;
    }

    public static void checkArgInUriPath(String argName, String argValue) throws ScmToolsException {
        if (!ScmArgChecker.checkUriPathArg(argValue)) {
            throw new ScmToolsException(argName + " is invalid:" + argName + "=" + argValue,
                    ScmExitCode.INVALID_ARG);
        }
    }
}
