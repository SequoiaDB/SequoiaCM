package com.sequoiacm.diagnose.command;

import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.diagnose.common.FileOperator;
import com.sequoiacm.diagnose.common.ResidueCheckInfo;
import com.sequoiacm.diagnose.config.WorkPathConfig;
import com.sequoiacm.diagnose.datasource.ScmDataSourceMgr;
import com.sequoiacm.diagnose.printer.ResiduePrinter;
import com.sequoiacm.diagnose.progress.ResidueProgress;
import com.sequoiacm.diagnose.task.DataCheckRunner;
import com.sequoiacm.diagnose.task.ExecutionContext;
import com.sequoiacm.diagnose.task.ResidueCheckTaskFactory;
import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Command
public class ResidueCommand extends SubCommand {
    private static final String NAME = "residue";
    private static final String WORK_PATH = "work-path";
    private static final String WORKSPACE = "workspace";
    private static final String SITE_NAME = "site";
    private static final String GATEWAY_URL = "url";
    private static final String ADMIN_USER = "user";
    private static final String ADMIN_USER_PASSWD = "passwd";
    private static final String DATA_TABLE = "data-table";
    private static final String DATA_ID_FILE_PATH = "dataid-file-path";
    private static final String MAX_COUNT = "max";
    private static final String WORKER_COUNT = "worker-count";

    private static final Logger logger = LoggerFactory.getLogger(ResidueCommand.class);
    private String workPath;
    private String workspace;
    private String siteName;
    private String url;
    private String user;
    private String passwd;
    private String dataTable;
    private String dataIdFilePath;
    private int workerCount = 1;
    private int maxCount = 10000;

    @Override
    protected Options addParam() {
        Options ops = new Options();
        ops.addOption(Option.builder().longOpt(WORK_PATH).desc("residue check work path")
                .hasArg(true).required(true).build());
        ops.addOption(Option.builder().longOpt(WORKSPACE).desc("workspace name").hasArg(true)
                .required(true).build());
        ops.addOption(Option.builder().longOpt(SITE_NAME)
                .desc("site name, only support sequoiadb datasource").hasArg(true).required(true)
                .build());
        ops.addOption(Option.builder().longOpt(GATEWAY_URL)
                .desc("gateway url,eg:<ip>:<port>/<siteName>").hasArg(true).required(true).build());
        ops.addOption(Option.builder().longOpt(ADMIN_USER).desc("admin user name").hasArg(true)
                .required(true).build());
        ops.addOption(Option.builder().longOpt(ADMIN_USER_PASSWD).desc("admin user password")
                .optionalArg(true).hasArg(true).required(true).build());
        ops.addOption(Option.builder().longOpt(DATA_TABLE)
                .desc("data table name to be residue check, only support lob table name")
                .hasArg(true).required(false).build());
        ops.addOption(Option.builder().longOpt(DATA_ID_FILE_PATH)
                .desc("data id list file path to be residue check").hasArg(true).required(false)
                .build());
        ops.addOption(Option.builder().longOpt(MAX_COUNT)
                .desc("max count of rejected residue check, default:10000, max: 1000000")
                .hasArg(true).required(false).build());
        ops.addOption(Option.builder().longOpt(WORKER_COUNT)
                .desc("number of threads for concurrent residue check, default: 1,max: 100")
                .hasArg(true).required(false).build());
        return ops;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "data residue detection";
    }

    @Override
    public void run(String[] args) throws Exception {
        if (hasHelp(args)) {
            printHelp();
            return;
        }
        CommonUtils.configLog(
                ScmHelper.getPwd() + File.separator + "conf" + File.separator + "residue-log.xml");
        Options ops = addParam();
        CommandLine cl = new DefaultParser().parse(ops, args, false);
        logger.info(CommonUtils.buildCommandInfo(cl, getName(), ADMIN_USER_PASSWD));
        progress(cl);
    }

    private void progress(CommandLine cl) throws Exception {
        initCommand(cl);
        DataCheckRunner runner = new DataCheckRunner(workerCount);
        try {
            WorkPathConfig workPathConfig = WorkPathConfig.getInstance();
            workPathConfig.initWorkPath(workPath);
            ResidueProgress residueProgress = new ResidueProgress();
            // 初始化数据源管理类
            ScmDataSourceMgr.getInstance().init(workspace, url, user, passwd);

            checkSiteValid(siteName);
            long dataIdCount;
            ResidueCheckInfo info = new ResidueCheckInfo(workspace, siteName, residueProgress);
            System.out.println("please wait, the number of data ids is being counted");
            if (isHasDataIdFilePath(cl)) {
                File file = new File(dataIdFilePath);
                if (!file.isAbsolute()) {
                    dataIdFilePath = CommonUtils
                            .getNormalizationPath(workPathConfig.getUserWorkDir() + dataIdFilePath);
                    file = new File(dataIdFilePath);
                }
                if (!file.exists()) {
                    throw new ScmToolsException(
                            "data id file not exist,filePath: " + dataIdFilePath,
                            ScmExitCode.SYSTEM_ERROR);
                }
                dataIdCount = CommonUtils.getLineCount(dataIdFilePath);
                info.setDataIdFilePath(dataIdFilePath);
            }
            else {
                dataIdCount = getDataIdCount(siteName, dataTable);
                info.setDataTable(dataTable);
            }
            if (dataIdCount == 0) {
                System.out.println("data id count is 0, no need to residue check");
                return;
            }
            if (dataIdCount > maxCount) {
                throw new ScmToolsException(
                        "Reject this residue check,because data id count bigger than max count, dataIdCount:"
                                + dataIdCount + ", maxCount:" + maxCount,
                        ScmExitCode.SYSTEM_ERROR);
            }
            FileOperator operator = FileOperator.getInstance();
            operator.addFileResource(WorkPathConfig.getInstance().getResidueIdFilePath());
            operator.addFileResource(WorkPathConfig.getInstance().getResidueErrorFilePath());
            residueProgress.setTotalCount(dataIdCount);
            ResiduePrinter printer = new ResiduePrinter(info);
            printer.start();
            ResidueCheckTaskFactory taskFactory = new ResidueCheckTaskFactory(info);
            ExecutionContext context = new ExecutionContext();
            while (true) {
                Runnable task = taskFactory.createTask(context);
                if (null != task) {
                    if (context.isNormal()) {
                        context.addTask();
                        runner.submit(task);
                    }
                    else {
                        throw context.exception();
                    }
                }
                else {
                    break;
                }
            }
            while (context.getIncompleteTaskCount() > 0) {
                try {
                    Thread.sleep(500);
                }
                catch (Exception e) {
                    // ignore
                }
            }
            printer.finish();
        }
        finally {
            runner.close();
            FileOperator.getInstance().close();
            ScmDataSourceMgr.getInstance().releaseResource();
        }
    }

    private void initCommand(CommandLine cl) throws ScmToolsException {
        this.workPath = cl.getOptionValue(WORK_PATH);
        this.workspace = cl.getOptionValue(WORKSPACE);
        this.siteName = cl.getOptionValue(SITE_NAME);
        this.url = cl.getOptionValue(GATEWAY_URL);
        this.user = cl.getOptionValue(ADMIN_USER);
        this.passwd = cl.getOptionValue(ADMIN_USER_PASSWD);
        if (null == passwd) {
            System.out.print("password for " + user + ": ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }
        checkArgValid(cl);
    }

    private void checkSiteValid(String siteName) throws ScmToolsException {
        ScmSiteInfo siteInfo = ScmDataSourceMgr.getInstance().getSiteInfo(siteName);
        if (null == siteInfo) {
            throw new ScmToolsException("site not exist,siteName:" + siteName,
                    ScmExitCode.INVALID_ARG);
        }
        if (!siteInfo.getDataTypeStr()
                .equals(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR)) {
            throw new ScmToolsException("only support sequoiadb site, siteName: " + siteName
                    + ", site type:" + siteInfo.getDataTypeStr(), ScmExitCode.SYSTEM_ERROR);
        }
    }

    private long getDataIdCount(String siteName, String dataTable) throws ScmToolsException {
        SequoiadbDatasource sdbDs = ScmDataSourceMgr.getInstance().getSdbDatasource(siteName);
        Sequoiadb db = null;
        DBCursor cursor = null;
        try {
            String[] split = dataTable.split("\\.");
            String csName = split[0];
            String clName = split[1];
            db = sdbDs.getConnection();
            DBCollection cl = db.getCollectionSpace(csName).getCollection(clName);
            cursor = cl.listLobs();
            long count = 0;
            while (cursor.hasNext()) {
                count++;
                cursor.getNext();
            }
            return count;
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to get data id list", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(cursor);
            if (null != db) {
                sdbDs.releaseConnection(db);
            }
        }
    }

    @Override
    public void printHelp() {
        Options ops = addParam();
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", ops);
    }

    private void checkArgValid(CommandLine cl) throws ScmToolsException {
        boolean hasDataTable = isHasDataTable(cl);
        boolean hasDataIdFilePath = isHasDataIdFilePath(cl);

        if (hasDataTable && hasDataIdFilePath) {
            throw new ScmToolsException(
                    "data table and data id file path cannot exist at the same time, please choose one",
                    ScmExitCode.SYSTEM_ERROR);
        }

        if (!hasDataTable && !hasDataIdFilePath) {
            throw new ScmToolsException(
                    "data table and data id file path cannot be null at the same time, please choose one",
                    ScmExitCode.SYSTEM_ERROR);
        }

        if (hasDataTable) {
            String dataTable = cl.getOptionValue(DATA_TABLE);
            String[] split = dataTable.split("\\.");
            if (split.length != 2) {
                throw new ScmToolsException("Invalid data table name", ScmExitCode.INVALID_ARG);
            }
            String csName = split[0];
            String clName = split[1];
            if (csName.length() == 0 || clName.length() == 0) {
                throw new ScmToolsException("Invalid data table name", ScmExitCode.INVALID_ARG);
            }
            this.dataTable = dataTable;
        }
        else {
            this.dataIdFilePath = cl.getOptionValue(DATA_ID_FILE_PATH);
        }

        if (cl.hasOption(WORKER_COUNT)) {
            int workCount = Integer.parseInt(cl.getOptionValue(WORKER_COUNT));
            if (workCount < 1 || workCount > 100) {
                throw new ScmToolsException("worker count need [1,100]", ScmExitCode.INVALID_ARG);
            }
            this.workerCount = workCount;
        }

        if (cl.hasOption(MAX_COUNT)) {
            int maxCount = Integer.parseInt(cl.getOptionValue(MAX_COUNT));
            if (maxCount < 1 || maxCount > 100 * 10000) {
                throw new ScmToolsException("data id max count need [1,1000000]",
                        ScmExitCode.INVALID_ARG);
            }
            this.maxCount = maxCount;
        }
    }

    private boolean isHasDataTable(CommandLine cl) {
        return cl.hasOption(DATA_TABLE);
    }

    private boolean isHasDataIdFilePath(CommandLine cl) {
        return cl.hasOption(DATA_ID_FILE_PATH);
    }
}
