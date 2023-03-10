package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.module.WorkPath;
import com.sequoiacm.test.module.Worker;
import com.sequoiacm.test.project.ScmTestProject;
import com.sequoiacm.test.project.ScmTestProjectMgr;
import com.sequoiacm.test.project.TestNgXml;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;

public class ScmTestRemoteExecutor extends ScmTestExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestRemoteExecutor.class);

    @Override
    public TestTaskInfo getTaskInfo() {
        return taskInfo;
    }

    public ScmTestRemoteExecutor(TestTaskInfo taskInfo) throws IOException {
        super(taskInfo);
        TestNgXml testNgXml = taskInfo.getTestNgXml();
        ScmTestProjectMgr testProjectMgr = ScmTestProjectMgr.getInstance();
        ScmTestProject testProject = testProjectMgr.getTestProject(testNgXml.getProject());

        // 准备远程执行环境(目录预创建、Jar包拷贝)
        Ssh ssh = null;
        try {
            Worker worker = taskInfo.getWorker();
            ssh = SshMgr.getInstance().getSsh(worker.getHostInfo());

            WorkPath workPath = worker.getWorkPath();
            rmAndCreateRemoteWorkDir(ssh, workPath.getJarPath(), workPath.getTmpPath(),
                    workPath.getConsoleOutPath(), workPath.getTestOutputPath());
            ssh.scpTo(LocalPathConfig.getListenerJarPath(), workPath.getJarPath());
            ssh.scpTo(testProject.getJarPath(), workPath.getJarPath());
        }
        finally {
            CommonUtil.closeResource(ssh);
        }
    }

    @Override
    public ExecResult call() throws IOException {
        Ssh ssh = null;
        try {
            Worker worker = taskInfo.getWorker();
            HostInfo hostInfo = worker.getHostInfo();
            WorkPath workPath = worker.getWorkPath();
            // 1. 准备远程连接环境
            ssh = SshMgr.getInstance().getSsh(hostInfo);
            // 2. 清楚残余的进度文件
            rmAndCreateRemoteWorkDir(ssh, workPath.getTmpPath());
            // 3. 准备新的测试 XML
            ssh.scpTo(taskInfo.getTestNgXml().getPath(), workPath.getTmpPath());
            // 4. 远程执行命令
            LinkedHashMap<String, String> env = new LinkedHashMap<>();
            env.put("JAVA_HOME", hostInfo.getJavaHome());
            env.put("PATH", "$JAVA_HOME/bin:$PATH");
            return ssh.exec(taskInfo.getExecCommand(), env);
        }
        catch (Exception e) {
            logger.error("test run failed", e);
            throw e;
        }
        finally {
            CommonUtil.closeResource(ssh);
        }
    }

    private void rmAndCreateRemoteWorkDir(Ssh ssh, String... remoteDirList) throws IOException {
        for (String remoteDir : remoteDirList) {
            logger.debug("Clean up and create a remote directory, dir path={}", remoteDir);
            ExecResult execResult = ssh
                    .sudoExec("rm -rf " + remoteDir + " && mkdir -p " + remoteDir);
            if (execResult.getExitCode() != 0) {
                throw new IOException("Failed to remove and create the directory, remoteHost:"
                        + ssh.getHost() + ", path=" + remoteDir + ", exec result:" + execResult);
            }
            HostInfo hostInfo = taskInfo.getWorker().getHostInfo();
            ssh.changeOwner(remoteDir, hostInfo.getUser(), null);
        }
    }
}
