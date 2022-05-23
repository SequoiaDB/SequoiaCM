package com.sequoiacm.mappingutil.command;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.common.*;
import com.sequoiacm.mappingutil.config.PathConfig;
import com.sequoiacm.mappingutil.config.ScmResourceMgr;
import com.sequoiacm.mappingutil.element.MappingOption;
import com.sequoiacm.mappingutil.element.WorkConf;
import com.sequoiacm.mappingutil.exception.ScmExitCode;
import com.sequoiacm.mappingutil.exec.*;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileLock;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScmFileMappingToolImpl extends ScmTool {

    private static final Logger logger = LoggerFactory.getLogger(ScmFileMappingToolImpl.class);

    public static String OPT_LONG_WORK_PATH = "work-path";
    public static String OPT_LONG_WORKSPACE = "workspace";
    public static String OPT_LONG_BUCKET = "bucket";
    public static String OPT_LONG_FILE_MATCHER = "file-matcher";
    public static String OPT_LONG_FILE_ID = "file-id";
    public static String OPT_LONG_KEY_TYPE = "key-type";

    public static String OPT_LONG_URL = "url";
    public static String OPT_LONG_USER = "user";
    public static String OPT_LONG_PASSWORD = "password";
    public static String OPT_LONG_PASSWORD_FILE = "password-file";

    public static String OPT_LONG_BATCH_SIZE = "batch-size";
    public static String OPT_LONG_ATTACH_SIZE = "attach-size";
    public static String OPT_LONG_MAX_FAILS = "max-fails";
    public static String OPT_LONG_THREAD = "thread";

    private Options options;
    private ScmHelpGenerator hp;

    public ScmFileMappingToolImpl() throws ScmToolsException {
        super("mapping");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt(null, OPT_LONG_WORK_PATH,
                "the working path of this execution", true, true, false));
        options.addOption(
                hp.createOpt(null, OPT_LONG_WORKSPACE, "workspace name.", true, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_BUCKET, "bucket name.", true, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_FILE_MATCHER,
                "file matching condition. all | 'jsonStr'", false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_FILE_ID, "the file path of the id list",
                false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_KEY_TYPE,
                "attach key type, all supported types: FILE_ID, FILE_NAME.", true, true, false));

        options.addOption(hp.createOpt(null, OPT_LONG_URL,
                "gateway url, eg:'host1:port/sitename,host2:port/sitename'.", true, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_USER, "login username.", true, true, false));
        options.addOption(
                hp.createOpt(null, OPT_LONG_PASSWORD, "login password.", true, false, false));
        options.addOption(hp.createOpt(null, OPT_LONG_PASSWORD_FILE, "login password file.", false,
                true, false));
        options.addOption(
                hp.createOpt(null, OPT_LONG_BATCH_SIZE, "batch size, default: 5000", false, true,
                        false));
        options.addOption(
                hp.createOpt(null, OPT_LONG_ATTACH_SIZE, "attach size, default: 50", false, true,
                        false));
        options.addOption(hp.createOpt(null, OPT_LONG_MAX_FAILS,
                "the maximum number of failures in this execution, default: 100", false, true,
                false));
        options.addOption(hp.createOpt(null, OPT_LONG_THREAD,
                "the maximum number of concurrent mappings, default: 20", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, options);
        MappingOption mappingOption = new MappingOption(cl);

        initToolsConfig(mappingOption);
        checkBucket(mappingOption);

        PathConfig pathConfig = PathConfig.getInstance();
        File workConfFile = new File(pathConfig.getWorkConfFilePath());
        ScmFileResource resource = ScmResourceFactory.getInstance()
                .createFileResource(workConfFile);
        ScmFileLock lock = null;
        try {
            lock = resource.createLock();
            if (!lock.tryLock()) {
                throw new ScmToolsException("Failed to lock file " + workConfFile.getName()
                        + ", The working path may be in use by another process, work path="
                        + pathConfig.getWorkPath()
                        + ". Please check the usage status of the current working path, or change to another path.",
                        ScmExitCode.WORK_PATH_CONFLICT);
            }
            checkAndInitWorkConf(resource, pathConfig, mappingOption);
            MappingProgress progress = generateMappingProgress();

            ProgressPrinter printer = new ProgressPrinter(progress);
            try {
                printer.start();
                doMapping(mappingOption, progress);
            }
            catch (Exception e) {
                printer.stopPrint();
                throw e;
            }
            finally {
                try {
                    printer.join();
                }
                catch (Exception e) {
                    logger.warn("Progress printing may be incomplete", e);
                }
            }
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
            resource.release();
        }
    }

    private void initToolsConfig(MappingOption option) throws ScmToolsException {
        String workPath = option.getWorkPath();
        PathConfig.getInstance().init(workPath);

        List<String> urlList = option.getUrlList();
        ScmUserInfo userInfo = option.getUserInfo();
        ScmResourceMgr.getInstance().init(urlList, userInfo);
    }

    private void checkBucket(MappingOption option) throws ScmToolsException {
        ScmSession session = null;
        ScmBucket bucket;
        try {
            session = ScmResourceMgr.getInstance().getSession();
            bucket = ScmFactory.Bucket.getBucket(session, option.getBucket());
        }
        catch (ScmException e) {
            throw new ScmToolsException("Failed to get bucket", ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(session);
        }
        CommonUtils.assertTrue(bucket.getWorkspace().equals(option.getWorkspace()),
                "The workspace of the bucket is:" + bucket.getWorkspace() + ", current workspace:"
                        + option.getWorkspace());
    }

    private void doMapping(MappingOption mappingOption, MappingProgress progress)
            throws ScmToolsException {
        if (progress.isFinish()) {
            return;
        }

        ExecutorService threadPool = null;
        FileIdCursor cursor = null;
        try {
            threadPool = Executors.newFixedThreadPool(mappingOption.getThread());
            cursor = generateCursor(mappingOption, progress);
            List<FileMappingTask> taskList;
            while (true) {
                taskList = getNextBatch(cursor, progress, mappingOption);
                if (taskList.size() == 0) {
                    progress.setStatus(CommonDefine.MappingStatus.FINISH);
                    FileOperateUtils.updateProgress(progress);
                    break;
                }

                List<Future> mappingResultList = new ArrayList<>();
                for (FileMappingTask task : taskList) {
                    mappingResultList.add(threadPool.submit(task));
                }

                boolean hasAbortedTask = false;
                for (Future future : mappingResultList) {
                    try {
                        future.get();
                    }
                    catch (Exception e) {
                        logger.error("Failed to get mapping result", e);
                        hasAbortedTask = true;
                    }
                }
                if (hasAbortedTask) {
                    throw new ScmToolsException("Exist abnormal mapping task",
                            ScmExitCode.SYSTEM_ERROR);
                }

                progress.setMarker(cursor.getMarker());
                FileOperateUtils.appendErrorKeyList(progress);
                FileOperateUtils.updateProgress(progress);
                CommonUtils.checkFailCount(progress.getProcessErrorCount(),
                        mappingOption.getMaxFails());
            }
        }
        finally {
            ScmCommon.closeResource(cursor);
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    private List<FileMappingTask> getNextBatch(FileIdCursor cursor, MappingProgress progress,
            MappingOption option) throws ScmToolsException {
        List<FileMappingTask> res = new ArrayList<>();

        int cnt = 0;
        List<ScmId> tmpIdList = new ArrayList<>();
        while (cursor.hasNext() && cnt++ < option.getBatchSize()) {
            tmpIdList.add(cursor.getNext());
            if (tmpIdList.size() == option.getAttachSize()) {
                res.add(new FileMappingTask(progress, option, tmpIdList));
                tmpIdList = new ArrayList<>();
            }
        }

        if (tmpIdList.size() > 0) {
            res.add(new FileMappingTask(progress, option, tmpIdList));
        }
        return res;
    }

    private void checkAndInitWorkConf(ScmFileResource resource, PathConfig pathConfig,
            MappingOption mappingOption) throws ScmToolsException {
        WorkConf lastWorkConf = CommonUtils.parseJsonStr(resource.readFile(), WorkConf.class);
        WorkConf currentWorkConf = new WorkConf(mappingOption);
        // 配置文件不存在或内容为空
        if (lastWorkConf == null) {
            resource.writeFile(CommonUtils.toJsonString(currentWorkConf));
        }
        else if (!lastWorkConf.equals(currentWorkConf)) {
            throw new ScmToolsException("Inconsistent with the last execution conf, current conf: "
                    + currentWorkConf + ". last exec conf: " + lastWorkConf,
                    ScmExitCode.WORK_CONF_ERROR);
        }

        changeLogOutputFile(pathConfig);
    }

    private void changeLogOutputFile(PathConfig pathConfig) throws ScmToolsException {
        String logConfFilePath = pathConfig.getLogConfFilePath();
        File logConfFile = new File(logConfFilePath);
        if (!logConfFile.exists()) {
            FileOperateUtils.copyLogXml2WorkPath(logConfFile);
        }

        InputStream is = null;
        try {
            is = new FileInputStream(logConfFile);
            ScmHelper.configToolsLog(is);
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException("The logback xml file does not exist",
                    ScmExitCode.FILE_NOT_FIND, e);
        }
        finally {
            ScmCommon.closeResource(is);
        }
    }

    private MappingProgress generateMappingProgress() throws ScmToolsException {
        MappingProgress mappingProgress;

        String progressFilePath = PathConfig.getInstance().getProgressFilePath();
        File progressFile = new File(progressFilePath);
        if (!progressFile.exists()) {
            mappingProgress = new MappingProgress();
            FileOperateUtils.updateProgress(mappingProgress);
            return mappingProgress;
        }

        ScmFileResource resource = ScmResourceFactory.getInstance()
                .createFileResource(progressFile);
        try {
            mappingProgress = CommonUtils.parseJsonStr(resource.readLine(), MappingProgress.class);
            return mappingProgress;
        }
        finally {
            resource.release();
        }
    }

    private FileIdCursor generateCursor(MappingOption mappingOption, MappingProgress progress)
            throws ScmToolsException {
        String idFilePath = mappingOption.getIdFilePath();
        if (idFilePath != null) {
            return new FileIdCursorFromLocalFile(idFilePath, progress.getMarker());
        }
        return new FileIdCursorFromFileMatcher(mappingOption.getWorkspace(),
                mappingOption.getFileMatcher(), progress.getMarker());
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}

class FileMappingTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FileMappingTask.class);

    private MappingProgress progress;
    private String bucketName;
    private ScmBucketAttachKeyType keyType;
    private List<ScmId> fileIds;

    public FileMappingTask(MappingProgress progress, MappingOption option, List<ScmId> idList) {
        this.progress = progress;
        this.bucketName = option.getBucket();
        this.keyType = option.getKeyType();
        this.fileIds = idList;
    }

    @Override
    public void run() {
        ScmSession session = null;
        try {
            List<ScmBucketAttachFailure> failures = null;
            try {
                session = ScmResourceMgr.getInstance().getSession();
                failures = ScmFactory.Bucket.attachFile(session, bucketName, fileIds, keyType);
            }
            catch (Exception e) {
                logger.error("Failed to attach file", e);
                progress.addErrorKeys(fileIds);
                progress.error(fileIds.size());
                return;
            }

            for (ScmBucketAttachFailure failure : failures) {
                ScmError error = failure.getError();
                if (error.equals(ScmError.METASOURCE_RECORD_EXIST)
                        || error.equals(ScmError.FILE_IN_ANOTHER_BUCKET)
                        || error.equals(ScmError.FILE_NOT_FOUND)) {
                    progress.addUnAttachableKey(failure);
                }
                else {
                    progress.addErrorKey(failure.getFileId());
                }
                logger.warn("Failed to attach file, id={}, cause by={}, error_code={}",
                        failure.getFileId(), failure.getMessage(), failure.getError());
            }
            progress.success(fileIds.size() - failures.size());
            progress.error(failures.size());
        }
        finally {
            ScmCommon.closeResource(session);
        }
    }
}
