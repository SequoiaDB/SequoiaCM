start 子命令提供内容服务节点的启动功能。

##子命令选项##

|选项      |缩写 |描述                                     |
|----------|-----|-----------------------------------------|
|--all     |-a   |启动所有节点                             |
|--port    |-p   |指定特定端口，启动该节点                 |
|--timeout |-t   |指定超时时间，在规定时间内节点未正常运行，判定启动失败，单位：秒，默认值：50s|


###示例###

1. 启动端口号为 15000 的内容服务节点节点

  ```lang-javascript
  $ scmctl.sh start -p 15000
  ```

2. 启动所有节点

  ```lang-javascript
  $ scmctl.sh start -a
  ```

>  **Note:**
> 
>  * 节点配置文件的 scm.jvm.options 配置项可以配置节点启动时的额外 JVM 参数
> 
>  * 节点配置文件位于：\<Sequoiacm安装目录\>/conf/content-server/\<节点端口号\>/application.properties 
> 
>  * JVM 远程调试参数：-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=12345
> 
>  * -XDebug 启动调试
> 
>  * -Xrunjdwp 通知 JVM 使用JAVA DEBUG WIRE PROTOCOL 运行调试环境
> 
>  * transport 用于在调试程序和 JVM 使用的进程之间通讯
> 
>  * dt_socket 套接字传输
>
>  * server=y/n JVM 是否需要作为调试服务器执行
> 
>  * suspend=y/n 是否在调试客户端建立连接之后启动VM
> 
>  * address=12345 调试服务器监听的端口号
> 
>  * 常见的 JVM 内存参数
> 
>  * -Xms\<size\> 设置 JVM 堆内存的初始大小，缺省单位为字节，该大小必须是 1024 的整数倍并大于 1MB；
> 
>  * -Xmx\<size\> 设置 JVM 堆内存的最大可用大小，缺省单位为字节，该大小必须是1024的整数倍并大于1MB；
> 
>  * -Xmn\<size\> 设置 JVM 堆内新生代的大小，缺省单位为字节；
> 
>  * -Xss\<size\> 每个线程可使用的内存大小，缺省单位为字节；