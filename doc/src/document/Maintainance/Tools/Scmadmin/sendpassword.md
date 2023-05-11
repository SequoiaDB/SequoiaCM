sendpassword 子命令提供了一键完成密码[加密][encrypt]和分发的功能。

##子命令选项##

|选项 |缩写 | 描述                                                                                                                    |是否必填|
|-----------|-----|-----------------------------------------------------------------------------------------------------------------------|--------|
|--user| -u | 用户名                                                                                                                   |是 |
|--password| -p| 待加密用户密码，指定值则使用明文输入，不指定值则命令行提示输入                                                                                       |是 |
|--hosts| | 待分发密码文件的主机列表，格式：server1:22,server2:22，端口号为 22 时可以省略端口部分                                                               |否 |
|--hosts-file | | 以文件方式指定待分发密码文件的主机列表，与 --hosts 互斥，文件内容的格式如下(每一行表示一个主机信息)：<br> server1,22,scmadmin,admin <br> server2,22,scmadmin,admin |否 |
|--save-path| | 加密后的密码文件在所分发主机上的保存位置                                                                                                  |是 |
|--override | | 当在分发主机上存在同名密码文件时，进行覆盖。不指定该参数时，不进行覆盖                                                                                   |否 |


> **Note:**
>
>  * 通过 --hosts 方式指定主机列表时，命令执行机需与主机列表中的主机配置 ssh 互信
>


###示例###

1. 加密码用户 scmadmin 的密码，并分发到主机 server1、server2 上的 /opt/sequoiacm/secret/sdb.pwd 路径下：

   ```lang-javascript
    $ ./scmadmin.sh sendpassword -u scmadmin --save-path /opt/sequoiacm/secret/sdb.pwd --hosts server1,server2 -p
   [1]Send password file to server1 success
   [2]Send password file to server2 success
   Process finished, success: 2, failed: 0
   The password file is saved on the hosts: [server1, server2], path: /opt/sequoiacm/secret/sdb.pwd 
   ```
2. 以文件方式指定待分发的主机列表：

   - 创建文件 hosts.txt，写入主机信息：
   
   ```lang-javascript
    server1,22,scmadmin,admin
    server2,22,scmadmin,admin
   ```
   
   - 执行命令，分发密码到 hosts.txt 所指定的主机上：
   
   ```lang-javascript
   $ ./scmadmin.sh sendpassword -u scmadmin --save-path /opt/sequoiacm/secret/sdb.pwd --hosts-file hosts.txt -p
   [1]Send password file to server1 success
   [2]Send password file to server2 success
   Process finished, success: 2, failed: 0
   The password file is saved on the hosts: [server1, server2], path: /opt/sequoiacm/secret/sdb.pwd 
   ```


[encrypt]:Maintainance/Tools/Scmadmin/encrypt.md