# SCM本地构建化
目的：用于开发人员快速编译，测试，执行用例

## 1 前提准备
   * Python版本:2.7.X
   * 系统:Linux或Windows
   * 所需部署安装虚拟机相互配置好 host 映射

## 2 使用介绍
  在Linux或Windows中进行:
### 2.1. 环境要求:

Python版本:2.7.X

### 2.2安装paramiko库

   * 目的：安装Python paramiko 外部库进行本地化构建ssh远程连接
   * 方式：离线安装
   * Python版本:2.7.X
   * 账号:root 密码:sequoiadb

**(1)在需要部署集群的Linux机器上(必须)(脚本安装)**

拷贝./localbuild/install_paramiko/Linux文件夹至对应Linux 机器上,之后在其机器上安装expect,最后在其目录下执行脚本即可;
```shell
bash install_paramiko.sh --host 192.168.XX.XX,192.168.XX.XX
```

注意:对于麒麟系统机器或者账号密码不一致情况安装Python Paramiko 需要单独在其机器下拷贝Linux文件夹在/usr/local/目录下，之后执行命令:
```shell
bash start.sh 
```

**(2)若项目在windows上(手动安装):**

1. 拷贝SCM项目下localbuild\install_parasmiko\windows下所有tar包和应用程序到C:\Python27\Tools
2. 解压ecdsa-0.10.tar包至Tools文件夹,解压后进入其目录cmd执行命令:python setup.py install
3. 成功完成步骤2后,双击pycrypto-2.6.win-amd64-py2.7应用程序进行安装
4. 成功完成步骤3后,解压paramiko-1.13.0.tar至Tools文件夹，解压后进入其目录cmd执行命令:python setup.py install
5. 成功完成步骤4后,进行检验:创建任意python文件,引入import paramiko后执行,若无对应相关报错，说明引入paramiko成功。

### 2.3填写配置信息和安装包存放位置

 * ./localbuild/conf/localbuild.conf ssh主机配置文件
 * ./localbuild/temp_package/sequoiadb-xx-installer.run SDB安装包存放位置

### 2.4主脚本:localbuild.py参数解析

```shell
参数 | 含义
--- | :---:   
--compile                            |仅编译
--installsdb                         |安装SDB集群
--installsdb --force                 |强制安装SDB集群
--installscm                         |安装部署SCM集群
--installscm --force                 |强制安装SCM集群
--runtest                            |执行基本测试用例
-h | --help                          |使用帮助
--host <arg>                         |后面配置主机号,逗号分隔 
--cleanscm                           |卸载SCM集群
--cleanws                            |清理工作区
```

### 2.5操作命令
   * 一键编译测试部署安装：
   ```shell
   python localbuild.py --compile --installsdb --installscm  --runtest  --host  192.168.XX.XX,192.168.XX.XX    
   ```

***需在./localbuild/temp_package/下放置SDB安装包文件***

   * 仅编译
   
   ```shell
   python localbuild.py --compile   
   ```
   * 仅安装sdb集群
   ```shell
    python localbuild.py --installsdb    --host 192.168.XX.XX    
   ```

***需在./localbuild/temp_package/下放置SDB安装包文件，安装两套单组单节点SDB集群***

   * 强制安装sdb集群
   ```shell
   python localbuild.py --installsdb    --host 192.168.XX.XX  --force     
   ```
   * 仅安装scm集群
   ```shell
   python localbuild.py  --installscm  --host 192.168.XX.XX,192.168.XX.XX     
   ```
   * 强制安装scm集群
   ```shell
   python localbuild.py  --installscm  --host 192.168.XX.XX,192.168.XX.XX --force      
   ```

   ***安装SCM集群后其安装包解压位置位于./localbuild/temp_package/sequoiacm***

   * 卸载scm集群
   ```shell
  python localbuild.py  --cleanscm  --host 192.168.XX.XX,192.168.XX.XX     
   ```
   * 仅执行基本测试用例
   ```shell
   python localbuild.py  --runtest      
   ```

   * 仅清理全部工作区
   ```shell
   python localbuild.py  --cleanws      
   ```

## 3 工作目录
```shell
|--localbuild
              |--conf
                          |--localbuild.conf  #填写主机ssh相关信息（必填）
                          |-- deployscmHostX_template #部署SCM模板文件（可根据实际需求修改）
              |temp_package #主要存放脚本生成文件和必要安装包
                          |sequoiadb-3.2.4-linux_x86_64-installer.run #SDB安装包存放位置
                          |test-report # 测试报告存放位置
                          |deployscmHostX_template.cfg #实际部署SCM文件
                          |paramiko.log # paramiko 连接日志
                          |scm.info # 部署scm集群后生成信息
                          |sdb.info # 部署sdb集群后生成信息
                          |workspace_template.json # 创建工作区文件
                          |testng.xml # test-case-story并行xml文件
                          |test-serial.xml #test-case-story串行行xml文件
                          |sequoicm-X.X.X-release.tar.gz # SCM安装包位置
                          |sequoicm #scm安装包解压后文件
```
## 3 附属脚本介绍
### 3.1 compile.py
编译SCM项目
```shell
python  compile.py --package-path <arg>
# package-path <arg> # 编译后安装包存放位置
```
### 3.2 deploy_sdb.py
安装部署SDB集群
```shell
python deploy_sdb.py --package-file <arg> --host <arg> --template <arg> --output <arg> --ssh-file <arg> --force
# package-file <arg> # sdb安装存放位置
# host <arg>         # 主机名，逗号分隔
# template <arg>     # sdb集群部署模板
# output  <arg>      # 输出部署完sdb集群生成信息存放位置,.info文件 具体参考sdb.info文件
# ssh-file <arg>     # 填写主机ssh相关信 具体参考./localbuild/conf/localbuild.conf
# force              # 是否强制安装
```   
### 3.3 deploy_scm.py
安装部署SCM集群
```shell
python deploy_scm.py  --package-file <arg> --host <arg> --template <arg>  --sdb-info <arg> --output <arg> --ssh-file <arg> --force
# package-file <arg> # scm安装包存放位置
# host <arg>         # 主机名，逗号分隔
# template <arg>     # sdb集群部署模板
# sdb-info <arg>     # 部署完sdb集群生成信息存放位置,.info文件 具体参考sdb.info文件
# output  <arg>      # 输出部署完scm集群生成信息存放位置,.info文件 具体参考scm.info文件
# ssh-file <arg>     # 填写主机ssh相关信 具体参考./localbuild/conf/localbuild.conf
# force              # 是否强制安装
```   
### 3.4 run_test.py
执行基本测试用例
```shell
python run_test.py --scm-info <arg> --workspace-file <arg>
# scm-info <arg>              # 部署完scm集群生成信息存放位置,.info文件 具体参考scm.info文件
# workspace-file   <arg>      # 工作区创建模板,具体参考./localbuild/conf/workspace_template.json
# ssh-file <arg>              # 填写主机ssh相关信 具体参考./localbuild/conf/localbuild.conf
```   

### 3.5 clean_scm.py
清理SCM集群环境
```shell
python clean_scm.py --host <arg>  --ssh-file <arg>
# host <arg>                  # 主机名，逗号分隔
# ssh-file <arg>              # 填写主机ssh相关信 具体参考./localbuild/conf/localbuild.conf
```   

### 3.6 clean_ws.py
清理工作区
```shell
python clean_scm.py --scm-info <arg>  --workspace-file <arg>
# scm-info <arg>              # 部署完sc,集群生成信息存放位置,.info文件 具体参考scm.info文件
# workspace-file   <arg>      # 工作区创建模板,具体参考./localbuild/conf/workspace_template.json
```

## 4 附属生成文件(部分介绍)
### 4.1 sdb.info
```shell
[cluster1]
coord = XX.XX.XX.XX:XX,

[cluster2]
coord = XX.XX.XX.XX:XX,
```
### 4.1 scm.info
```shell
mainSdbUrl=XX.XX.XX.XX:XX  # 192.168.XX.XX:11810
getWayUrl=XX.XX.XX.XX:XX   # 192.168.XX.XX:8080
S3Url=XX.XX.XX.XX:XX       # 192.168.XX.XX:16000
sshHostInfo=root,sequoiadb # 用来保存需要生成s3key机器的ssh用户密码信息
```