目的： 安装Python paramiko 外部库进行本地化构建ssh远程连接

方式：离线安装

Pythony版本:2.7.*

在需要部署集群的Linux机器上

--------------------------Linux(必须)-------------------------------------------------------

脚本安装：

* 若项目在Linux机器上，若无expect,安装expect,之后在目录下直接执行脚本即可：

* 若项目在windows上，则需拷贝./localbuild/install_paramiko/Linux文件夹至对应Linux 机器上，之后安装expect , 最后在其目录下执行脚本即可;

***注意：需要给Linux下面install_paramiko.sh 和start.sh赋予权限***

```shell
bash install_paramiko.sh --host 192.168.XX.XX,192.168.XX.XX
```
--host参数说明，后面接所需要安装部署主机名，逗号分隔(必填):

其脚本主要作用是在填写的主机中安装paramiko外部库，修改/etc/ssh/sshd_config 修改配置文件，在/etc/environment中添加JAVA_HOME

***注意:对于麒麟系统机器需要安装Python Paramiko 需要单独在其机器下拷贝Linux文件夹在/usr/local/目录下，之后执行命令:***
```shell
bash start.sh 
```


--------------------------Windows-----------------------------------------------------------

若项目在windows上:需要手动安装：
* 拷贝SCM项目下localbuild\install_parasmiko\windows下所有tar包和应用程序到C:\Python27\Tools
* 解压ecdsa-0.10.tar包至Tools文件夹,解压后进入其目录cmd执行命令:python setup.py install
* 成功完成步骤2后,双击pycrypto-2.6.win-amd64-py2.7应用程序进行安装
* 成功完成步骤3后,解压paramiko-1.13.0.tar至Tools文件夹，解压后进入其目录cmd执行命令:python setup.py install
* 成功完成步骤4后,进行检验:创建任意python文件,引入import paramiko后执行,若无对应相关报错，说明引入paramiko成功。