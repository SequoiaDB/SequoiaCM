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
| ZooKeeper      | 3.4.12以上版本                         |
| SequoiaDB      | 3.0.0以上版本                          |
| python         | 2.7.x版本                              |
| Elasticsearch  | 6.3.2版本                              |

##系统要求##

以 Linux 操作系统为例，要求如下：

###配置主机名###

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
