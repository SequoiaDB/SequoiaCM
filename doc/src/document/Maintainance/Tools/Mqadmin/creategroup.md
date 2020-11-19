creategroup 子命令提供创建消费组的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|----------------------------------------------------|--------|
|--name     |-n   |新建消费组的名字                                             |是      |
|--topic    |-t   |主题名     |是|
|--offset   |-o   |新建消费组从 topic 的什么位置开始消费，可选值：oldest、latest，默认值：oldest|否|
|--url      |-u   |消息队列服务节点地址，默认值：localhost:8610|否|

###示例###

创建消费组

   ```lang-javascript
   $  mqadmin.sh creategroup --name mygroup --topic mytopic
   ```