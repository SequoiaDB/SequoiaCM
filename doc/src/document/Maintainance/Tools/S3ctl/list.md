list 子命令提供查询和显示本机 S3 服务节点的功能。

## 子命令选项 ##

|选项   |缩写 |描述                                                       |
|-------|-----|-----------------------------------------------------------|
|--port |-p   |端口号，默认查询所有端口号                                 |
|--mode |-m   |查看本地所有节点:'local'，查看运行中的节点:'run',默认:'run'|

### 示例 ###

1.  查看本机运行中 s3 服务的节点

  ```lang-javascript
  $ s3ctl.sh list
  ```

2.  查看本机所有 s3 服务的节点

  ```lang-javascript
  $ s3ctl.sh list -m local
  ```

3.  查看特定端口 s3 服务的节点

  ```lang-javascript
  $ s3ctl.sh list -p 8180 -m local
  ```

