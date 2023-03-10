# SCM 测试执行工具

SCM 测试执行工具可供 CI 与开发人员统一使用，方便维护以及开发人员本地重现 CI 测试流程

## 1 项目目录结构

```shell
|--executor               # 编译产物
|--script                 
  |--scmtest.properties   # 配置文件（测试执行机、SCM集群的相关配置……）
  |--scmtest.py           # 入口脚本
  |--scmtest-project.cfg  # 测试工程描述文件
|--src                    # 源码目录
|--dev.py                 # 编译脚本
|--README.md              # 使用手册
```

## 2 编译

编译打包测试工具

```shell
python dev.py
```

编译产物介绍

```shell
|--conf           # 配置文件
|--jars           # 测试执行工具、监听器 Jar 包
|--log            # 测试工具自身日志输出
|--output         # 测试用例控制台输出
|--test-output    # 测试报告
|--tmp            # 测试工具产生的临时文件（切分后的 XML、本地测试进度）
|--scmtest.py     # 工具入口脚本
```

远端执行机工作目录

```shell
|--jars           # 测试执行工具、监听器 Jar 包
|--output         # 测试用例控制台输出
|--test-output    # 测试报告
|--tmp            # 测试工具产生的临时文件（切分后的 XML、本地测试进度）
```

## 3 执行

3.1 指定工程测试

```shell
python scmtest.py runtest --project story --sites fourSite
```

3.2 指定 testng.xml 测试

```shell
python scmtest.py runtest --project story --testng-conf testng_env_before --sites fourSite
```

3.3 指定包测试

```shell
python scmtest.py runtest --project story --testng-conf testng --packages com.sequoiacm.asynctask,com.sequoiacm.asynctask.concurrent --sites fourSite
```

runtest 参数介绍与限制

```shell
--project        #指定测试工程（必填、唯一）
--testng-conf    #指定具体的 testng 的 xml 模板（逗号分隔）
--packages       #指定需要执行的包（逗号分隔，依赖于 --testng-conf）
--classess       #指定需要执行的类（逗号分隔，依赖于 --testng-conf）
--sites          #指定SCM集群的站点数
--thread         #指定并发用例的线程数（对串行用例不生效）
--nocompile      #本次运行不编译指定的测试工程（project）
--help           #打印帮助信息
```