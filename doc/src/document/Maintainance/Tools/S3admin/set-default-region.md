set-default-region 子命令提供设置 S3 服务默认区域的功能。

## 子命令选项 ##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--region   |-r   | 指定某个工作区的名字，作为 S3 默认 Region               |是      |
|--url      |     | 网关地址|是|
|--user     |-u   | 管理员用户名|是|
|--password |-p   | 管理员密码，不指定值表示采用交互式输入密码|是|

### 示例 ###

设置默认区域

   ```lang-javascript
   $  s3admin.sh set-default-region --region ws_default --user admin --password --url localhost:8080
   ```