updatetopic 子命令提供更新消费组的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|----------------------------------------------------|--------|
|--name     |-n   |新建主题的名字                                             |是      |
|--new-partition-count    |-p   |主题的新分区数     |是|
|--timeout  |-p   |更新超时时间，默认不超时，单位：ms     |是|
|--url      |-u   |消息队列服务节点地址，默认值：localhost:8610|否|

###示例###

更新主题

   ```lang-javascript
   $  mqadmin.sh updatetopic --name  mytopic --new-partition-count 5
   ```