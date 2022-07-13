createnode 子命令提供创建全文检索服务节点的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--type     |-t   |节点类型，可选值：fulltext-server                        |是      |
|           |-D   |节点配置参数，如指定节点端口号：-Dserver.port=8180       |是|



>  **Note:**
>
>  * 全文检索服务所支持的节点配置参数请查看 [全文检索服务节点章节][fulltext_config]

###示例###

创建全文检索服务节点

   ```lang-javascript
   $  ftadmin.sh createnode --type fulltext-server -Dserver.port=8310 -Deureka.instance.metadata-map.zone=zone1 -Deureka.client.region=DefaultRegion -Deureka.client.availability-zones.DefaultRegion=zone1 -Deureka.client.service-url.zone1=http://localhost:8800/eureka/ -Dscm.zookeeper.urls=localhost:2181 -Dscm.fulltext.es.urls=http://192.168.20.74:9200 -Dscm.textaulParser.pic.tessdataDir=/usr/share/tesseract-ocr/tessdata/
   ```

[fulltext_config]:Maintainance/Node_Config/fulltext.md