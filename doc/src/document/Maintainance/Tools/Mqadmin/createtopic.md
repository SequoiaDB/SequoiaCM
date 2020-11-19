createtopic 子命令提供创建消费组的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|----------------------------------------------------|--------|
|--name     |-n   |新建主题的名字                                             |是      |
|--partition-count    |-p   |主题分区数     |是|
|--url      |-u   |消息队列服务节点地址，默认值：localhost:8610|否|

###示例###

创建主题

   ```lang-javascript
   $  mqadmin.sh createtopic --name  mytopic --partition-count 3
   ```