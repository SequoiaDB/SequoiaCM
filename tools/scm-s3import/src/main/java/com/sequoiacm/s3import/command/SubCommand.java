package com.sequoiacm.s3import.command;

import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.*;
import com.sequoiacm.s3import.config.ImportPathConfig;
import com.sequoiacm.s3import.config.ImportToolProps;
import com.sequoiacm.s3import.config.S3ClientManager;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.fileoperation.S3ImportFileLock;
import com.sequoiacm.s3import.fileoperation.S3ImportFileResource;
import com.sequoiacm.s3import.fileoperation.S3ImportResourceFactory;
import com.sequoiacm.s3import.module.S3Bucket;
import com.sequoiacm.s3import.module.S3ImportOptions;
import com.sequoiacm.s3import.module.WorkEnv;
import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.StringUtils;

import java.io.*;
import java.util.List;

public abstract class SubCommand {

    public abstract String getName();

    public abstract String getDesc();

    protected abstract void process(S3ImportOptions importOptions) throws ScmToolsException;

    public void run(String[] args) throws ScmToolsException {
        CommandLine cl = parseOptions(args, commandOptions());
        if (cl.hasOption(CommonDefine.Option.LONG_HELP)) {
            printHelp();
            return;
        }

        S3ImportOptions importOptions = parseCommandLineArgs(cl);

        ImportPathConfig pathConfig = ImportPathConfig.getInstance();
        pathConfig.initWorkPath(importOptions.getWorkPath());
        if (importOptions.getConfPath() != null) {
            pathConfig.setConfPath(importOptions.getConfPath());
        }

        File workEnvFile = new File(pathConfig.getWorkEnvFilePath());
        S3ImportFileResource resource = S3ImportResourceFactory.getInstance()
                .createFileResource(workEnvFile);
        S3ImportFileLock lock = resource.createLock();
        if (!lock.tryLock()) {
            throw new ScmToolsException("The working path is being used by another process",
                    S3ImportExitCode.WORK_PATH_CONFLICT);
        }
        try {
            changeLogOutputFile(pathConfig);
            InitWorkEnv(resource);
            checkAndInitBucketConf(importOptions.getBucketList());
            ProgressPrinter printer = new ProgressPrinter(getName(), importOptions.getBucketList());
            try {
                printer.start();
                process(importOptions);
            }
            catch (Exception e) {
                printer.stopPrint();
                throw e;
            }
            finally {
                try {
                    printer.join();
                }
                catch (InterruptedException e) {
                    throw new ScmToolsException("Failed to join print thread",
                            S3ImportExitCode.SYSTEM_ERROR);
                }
            }
        }
        finally {
            lock.unlock();
            resource.release();
        }
    }

    protected void checkAndInitBucketConf(List<S3Bucket> bucketList) throws ScmToolsException {
        S3ClientManager clientManager = S3ClientManager.getInstance();
        S3Utils.checkAndInitVersionControl(clientManager.getSrcS3Client(),
                clientManager.getDestS3Client(), bucketList);
    }

    protected Options commandOptions() throws ScmToolsException {
        Options ops = new Options();
        ops.addOption(Option.builder(CommonDefine.Option.SHORT_HELP)
                .longOpt(CommonDefine.Option.LONG_HELP).hasArg(false).build());
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.WORK_PATH).hasArg(true)
                .desc("the working path of this execution").build());
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.MAX_EXEC_TIME).hasArg(true)
                .desc("maximum time for this execution").build());
        ops.addOption(Option.builder().longOpt(CommonDefine.Option.CONF).hasArg(true)
                .desc("the custom configuration file path, sample:"
                        + new File(ImportPathConfig.getInstance().getConfPath()))
                .build());
        return ops;
    }

    protected S3ImportOptions parseCommandLineArgs(CommandLine cl) throws ScmToolsException {
        ArgUtils.checkRequiredOption(cl, CommonDefine.Option.WORK_PATH);
        return new S3ImportOptions(cl);
    }

    protected void printHelp() throws ScmToolsException {
        Options validOps = new Options();
        Options ops = commandOptions();
        // filter --help option
        for (Option option : ops.getOptions()) {
            if (StringUtils.equals(option.getLongOpt(), CommonDefine.Option.LONG_HELP)) {
                continue;
            }
            validOps.addOption(option);
        }
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", validOps);
    }

    private CommandLine parseOptions(String[] args, Options options) {
        try {
            return new DefaultParser().parse(options, args);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private void changeLogOutputFile(ImportPathConfig pathConfig) throws ScmToolsException {
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
                    S3ImportExitCode.FILE_NOT_FIND, e);
        }
        finally {
            CommonUtils.closeResource(is);
        }
    }

    private void InitWorkEnv(S3ImportFileResource fileResource) throws ScmToolsException {
        ImportToolProps toolProps = ImportToolProps.getInstance();
        WorkEnv lastWorkEnv = CommonUtils.parseJsonStr(fileResource.readFile(), WorkEnv.class);
        WorkEnv currentWorkEnv = new WorkEnv(toolProps.getSrcS3().getUrl(),
                toolProps.getDestS3().getUrl());
        // 配置文件不存在或内容为空
        if (lastWorkEnv == null) {
            fileResource.writeFile(CommonUtils.toJSONString(currentWorkEnv));
        }
        else if (!lastWorkEnv.equals(currentWorkEnv)) {
            throw new ScmToolsException(
                    "Inconsistent with the last execution environment, current env: "
                            + currentWorkEnv + ". last exec env: " + lastWorkEnv,
                    S3ImportExitCode.ENV_ERROR);
        }
    }
}
