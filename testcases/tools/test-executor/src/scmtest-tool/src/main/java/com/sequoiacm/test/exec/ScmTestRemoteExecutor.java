package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.config.RemotePathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.module.TestTaskInfo;
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

    @Override
    public void updateTaskInfo(TestTaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    public ScmTestRemoteExecutor(TestTaskInfo taskInfo) throws IOException {
        super(taskInfo);
        TestNgXml testNgXml = taskInfo.getTestNgXml();
        ScmTestProjectMgr testProjectMgr = ScmTestProjectMgr.getInstance();
        ScmTestProject testProject = testProjectMgr.getTestProject(testNgXml.getProject());

        // 准备远程执行环境(目录预创建、Jar包拷贝、清楚残留的进度文件)
        Ssh ssh = null;
        try {
            ssh = SshMgr.getInstance().getSsh(taskInfo.getHostInfo());
            rmAndCreateRemoteWorkDir(ssh, RemotePathConfig.JARS_PATH, RemotePathConfig.TMP_PATH,
                    RemotePathConfig.OUTPUT_PATH, RemotePathConfig.TEST_OUTPUT_PATH);
            ssh.scpTo(LocalPathConfig.getListenerJarPath(), RemotePathConfig.JARS_PATH);
            ssh.scpTo(testProject.getJarPath(), RemotePathConfig.JARS_PATH);
        }
        finally {
            CommonUtil.closeResource(ssh);
        }
    }

    @Override
    public ExecResult call() throws Exception {
        Ssh ssh = null;
        ExecResult result;
        try {
            HostInfo hostInfo = taskInfo.getHostInfo();
            ssh = SshMgr.getInstance().getSsh(hostInfo);
            ssh.scpTo(taskInfo.getTestNgXml().getPath(), RemotePathConfig.TMP_PATH);

            LinkedHashMap<String, String> env = new LinkedHashMap<>();
            env.put("JAVA_HOME", hostInfo.getJavaHome());
            env.put("PATH", "$JAVA_HOME/bin:$PATH");
            result = ssh.exec(taskInfo.getExecCommand(), env);
        }
        finally {
            CommonUtil.closeResource(ssh);
        }
        return result;
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
            ssh.changeOwner(remoteDir, taskInfo.getHostInfo().getUser(), null);
        }
    }
}
