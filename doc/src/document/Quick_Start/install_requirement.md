在安装 SequoiaCM 产品之前，请确保您选择的系统满足必须的操作系统以及软硬件要求。

##硬件要求##

操作系统： 64位操作系统

>  **Note:**

>  * SequoiaCM 更多的是消耗系统内存和 CPU，在此，暂时只是建议内存配置在 4G 以上。

##软件要求##

###中间件版本要求###

| 需求项         | 版本要求                               |
|----------------|----------------------------------------|
| JDK            | 1.8以上版本                            |
| SequoiaDB      | 3.0.0以上版本                          |
| python         | 2.7.x版本                              |
| Elasticsearch  | 6.3.2版本或8.2.0版本                   |

##系统要求##

以 Linux 操作系统为例，要求如下：

###配置主机名###

- **主机名命名规范（遵循 Java 软件体系的主机名规范）**
  1. 主机名由数字（0-9）、字母（a-z、A-Z）、连字符（-）、点字符（.）组成；
  2. 不能以连字符、点字符开头和结尾。如：-server-scm、server1.scm.；
  3. 连字符和点字符不能连续出现。如：-- .. -. .-；
  4. 如果存在点字符，最后一个点字符之后的所有字符不能含有非字母字符。如：redhat-75.server1.scm1、redhat-75.server1.sc-m；

  合法主机名，如：redhat75-server1-scm、redhat-75.server1.scm；非法主机名，如：redhat75_server1_scm（不能含有下划线，建议使用连字符：- 代替）

- **配置方法**

  - 对于SUSE:
     1. 使用 root 权限登陆，执行 hostname server1 （server1为主机名称，可根据需要修改。）；
         
         ```lang-javascript
         # hostname server1
         ```
     2. 打开 /etc/HOSTNAME 文件；
         
         ```lang-javascript
         # vi /etc/HOSTNAME
         ```
     3. 修改文件内容，配置为主机名称 server1 （主机名称）；
     
         ``` 
         server1
         ```
     4. 按 : wq 保存退出；  

  - 对于 RedHat：
     1. 使用 root 权限登陆，执行 hostname server1 （server1为主机名称，可根据需要修改。）；
         
         ```lang-javascript
         # hostname server1
         ```
     2. 打开 /etc/sysconfig/network 文件；  
         
         ```lang-javascript
         # vi /etc/sysconfig/network
         ```
     3. 将 HOSTNAME 一行修改为 HOSTNAME = server1 （其中server1 为新主机名）；

         ```
         HOSTNAME = server1 
         ``` 
     4. 按 : wq 保存退出；

  - 对于 Ubuntu：
     1. 使用 root 权限登陆，执行 hostname server1 （server1为主机名称，可根据需要修改。）；
         
         ```lang-javascript
         # hostname server1
         ```
     2. 打开 /etc/hostname 文件；
         
         ```lang-javascript
         # vi /etc/hostname
         ```
     3. 修改文件内容，配置为主机名称 server1
        
         ```
         server1
         ```
     4. 执行 : wq 保存退出；

- **验证方法**  

  执行 hostname 命令，确认打印信息是否为 “server1”

  ```lang-javascript
  # hostname
  ```

###配置主机名/ip地址映射###

- **配置方法**
  - 使用 root 权限，打开 /etc/hosts 文件 
   
     ```lang-javascript
     # vi /etc/hosts
     ```
  - 修改 /etc/hosts ，将服务器节点的主机名与IP映射关系配置到该文件中  

     ```
     192.168.20.200 server1  
     192.168.20.201 server2  
     192.168.20.202 server3
     ```
  - 保存退出

- **验证方法**
  1. ping server1（本机主机名） 可以 ping 通 
     
     ```lang-javascript
     # ping server1
     ```
  2. ping server2（远端主机名） 可以 ping 通

     ```lang-javascript
     # ping server2
     ```
     
>  **Note:**

>  * 当同一个台机器拥有多个 IP 时，若任一 IP 都允许用于 SequoiaCM 集群中的节点通信，请为所有 IP 都配置相同的 hosts 映射；否则请在后续的节点部署阶段，手动指定节点所在机器的主机名及 IP 地址信息，详情请参考[节点配置][public_config]中的 eureka.instance.hostname、spring.cloud.client.hostname、eureka.instance.ip-address 配置项。


###关闭防火墙 ###

>  **Note:**

>  * 需要管理员权限

- **配置方法**

  - 对于 SUSE:   
     1. 执行如下命令
         
         ```lang-javascript
         # SuSEfirewall2 stop
         # chkconfig SuSEfirewall2_init off
         # chkconfig SuSEfirewall2_setup off
	     ```

  - 对于 RedHat：
     1. 执行如下命令    

         ```lang-javascript
         # service iptables stop
         # chkconfig iptables off
         ```
  - 对于 Ubuntu： 
     1. 执行如下命令

         ```lang-javascript
         # ufw disable
         ```

- **验证方法**
  - 对于 SUSE

     ```lang-javascript
     # chkconfig -list | grep fire
     ``` 
  - 对于 RedHat:
     
     ```lang-javascript
     # service iptables status
     ``` 
  - 对于 Ubuntu:
     
     ```lang-javascript
     # ufw status
     ```

>  **Note:**

>  * 每台作为 SequoiaCM 服务器的机器都需要配置

[public_config]:Maintainance/Node_Config/Readme.md