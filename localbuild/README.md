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

--host参数说明，后面接所需要安装部署主机名，逗号分隔(必填):

其脚本主要作用是在填写的主机中安装paramiko外部库，修改/etc/ssh/sshd_config 修改配置文件，在/etc/environment中添加JAVA_HOME

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
* ./localbuild/tmp/sequoiadb-xx-installer.run SDB安装包存放位置

### 2.4主脚本:localbuild.py参数解析

```shell
参数 | 含义
--- | :---:   
--compile                            |仅编译
--installsdb                         |安装SDB集群
--installsdb --force                 |强制安装SDB集群
--installscm                         |安装部署SCM集群
--installscm --force                 |强制安装SCM集群
--runtest                            |执行集成测试用例
--runut                              |执行单元测试用例
--statistical-cov                    |统计代码覆盖率
-h | --help                          |使用帮助
--host <arg>                         |后面配置主机号,逗号分隔 
--cleanscm                           |卸载SCM集群
--site                               |站点数，支持两站点，四站点 (适用于installsdb, installscm, runtest)
--project                            |测试工程，支持story, tdd, sdv, all (仅适用于runtest)
--runbase                            |仅执行基本测试用例 (仅适用于runtest)
```

### 2.5操作命令
* 一键编译测试部署安装：
   ```shell
   python localbuild.py --compile --installsdb --installscm  --runtest --runut --statistical-cov --host  192.168.XX.XX,192.168.XX.XX  --site twoSite --runbase  
   ```

***需在./localbuild/tmp/下放置SDB安装包文件***

* 仅编译

   ```shell
   python localbuild.py --compile   
   ```
* 仅安装sdb集群
   ```shell
    python localbuild.py --installsdb  --host 192.168.XX.XX --site twoSite 
   ```

***需在./localbuild/tmp/下放置SDB安装包文件，twoSite代表安装两套单组单节点SDB集群***

* 强制安装sdb集群
   ```shell
   python localbuild.py --installsdb    --host 192.168.XX.XX --site twoSite --force     
   ```
* 安装sdb集群(4套单组单节点)
   ```shell
   python localbuild.py --installsdb    --host 192.168.XX.XX  --site fourSite      
   ```
* 仅安装scm集群
   ```shell
   python localbuild.py  --installscm  --host 192.168.XX.XX,192.168.XX.XX --site twoSite    
   ```
* 强制安装scm集群
   ```shell
   python localbuild.py  --installscm  --host 192.168.XX.XX,192.168.XX.XX --site twoSite --force      
   ```

***安装SCM集群后其安装包解压位置位于./localbuild/tmp/sequoiacm***
* 安装scm集群(4站点集群)
   ```shell
   python localbuild.py  --installscm  --host 192.168.XX.XX,192.168.XX.XX --site fourSite    
   ```
* 卸载scm集群
   ```shell
  python localbuild.py  --cleanscm  --host 192.168.XX.XX,192.168.XX.XX     
   ```
* 仅执行基本测试用例(2个站点)
   ```shell
   python localbuild.py  --runtest --host 192.168.XX,XX,192.168,XX,XX --site twoSite --runbase    
   ```
* 仅执行测试用例(4个站点)
   ```shell
   python localbuild.py  --runtest --host 192.168.XX,XX,192.168,XX,XX --site fourSite   
   ```
***执行测试用例参数可以特殊指定一个站点即：--site oneSite***

## 3 工作目录
```shell
|--localbuild
              |--conf
                          |--localbuild.conf  #填写主机ssh相关信息（必填）
                          |-- deployscmHostX_template #部署SCM模板文件（可根据实际需求修改）
              |--tmp #主要存放脚本生成文件和必要安装包
                          |--statistical_cov  #执行覆盖率统计的中间产物、报告存放位置
                          |sequoiadb-3.2.4-linux_x86_64-installer.run #SDB安装包存放位置
                          |deployscmHostX_template.cfg #实际部署SCM文件
                          |paramiko.log # paramiko 连接日志
                          |scm.info # 部署scm集群后生成信息
                          |sdb.info # 部署sdb集群后生成信息
                          |sequoicm-X.X.X-release.tar.gz # SCM安装包位置
                          |sequoicm #scm安装包解压后文件
                          |localbuild.log # 日志文件
                          |test_executor # 测试工具工作目录
              |--test_executor # 测试工具目录
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
执行测试用例
```shell
python run_test_tool.py --scm-info <arg>  ssh-file <arg> --project all --site fourSite 
# 跑多个测试工程：表示执行四个站点tdd,story, sdv串并行所有用例
python run_test_tool.py --scm-info <arg>  ssh-file <arg> --project all --site twoSite --runbase
# 跑多个测试工程：表示执行两个站点tdd,story, sdv串并行所有基本用例
python run_test_tool.py --scm-info <arg>  ssh-file <arg> --project sotry --site twoSite --testng-conf testng
# 跑单个测试工程：表示执行两个站点story并行所有用例
# scm-info <arg>              # 部署完scm集群生成信息存放位置,.info文件 具体参考scm.info文件
# ssh-file <arg>              # 填写主机ssh相关信 具体参考./localbuild/conf/localbuild.conf
# project <arg>               # 填写测试工程名称，支持tdd, story, sdv, all(表示跑tdd,story串并行)
# site <arg>                  # 填写站点，支持oneSite ,twoSite, fourSite
# testng-conf <arg>           # 支持testng , testng-serial
# runbase                     # 跑基本测试用例，搭配 all，project 使用
# packages <arg>              # 指定需要执行的包（逗号分隔，依赖于 --testng-conf）
# classes <arg>               # 指定需要执行的类（逗号分隔，依赖于 --testng-conf）
# work-path                   # 工作目录，本次工具执行中间产物，结果收集
# conf                        # 工具配置文件路径，默认从模板中读取(./localbuild/test_executor), 用户可自定义调整文件路径
```   

### 3.5 run_unit_test.py
执行单元测试用例
```shell
python run_unit_test.py --scm-info <arg> --output <arg> 
# 执行 SCM 中的所有单元测试用例
```   

### 3.6 statistical_cov.py
统计代码覆盖率
```shell
python statistical_cov.py -work-path <arg> --scm-deploy-info <arg> --scm-ut-info <arg> 
# 执行 SCM 中的所有单元测试用例
# work-path                   # 工作目录，本次覆盖率统计的中间产物归档在该目录下
# scm-deploy-info             # 部署完 scm 集群节点信息存放位置,.info文件 具体参考scm_deploy.info文件
# scm-ut-info                 # 执行完单元测试后各个测试单元的描述信息,.info文件 具体参考scm_ut.info文件
```   

### 3.7 clean_scm.py
清理SCM集群环境
```shell
python clean_scm.py --host <arg>  --ssh-file <arg>
# host <arg>                  # 主机名，逗号分隔
# ssh-file <arg>              # 填写主机ssh相关信 具体参考./localbuild/conf/localbuild.conf
```

## 4 附属生成文件(部分介绍)
### 4.1 sdb.info
```shell
[cluster1]
coord = XX.XX.XX.XX:XX,

[cluster2]
coord = XX.XX.XX.XX:XX,
# 第一个SDB集群作为SCM主站点,部署SCM会根据部署模板替换掉[metasource]与[datasource]中信息;
```
### 4.2 scm.info
```shell
mainSdbUrl=XX.XX.XX.XX:XX  # 元数据服务Url
sdbuser=XX                 # 元数据服务账号
sdbpassword=XX             # 元数据服务密码
gateWayUrl=XX.XX.XX.XX:XX  # 网关Url
sshuser=XX                 # 网关所在机器ssh 用户 
sshpassword=XX             # 网关所在机器ssh 密码
omUrl=XX.XX.XX.XX:XX       # Om服务信息
```
### 4.3 scm_deploy.info
```shell
# 服务/站点名，主机IP，端口，覆盖率代理端口
serviceName,hostname,port,coveragePort
rootSite,192.168.31.71,15000,15002
branchSite1,192.168.31.71,15100,15102
```
### 4.4 scm_ut.info
```shell
# 模块名，模块路径
moduleName,modulePath
sequoiacm-content-server,/opt/git/sequoiacm/src/content-server/project/server/contentserver/
sequoiacm-infrastructure-common,/opt/git/sequoiacm/src/infrastructure/project/common/
```