package com.sequoiacm.diagnose.command;

import com.sequoiacm.diagnose.common.CheckLevel;
import com.sequoiacm.diagnose.common.CompareInfo;
import com.sequoiacm.diagnose.common.FileOperator;
import com.sequoiacm.diagnose.config.WorkPathConfig;
import com.sequoiacm.diagnose.datasource.ScmDataSourceMgr;
import com.sequoiacm.diagnose.printer.ComparePrinter;
import com.sequoiacm.diagnose.progress.CompareProgress;
import com.sequoiacm.diagnose.task.CompareTaskFactory;
import com.sequoiacm.diagnose.task.DataCheckRunner;
import com.sequoiacm.diagnose.task.ExecutionContext;
import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Command
public class CompareCommand extends SubCommand {
    private static final String NAME = "compare";
    private static final String WORK_PATH = "work-path";
    private static final String WORKSPACE = "workspace";
    private static final String BEGIN_TIME = "begin-time";
    private static final String END_TIME = "end-time";
    private static final String GATEWAY_URL = "url";
    private static final String ADMIN_USER = "user";
    private static final String ADMIN_USER_PASSWD = "passwd";
    private static final String LEVEL = "check-level";
    private static final String FULL = "full";
    private static final String WORKER_COUNT = "worker-count";
    private static final Logger logger = LoggerFactory.getLogger(CompareCommand.class);
    private String workPath;
    private String workspace;
    private String beginTimeStr;
    private String endTimeStr;
    private String url;
    private String user;
    private String passwd;
    private CheckLevel checkLevel = CheckLevel.MD5;
    private int workerCount = 1;
    private boolean full = false;

    @Override
    protected Options addParam() {
        Options ops = new Options();
        ops.addOption(Option.builder().longOpt(WORK_PATH).desc("compare check work path")
                .hasArg(true).required(true).build());
        ops.addOption(Option.builder().longOpt(WORKSPACE).desc("workspace name").hasArg(true)
                .required(true).build());
        ops.addOption(Option.builder().longOpt(BEGIN_TIME)
                .desc("file's meta create_time of begin time to filter,eg: <yyyyMMdd>").hasArg(true)
                .required(true).build());
        ops.addOption(Option.builder().longOpt(END_TIME)
                .desc("file's meta create_time of end time to filter,eg: <yyyyMMdd>").hasArg(true)
                .required(true).build());
        ops.addOption(Option.builder().longOpt(GATEWAY_URL)
                .desc("gateway url,eg:<ip>:<port>/<siteName>").hasArg(true).required(true).build());
        ops.addOption(Option.builder().longOpt(ADMIN_USER).desc("admin user name").hasArg(true)
                .required(true).build());
        ops.addOption(Option.builder().longOpt(ADMIN_USER_PASSWD).desc("admin user password")
                .optionalArg(true).hasArg(true).required(true).build());
        ops.addOption(Option.builder().longOpt(LEVEL)
                .desc("data check level:1(size:compare data size)，2（md5:compare data size and md5）")
                .hasArg(true).required(false).build());
        ops.addOption(Option.builder().longOpt(FULL)
                .desc("compare record output same result,false:no output,true:output,default:false")
                .hasArg(true).required(false).build());
        ops.addOption(Option.builder().longOpt(WORKER_COUNT)
                .desc("number of threads for concurrent compare check, default: 1,max: 100")
                .hasArg(true).required(false).build());
        return ops;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "data consistency detection";
    }

    @Override
    public void run(String[] args) throws Exception {
        if (hasHelp(args)) {
            printHelp();
            return;
        }
        CommonUtils.configLog(
                ScmHelper.getPwd() + File.separator + "conf" + File.separator + "compare-log.xml");
        Options ops = addParam();
        CommandLine cl = new DefaultParser().parse(ops, args, false);
        // 输出命令信息
        logger.info(CommonUtils.buildCommandInfo(cl, getName(), ADMIN_USER_PASSWD));
        progress(cl);
    }

    private void progress(CommandLine cl) throws Exception {
        initCommand(cl);
        DataCheckRunner runner = new DataCheckRunner(workerCount);
        try {
            WorkPathConfig workPathConfig = WorkPathConfig.getInstance();
            workPathConfig.initWorkPath(workPath);
            ScmDataSourceMgr.getInstance().init(workspace, url, user, passwd);

            CompareProgress progress = new CompareProgress();
            long fileCount = getFileCount(workspace, beginTimeStr, endTimeStr);
            if (fileCount == 0) {
                System.out.println("file count is 0, no need to compare");
                return;
            }
            progress.setTotalCount(fileCount);
            CompareInfo compareInfo = new CompareInfo(workspace, beginTimeStr, endTimeStr, progress,
                    checkLevel, full);
            // 初始化文件头
            FileOperator operator = FileOperator.getInstance();
            operator.addFileResource(WorkPathConfig.getInstance().getCompareResultFilePath());
            operator.write2File(WorkPathConfig.getInstance().getCompareResultFilePath(),
                    getCompareFileHeader());
            if (checkLevel == CheckLevel.MD5) {
                operator.addFileResource(WorkPathConfig.getInstance().getNullMd5FilePath());
                operator.write2File(WorkPathConfig.getInstance().getNullMd5FilePath(),
                        getNullMd5FileHeader());
            }
            ComparePrinter printer = new ComparePrinter(compareInfo);
            printer.start();
            CompareTaskFactory factory = new CompareTaskFactory(compareInfo);
            ExecutionContext context = new ExecutionContext();
            while (true) {
                Runnable task = factory.createTask(context);
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

    private long getFileCount(String ws, String begin, String end)
            throws InterruptedException, ScmToolsException {
        SequoiadbDatasource metaSdbDs = ScmDataSourceMgr.getInstance().getMetaSdbDs();
        Sequoiadb db = null;
        try {
            BSONObject matcher = CommonUtils.getMatcher(begin, end);
            logger.info("file filter matcher:{}", matcher);
            db = metaSdbDs.getConnection();
            DBCollection fileCl = db.getCollectionSpace(ws + "_META").getCollection("FILE");
            long fileCount = fileCl.getCount(matcher);
            DBCollection fileHistoryCl = db.getCollectionSpace(ws + "_META")
                    .getCollection("FILE_HISTORY");
            long historyCount = fileHistoryCl.getCount(matcher);
            return fileCount + historyCount;
        }
        finally {
            if (null != db) {
                metaSdbDs.releaseConnection(db);
            }
        }
    }

    private void initCommand(CommandLine cl) throws ScmToolsException {
        this.workPath = cl.getOptionValue(WORK_PATH);
        this.workspace = cl.getOptionValue(WORKSPACE);
        this.beginTimeStr = cl.getOptionValue(BEGIN_TIME);
        this.endTimeStr = cl.getOptionValue(END_TIME);
        this.url = cl.getOptionValue(GATEWAY_URL);
        this.user = cl.getOptionValue(ADMIN_USER);
        this.passwd = cl.getOptionValue(ADMIN_USER_PASSWD);
        if (null == passwd) {
            System.out.print("password for " + user + ": ");
            passwd = ScmCommandUtil.readPasswdFromStdIn();
        }
        checkArgValid(cl);
    }

    private String getCompareFileHeader() {
        StringBuilder builder = new StringBuilder().append("file_id").append(",")
                .append("site_name").append(",").append("major_version").append(",")
                .append("minor_version").append(",").append("resultType").append(",")
                .append("detail");
        return builder.toString();
    }

    private String getNullMd5FileHeader() {
        StringBuilder builder = new StringBuilder().append("file_id").append(",")
                .append("major_version").append(",").append("minor_version");
        return builder.toString();
    }

    private void checkArgValid(CommandLine cl) throws ScmToolsException {
        // check time valid
        String beginTimeStr = cl.getOptionValue(BEGIN_TIME);
        String endTimeStr = cl.getOptionValue(END_TIME);
        Date beginDate = formatTime(beginTimeStr);
        Date endDate = formatTime(endTimeStr);
        if (beginDate.compareTo(endDate) > 0) {
            throw new ScmToolsException(
                    "the time range is valid,beginTime:" + beginTimeStr + ", endTime:" + endTimeStr,
                    ScmExitCode.INVALID_ARG);
        }

        if (cl.hasOption(WORKER_COUNT)) {
            int workCount = Integer.parseInt(cl.getOptionValue(WORKER_COUNT));
            if (workCount < 1 || workCount > 100) {
                throw new ScmToolsException("worker count need [1,100]", ScmExitCode.INVALID_ARG);
            }
            this.workerCount = workCount;
        }

        if (cl.hasOption(LEVEL)) {
            String levelStr = cl.getOptionValue(LEVEL);
            int level;
            try {
                level = Integer.parseInt(levelStr);
            }
            catch (Exception e) {
                throw new ScmToolsException(
                        "Invalid check level,please choose 1 or 2,checkLevel:" + levelStr,
                        ScmExitCode.INVALID_ARG, e);
            }
            CheckLevel type = CheckLevel.getType(level);
            if (type == CheckLevel.UNKNOWN) {
                throw new ScmToolsException(
                        "Invalid check level,please choose 1 or 2,checkLevel:" + levelStr,
                        ScmExitCode.INVALID_ARG);
            }
            this.checkLevel = type;
        }

        if (cl.hasOption(FULL)) {
            String full = cl.getOptionValue(FULL).toLowerCase();
            if (!full.equals("true") && !full.equals("false")) {
                throw new ScmToolsException(
                        "Invalid argument full ,please choose true or false,full:" + full,
                        ScmExitCode.INVALID_ARG);
            }
            this.full = full.equals("true");
        }
    }

    private Date formatTime(String time) throws ScmToolsException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setLenient(false);
        try {
            return dateFormat.parse(time);
        }
        catch (ParseException e) {
            throw new ScmToolsException(
                    "can not parse time to date,please input \"yyyyMMdd\",eg: 20230301.time:"
                            + time,
                    ScmExitCode.INVALID_ARG, e);
        }
    }

    @Override
    public void printHelp() {
        Options ops = addParam();
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", ops);
    }
}
