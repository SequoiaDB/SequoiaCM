### 守护进程启停 ###

- 启动守护进程

  1). 执行启动

  ```shell
  scmd.sh start
  ```

  > **Note：**
  >
  >  - 可指定 -p 参数调整监控周期，默认值为 5（单位：秒）。

  2). 检查守护进程

  ```shell
  ps -ef | grep sequoiacm-daemon
  ```

- 关闭守护进程

  ```shell
  scmd.sh stop
  ```

  > **Note：**
  >
  >  * 关闭守护进程，需切换至启动守护进程的用户。

### 查看监控列表 ###

```shell
scmd.sh list
```

> **Note：**
>
>  * 监控列表仅在守护进程启用时生效。
>  * status 表示节点的监控状态，on 为正在监控中（节点挂掉时会自动拉起），off 则不监控。

### 修改或添加监控节点 ###

> 使用 SequoiaCM 节点工具启动/停止 SequoiaCM 服务节点时，默认会开启/暂停监控，不需要手动修改。

- 修改监控节点状态

    - 示例1：停止对本机内容服务节点的监控

  ```shell
  scmd.sh chstatus \
  -t CONTENT-SERVER \
  -s off
  ```

    - 示例2：停止对本机 15000 端口对应节点的监控

  ```shell
  scmd.sh chstatus \
  -p 15000 \
  -s off
  ```

- 添加监控节点

    - 示例1：添加对本机一个内容服务节点的监控

  ```shell
  scmd.sh add \
  -t CONTENT-SERVER \
  -c /opt/sequoiacm/sequoiacm-content/conf/content-server/15000/application.properties \
  -s on
  ```

### 查看命令选项

- 查看工具的子命令列表

  ```shell
  scmd.sh -h
  ```

- 查看子命令选项，以 add 为例

   ```shell
   ./scmd.sh -h add
   ```

