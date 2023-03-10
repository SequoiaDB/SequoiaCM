package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.BashUtil;
import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.module.WorkPath;
import com.sequoiacm.test.project.ScmTestProject;
import com.sequoiacm.test.project.ScmTestProjectMgr;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ScmTestLocalExecutor extends ScmTestExecutor {

    public ScmTestLocalExecutor(TestTaskInfo taskInfo) throws IOException {
        super(taskInfo);
        String project = taskInfo.getTestNgXml().getProject();
        ScmTestProject testProject = ScmTestProjectMgr.getInstance().getTestProject(project);
        // 准备执行环境（目录预创建、Jar 包拷贝）
        WorkPath workPath = taskInfo.getWorker().getWorkPath();
        CommonUtil.cleanOrInitDir(workPath.getJarPath(), workPath.getTmpPath(),
                workPath.getConsoleOutPath(), workPath.getTestOutputPath());

        File jarDir = new File(workPath.getJarPath());
        FileUtils.copyFileToDirectory(new File(LocalPathConfig.getListenerJarPath()), jarDir);
        FileUtils.copyFileToDirectory(new File(testProject.getJarPath()), jarDir);
    }

    @Override
    public ExecResult call() throws Exception {
        WorkPath workPath = taskInfo.getWorker().getWorkPath();
        // 1. 清楚残余的进度文件
        CommonUtil.cleanOrInitDir(workPath.getTmpPath());
        // 2. 准备新的测试 XML
        FileUtils.copyFileToDirectory(new File(taskInfo.getTestNgXml().getPath()),
                new File(workPath.getTmpPath()));
        // 3. 执行测试用例
        return BashUtil.exec(taskInfo.getExecCommand());
    }
}
