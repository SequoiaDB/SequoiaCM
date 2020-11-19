消息队列服务主要负责 SequoiaCM 集群内部微服务间的消息投递：

![消息队列服务][message_queue]

SequoiaCM 消息队列服务通过元数据服务实现消息的持久化，为 SequoiaCM 系统内部提供了消息队列的基本功能，并实现了至少消费一次、顺序消费等特性，其设计主要参考了 Kafka，定义了如下概念：

- 主题（Topic）：消息队列服务中对消息的一个逻辑分类，每条消息属于且仅属于一个主题，生产者投递消息、消费者订阅消息均需要指定主题
- 消费组（Consumer Group）：消费者的一个逻辑分类， 消费组与主题绑定，每个消费者必须加入消费组才能进行消费，同一个主题下的不同消费组互不干扰，独立消费主题内消息，同一个消费组下的消费者互相竞争地消费主题下的消息。
- 分区（Partition）：主题内对消息的逻辑划分，主题的分区数由创建主题时指定，主题内的消息根据消息 ID 哈希取模分区数确定所在分区，一个分区在每个消费组中，只会由一个消费者负责消费，所以分区内的消息可以保证顺序消费，分区数决定了主题在一个消费组内最多能有多少个消费者同时进行消费。

一个典型的主题、消费组、分区关系示意图：

![消息消费][message_queue_consuem]


[message_queue]:Architecture/Microservice/message_queue.png
[message_queue_consuem]:Architecture/Microservice/message_queue_consume.png